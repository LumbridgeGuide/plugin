package com.lumbridgeguide.gear.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RunePouchEntry {
    private int itemId;
    private String name;
    private int quantity;
}
