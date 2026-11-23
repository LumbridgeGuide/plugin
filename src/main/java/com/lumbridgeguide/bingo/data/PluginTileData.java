package com.lumbridgeguide.bingo.data;

import lombok.Data;
import net.runelite.api.Skill;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Data
public class PluginTileData {
    private String id;
    private int position;
    private TileType type;
    private String title;
    private String description;
    private int points;
    private String skill;
    private long xpTarget;
    private List<TileItemEntry> items;
    private Integer monsterId;
    private String monsterName;
    private Integer killCount;
    private boolean claimed;
    private String claimedByTeamId;
    private String claimedByTeamName;
    private List<PluginProgressData> progress;
    private ProofStatus proofStatus;

    public boolean isProofPending() {
        return proofStatus == ProofStatus.PENDING;
    }

    public boolean isProofRejected() {
        return proofStatus == ProofStatus.REJECTED;
    }

    public long target() {
        if (type == TileType.KILL_COUNT) {
            return killCount == null ? 0 : killCount;
        }
        return type == TileType.SKILL_XP ? xpTarget : 0;
    }

    public Optional<Skill> skill() {
        if (skill == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Skill.valueOf(skill.toUpperCase(Locale.ROOT).replace(' ', '_')));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public boolean tracksXpIn(Skill gameSkill) {
        return type == TileType.SKILL_XP && skill().filter(gameSkill::equals).isPresent();
    }

    public boolean countsKillsOf(String npcName, Integer npcId) {
        if (type != TileType.KILL_COUNT) {
            return false;
        }

        if (monsterId != null && monsterId.equals(npcId)) {
            return true;
        }
        return namesMatch(monsterName, npcName);
    }

    private static boolean namesMatch(String typedName, String gameName) {
        if (typedName == null || gameName == null) {
            return false;
        }

        String typed = normalise(typedName);
        String game = normalise(gameName);
        return !typed.isEmpty() && (typed.equals(game) || typed.equals(game + "s") || game.equals(typed + "s"));
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
