package com.lumbridgeguide.ui;

import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginTileData;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BoardGridPanel extends JPanel {

    private static final int GAP = 3;

    public BoardGridPanel() {
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 4, 0));
    }

    public void update(PluginBoardData board) {
        removeAll();

        if (board == null || board.getTiles() == null || board.getTiles().isEmpty()) {
            revalidate();
            repaint();
            return;
        }

        int gridSize = board.getGridSize();
        setLayout(new GridLayout(gridSize, gridSize, GAP, GAP));

        List<PluginTileData> sortedTiles = board.getTiles().stream()
                .sorted(Comparator.comparingInt(PluginTileData::getPosition))
                .collect(Collectors.toList());

        for (PluginTileData tile : sortedTiles) {
            add(new BingoTilePanel(tile));
        }

        revalidate();
        repaint();
    }
}

