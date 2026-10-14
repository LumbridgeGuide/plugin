package com.lumbridgeguide.account;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.account.data.AccountStatusData;
import com.lumbridgeguide.account.data.AccountStatusRequest;
import com.lumbridgeguide.account.data.AccountSyncPayload;
import com.lumbridgeguide.account.data.AccountSyncResultData;
import com.lumbridgeguide.api.ApiErrorData;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Links the logged-in game account and syncs its skills and quests to Lumbridge Guide */
@Slf4j
@Singleton
public class AccountSyncService {

    private static final long UNKNOWN_ACCOUNT_HASH = -1L;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final Client client;
    private final ClientThread clientThread;
    private final LumbridgeGuideClient apiClient;
    private final LumbridgeGuideConfig config;

    private volatile AccountSyncPayload latest;
    private volatile String linkStatus;
    private volatile String message;
    private volatile AccountStatusText.Tone messageTone = AccountStatusText.Tone.MUTED;
    private volatile String lastSynced;
    private volatile boolean busy;
    private volatile Consumer<AccountView> listener;

    @Inject
    public AccountSyncService(
            Client client, ClientThread clientThread, LumbridgeGuideClient apiClient, LumbridgeGuideConfig config) {
        this.client = client;
        this.clientThread = clientThread;
        this.apiClient = apiClient;
        this.config = config;
    }

    public void setListener(Consumer<AccountView> listener) {
        this.listener = listener;
        publish();
    }

    private final LoginTracker loginTracker = new LoginTracker();

    /** Called when the plugin starts, which may be while the player is already logged in. */
    public void startUp() {
        loginTracker.start(client.getGameState() == GameState.LOGGED_IN);
    }

    /** Called when the plugin stops, so a later start does not show the previous account */
    public void reset() {
        listener = null;
        latest = null;
        linkStatus = null;
        message = null;
        busy = false;
        lastSynced = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (loginTracker.onGameState(event.getGameState())) {
            onLoggedOut();
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (loginTracker.onTick()) {
            onLoggedIn();
        }
    }

    private void onLoggedIn() {
        message = null;
        lastSynced = null;
        loadAccountOnClientThread(config.syncOnLoginLogout());
    }

    public void refresh() {
        message = null;
        clientThread.invoke(() -> loadAccountOnClientThread(false));
    }

    private void loadAccountOnClientThread(boolean syncIfLinked) {
        AccountSyncPayload snapshot = capture();
        latest = snapshot;
        linkStatus = null;
        publish();
        if (snapshot == null || !apiClient.hasApiKey()) {
            return;
        }

        apiClient.post("/plugin/accounts/status", new AccountStatusRequest(snapshot.getAccountHash()),
                response -> {
                    AccountStatusData status = apiClient.deserialize(response.getBody(), AccountStatusData.class);
                    linkStatus = status.getStatus();
                    publish();
                    if (syncIfLinked && "LINKED".equals(linkStatus)) {
                        send(snapshot, false);
                    }
                },
                this::onFailure);
    }

    private void onLoggedOut() {
        AccountSyncPayload snapshot = latest;
        if (snapshot != null && config.syncOnLoginLogout() && "LINKED".equals(linkStatus)) {
            send(snapshot, false);
        }
        latest = null;
        linkStatus = null;
        message = null;
        lastSynced = null;
        publish();
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        AccountSyncPayload snapshot = latest;
        if (snapshot != null) {
            latest = snapshot.toBuilder().skills(readSkills()).build();
        }
    }

    /** Quest points change when a quest is completed. */
    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarpId() != VarPlayerID.QP) {
            return;
        }
        AccountSyncPayload snapshot = latest;
        if (snapshot != null) {
            latest = snapshot.toBuilder().quests(readQuests()).questPoints(readQuestPoints()).build();
        }
    }

    public void syncNow() {
        if (!apiClient.hasApiKey()) {
            setMessage("Set your API key in the plugin settings first.", AccountStatusText.Tone.ERROR);
            return;
        }

        busy = true;
        publish();
        clientThread.invoke(() -> {
            AccountSyncPayload snapshot = capture();
            if (snapshot == null) {
                busy = false;
                setMessage("Log in to the game to sync.", AccountStatusText.Tone.ERROR);
                return;
            }
            latest = snapshot;
            send(snapshot, true);
        });
    }

    private void send(AccountSyncPayload snapshot, boolean manual) {
        busy = true;
        publish();
        apiClient.post("/plugin/accounts/sync", snapshot.toBuilder().manual(manual).build(),
                response -> {
                    AccountSyncResultData result =
                            apiClient.deserialize(response.getBody(), AccountSyncResultData.class);
                    busy = false;
                    linkStatus = result.getStatus();
                    lastSynced = TIME_FORMAT.format(Instant.now());
                    setMessage(result.isNewlyLinked() ? "Account linked and synced." : "Synced.",
                            AccountStatusText.Tone.SUCCESS);
                },
                this::onFailure);
    }

    private void onFailure(ApiResponse response) {
        busy = false;
        ApiErrorData error = apiClient.parseError(response);
        String impliedStatus = AccountStatusText.impliedLinkStatus(error == null ? null : error.getError());
        if (impliedStatus != null) {
            linkStatus = impliedStatus;
        }
        setMessage(apiClient.failureMessage(response, "Sync failed"), AccountStatusText.Tone.ERROR);
    }

    private void setMessage(String text, AccountStatusText.Tone tone) {
        message = text;
        messageTone = tone;
        publish();
    }

    private AccountSyncPayload capture() {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) {
            return null;
        }
        long accountHash = client.getAccountHash();
        String displayName = client.getLocalPlayer().getName();
        if (accountHash == UNKNOWN_ACCOUNT_HASH || displayName == null) {
            return null;
        }

        Set<String> worldTypes = client.getWorldType().stream().map(WorldType::name).collect(Collectors.toSet());
        return AccountSyncPayload.builder()
                .accountHash(accountHash)
                .displayName(Text.sanitize(displayName))
                .accountType(AccountStatusText.accountTypeForIronmanVarbit(client.getVarbitValue(VarbitID.IRONMAN)))
                .worldTypes(worldTypes)
                .skills(readSkills())
                .questPoints(readQuestPoints())
                .quests(readQuests())
                .build();
    }

    private List<AccountSyncPayload.SkillEntry> readSkills() {
        List<AccountSyncPayload.SkillEntry> skills = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            int level = Math.max(1, Math.min(99, client.getRealSkillLevel(skill)));
            skills.add(new AccountSyncPayload.SkillEntry(skill.getName(), level, client.getSkillExperience(skill)));
        }
        return skills;
    }

    private List<AccountSyncPayload.QuestEntry> readQuests() {
        List<AccountSyncPayload.QuestEntry> quests = new ArrayList<>();
        for (Quest quest : Quest.values()) {
            quests.add(new AccountSyncPayload.QuestEntry(quest.getName(), quest.getState(client).name()));
        }
        return quests;
    }

    private int readQuestPoints() {
        return client.getVarpValue(VarPlayerID.QP);
    }

    private void publish() {
        Consumer<AccountView> current = listener;
        if (current == null) {
            return;
        }

        AccountSyncPayload snapshot = latest;
        AccountStatusText.Line status = snapshot == null
                ? new AccountStatusText.Line("Log in to the game to link or sync this account.",
                AccountStatusText.Tone.MUTED)
                : AccountStatusText.forStatus(linkStatus);

        current.accept(new AccountView(
                snapshot == null ? null : snapshot.getDisplayName(),
                snapshot == null ? "" : AccountStatusText.accountTypeLabel(snapshot.getAccountType()),
                snapshot == null ? "" : AccountStatusText.statusBadge(linkStatus),
                status.getText(),
                status.getTone(),
                message,
                messageTone,
                lastSynced,
                busy));
    }

    @Value
    public static class AccountView {
        String displayName;
        String accountType;
        String statusBadge;
        String statusText;
        AccountStatusText.Tone statusTone;
        String message;
        AccountStatusText.Tone messageTone;
        String lastSynced;
        boolean busy;
    }
}
