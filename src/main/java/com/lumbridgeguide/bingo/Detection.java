package com.lumbridgeguide.bingo;

import lombok.Data;

import java.time.Instant;

@Data
public class Detection {
    private final DetectionEvent event;
    private final Integer itemId;
    private final Integer npcId;
    private final String npcName;
    private final Integer killCount;
    private String detectedAt = Instant.now().toString();

    public static Detection drop(int itemId, Integer npcId, String npcName) {
        return new Detection(DetectionEvent.DROP, itemId, npcId, npcName, null);
    }

    public static Detection killCount(KillCount killCount) {
        return new Detection(DetectionEvent.KILL_COUNT, null, null, killCount.getMonster(), (int) killCount.getCount());
    }

    public static Detection loot(int npcId, String npcName) {
        return new Detection(DetectionEvent.KILL_COUNT, null, npcId, npcName, null);
    }

    public static Detection xp() {
        return new Detection(DetectionEvent.XP, null, null, null, null);
    }
}
