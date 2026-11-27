package com.lumbridgeguide.bingo;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.image.BufferedImage;

@Data
@AllArgsConstructor
public class ProofOffer {
    private String id;
    private PluginBoardData board;
    private PluginTileData tile;
    private String reason;
    private BufferedImage screenshot;
    private byte[] png;
    private String imageSha256;
    private XpReading xp;
    private long accountHash;
    private Detection detection;
}
