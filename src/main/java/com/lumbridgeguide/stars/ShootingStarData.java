package com.lumbridgeguide.stars;

import lombok.Data;

/** A shooting star as the website lists it: its world, where it landed, its size and when it was reported. */
@Data
public class ShootingStarData {
    private int world;
    private int x;
    private int y;
    private int plane;
    private int level;
    private String reportedAt;
}
