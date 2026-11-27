package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class PluginProofResult {
    private long id;
    private String boardTitle;
    private String tileTitle;
    private ProofStatus status;
    private boolean reversed;
}
