package com.lumbridgeguide.account;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.account.data.AccountStatusData;
import com.lumbridgeguide.account.data.AccountStatusRequest;
import com.lumbridgeguide.account.data.AccountSyncPayload;
import com.lumbridgeguide.account.data.AccountSyncResultData;
import com.lumbridgeguide.account.data.QuestEntry;
import com.lumbridgeguide.account.data.SkillEntry;
import com.lumbridgeguide.api.ApiErrorData;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import lombok.RequiredArgsConstructor;
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

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AccountSyncService {

    public static final long UNKNOWN_ACCOUNT_HASH = -1L;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final Client client;
    private final ClientThread clientThread;
    private final LumbridgeGuideClient apiClient;
    private final LumbridgeGuideConfig config;
    private final LoginTracker loginTracker = new LoginTracker();

    private volatile AccountSyncPayload latest;
    private volatile String linkStatus;
    private volatile String message;
    private volatile StatusTone messageTone = StatusTone.MUTED;
    private volatile String lastSynced;
    private volatile boolean busy;
    private volatile Consumer<AccountView> listener;

    public void setListener(Consumer<AccountView> listener) {
        this.listener = listener;
        publish();
    }

    public void startUp() {
        loginTracker.start(client.getGameState() == GameState.LOGGED_IN);
    }

    public void reset() {
        listener = null;
        busy = false;
        clear();
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
        boolean sync = config.syncOnLoginLogout();
        loadAccountOnClientThread(sync, sync);
    }

    public void refresh() {
        message = null;
        clientThread.invoke(() -> loadAccountOnClientThread(true, false));
    }

    private void loadAccountOnClientThread(boolean checkLink, boolean syncIfLinked) {
        AccountSyncPayload snapshot = capture();
        latest = snapshot;
        linkStatus = null;
        publish();
        if (!checkLink || snapshot == null || !apiClient.hasApiKey()) {
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
        clear();
    }

    private void clear() {
        latest = null;
        linkStatus = null;
        message = null;
        lastSynced = null;
        publish();
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        AccountSyncPayload snapshot = latest;
        if (snapshot != null && config.syncOnLoginLogout()) {
            latest = snapshot.toBuilder().skills(readSkills()).build();
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        AccountSyncPayload snapshot = latest;
        if (event.getVarpId() == VarPlayerID.QP && snapshot != null && config.syncOnLoginLogout()) {
            latest = snapshot.toBuilder().quests(readQuests()).questPoints(readQuestPoints()).build();
        }
    }

    public void syncNow() {
        if (!apiClient.hasApiKey()) {
            setMessage("Add your plugin key in the plugin settings first", StatusTone.ERROR);
            return;
        }

        busy = true;
        publish();
        clientThread.invoke(() -> {
            AccountSyncPayload snapshot = capture();
            if (snapshot == null) {
                busy = false;
                setMessage("Log in to the game to sync", StatusTone.ERROR);
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
                    setMessage(result.isNewlyLinked() ? "Account linked and synced" : "Synced", StatusTone.SUCCESS);
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
        setMessage(apiClient.failureMessage(response, "Sync failed"), StatusTone.ERROR);
    }

    private void setMessage(String text, StatusTone tone) {
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
                .accountType(accountType(client.getVarbitValue(VarbitID.IRONMAN)))
                .worldTypes(worldTypes)
                .skills(readSkills())
                .questPoints(readQuestPoints())
                .quests(readQuests())
                .build();
    }

    private List<SkillEntry> readSkills() {
        List<SkillEntry> skills = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            skills.add(new SkillEntry(skill.getName(), client.getRealSkillLevel(skill),
                    client.getSkillExperience(skill)));
        }
        return skills;
    }

    private List<QuestEntry> readQuests() {
        List<QuestEntry> quests = new ArrayList<>();
        for (Quest quest : Quest.values()) {
            quests.add(new QuestEntry(quest.getName(), quest.getState(client).name()));
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
        StatusLine status = snapshot == null ? AccountStatusText.LOGGED_OUT : AccountStatusText.forStatus(linkStatus);
        current.accept(new AccountView(
                snapshot == null ? null : snapshot.getDisplayName(),
                status.getHeadline(),
                status.getDetail(),
                status.getTone(),
                status.getAction(),
                message,
                messageTone,
                lastSynced,
                busy));
    }

    private static String accountType(int ironmanVarbit) {
        switch (ironmanVarbit) {
            case 1:
                return "IRONMAN";
            case 2:
                return "ULTIMATE_IRONMAN";
            case 3:
                return "HARDCORE_IRONMAN";
            case 4:
                return "GROUP_IRONMAN";
            case 5:
                return "HARDCORE_GROUP_IRONMAN";
            case 6:
                return "UNRANKED_GROUP_IRONMAN";
            default:
                return "MAIN";
        }
    }
}
