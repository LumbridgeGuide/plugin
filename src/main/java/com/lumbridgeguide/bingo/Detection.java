package com.lumbridgeguide.bingo;

import lombok.Value;

import java.time.Instant;

@Value
public class Detection {
    DetectionEvent event;
    Integer itemId;
    Integer npcId;
    String npcName;
    Integer killCount;
    String detectedAt = Instant.now().toString();

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
