package com.lumbridgeguide.account.data;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

/** Skills, quests and account details the plugin sends when syncing */
@Value
@Builder(toBuilder = true)
public class AccountSyncPayload {
    long accountHash;
    String displayName;
    String accountType;
    Set<String> worldTypes;
    boolean manual;
    List<SkillEntry> skills;
    Integer questPoints;
    List<QuestEntry> quests;

    @Value
    public static class SkillEntry {
        String skill;
        int level;
        int experience;
    }

    @Value
    public static class QuestEntry {
        String name;
        String state;
    }
}
