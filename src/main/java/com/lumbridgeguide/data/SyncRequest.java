package com.lumbridgeguide.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class SyncRequest {
    private final String playerName;
    private final List<InventoryItemData> items;
}

