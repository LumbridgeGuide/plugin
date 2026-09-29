package com.lumbridgeguide.account;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.account.data.AccountStatusData;
import com.lumbridgeguide.account.data.AccountStatusRequest;
import com.lumbridgeguide.account.data.AccountSyncPayload;
import com.lumbridgeguide.account.data.AccountSyncResultData;
import com.lumbridgeguide.api.ApiErrorData;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;

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

/**
 * Links and syncs the logged-in OSRS account. Game state is only read on the client thread; requests are sent from
 * there and answered on OkHttp threads, so the listener must hand UI work to Swing itself.
 *
 * A snapshot of the account is taken on login and kept current (skills on every stat change, quests when one is
 * completed), because by the time the client reports a logout its data may already be gone.
 */
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

    /** Call on the first game tick after a real login, when skills and quests have loaded. */
    public void onLoggedIn() {
        message = null;
        lastSynced = null;
        loadAccount(config.syncOnLoginLogout());
    }

    /** The Refresh button: re-reads the logged-in account and its link status, in case the tab has gone stale. */
    public void refresh() {
        message = null;
        clientThread.invoke(() -> loadAccount(false));
    }

    /** Must run on the client thread. Syncs afterwards only when asked to and the account is already linked. */
    private void loadAccount(boolean syncIfLinked) {
        latest = capture();
        linkStatus = null;
        publish();
        if (latest == null || !apiClient.hasApiKey()) {
            return;
        }
        apiClient.post("/plugin/accounts/status", new AccountStatusRequest(latest.getAccountHash()),
                response -> {
                    AccountStatusData status = apiClient.deserialize(response.getBody(), AccountStatusData.class);
                    linkStatus = status.getStatus();
                    publish();
                    if (syncIfLinked && "LINKED".equals(linkStatus)) {
                        send(latest, false);
                    }
                },
                this::onFailure);
    }

    public void onLoggedOut() {
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

    /** Call from the client thread on every stat change. */
    public void onStatChanged() {
        AccountSyncPayload snapshot = latest;
        if (snapshot != null) {
            latest = snapshot.toBuilder().skills(readSkills()).build();
        }
    }

    /** Call from the client thread after a quest is completed. */
    public void onQuestPointsChanged() {
        AccountSyncPayload snapshot = latest;
        if (snapshot != null) {
            latest = snapshot.toBuilder().quests(readQuests()).questPoints(readQuestPoints()).build();
        }
    }

    /** The Sync now button. Takes a fresh snapshot and may link a new account. */
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
        ApiErrorData error = parseError(response);
        String impliedStatus = AccountStatusText.statusForError(error == null ? null : error.getError());
        if (impliedStatus != null) {
            linkStatus = impliedStatus;
        }
        if (response.getStatusCode() == -1) {
            setMessage("Could not reach Lumbridge Guide.", AccountStatusText.Tone.ERROR);
        } else if (response.getStatusCode() == 401) {
            setMessage("Check your API key in the plugin settings.", AccountStatusText.Tone.ERROR);
        } else if (error != null && error.getMessage() != null) {
            setMessage(error.getMessage(), AccountStatusText.Tone.ERROR);
        } else {
            setMessage("Sync failed (" + response.getStatusCode() + ").", AccountStatusText.Tone.ERROR);
        }
    }

    private ApiErrorData parseError(ApiResponse response) {
        try {
            return apiClient.deserialize(response.getBody(), ApiErrorData.class);
        } catch (RuntimeException exception) {
            return null;
        }
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
                .displayName(displayName.replace(' ', ' '))
                .accountType(AccountStatusText.accountType(client.getVarbitValue(VarbitID.IRONMAN)))
                .worldTypes(worldTypes)
                .skills(readSkills())
                .questPoints(readQuestPoints())
                .quests(readQuests())
                .build();
    }

    private List<AccountSyncPayload.SkillEntry> readSkills() {
        List<AccountSyncPayload.SkillEntry> skills = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) {
                continue;
            }
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

    @Getter
    public static final class AccountView {
        private final String displayName;
        private final String accountType;
        private final String statusBadge;
        private final String statusText;
        private final AccountStatusText.Tone statusTone;
        private final String message;
        private final AccountStatusText.Tone messageTone;
        private final String lastSynced;
        private final boolean busy;

        AccountView(String displayName, String accountType, String statusBadge, String statusText,
                    AccountStatusText.Tone statusTone, String message, AccountStatusText.Tone messageTone,
                    String lastSynced, boolean busy) {
            this.displayName = displayName;
            this.accountType = accountType;
            this.statusBadge = statusBadge;
            this.statusText = statusText;
            this.statusTone = statusTone;
            this.message = message;
            this.messageTone = messageTone;
            this.lastSynced = lastSynced;
            this.busy = busy;
        }
    }
}
