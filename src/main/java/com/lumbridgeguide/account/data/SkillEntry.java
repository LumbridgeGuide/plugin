package com.lumbridgeguide.account.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillEntry {
    private String skill;
    private int level;
    private int experience;
}
