package com.lumbridgeguide.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class EquipmentItemData {
    private final int itemId;
    private final String name;
    private final int quantity;
    private final String slot;
}

