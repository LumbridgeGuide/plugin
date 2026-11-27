package com.lumbridgeguide.account.data;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountSyncPayload {
    private long accountHash;
    private String displayName;
    private String accountType;
    private Set<String> worldTypes;
    private boolean manual;
    private List<SkillEntry> skills;
    private Integer questPoints;
    private List<QuestEntry> quests;
}
