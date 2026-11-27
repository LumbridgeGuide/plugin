package com.lumbridgeguide.bingo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SavedProof {
    private String id;
    private String boardId;
    private String tileId;
    private String reason;
    private String xpSkill;
    private Long xpGained;
    private Long xpTotal;
    private long accountHash;
    private long takenAt;
    private String imageSha256;
    private Detection detection;
}
