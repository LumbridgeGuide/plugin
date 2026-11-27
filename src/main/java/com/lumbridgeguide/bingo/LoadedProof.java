package com.lumbridgeguide.bingo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.image.BufferedImage;

@Data
@AllArgsConstructor
public class LoadedProof {
    private SavedProof saved;
    private BufferedImage screenshot;
    private byte[] png;
}
