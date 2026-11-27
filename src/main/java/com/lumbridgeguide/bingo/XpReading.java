package com.lumbridgeguide.bingo;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.Skill;

@Data
@AllArgsConstructor
public class XpReading {
    private Skill skill;
    private Long gained;
    private long total;
}
