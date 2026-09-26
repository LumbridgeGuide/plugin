package com.lumbridgeguide.data;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * One account sync. {@code manual} is true only when the player pressed Sync now, which is the only way a new
 * account gets linked.
 */
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
