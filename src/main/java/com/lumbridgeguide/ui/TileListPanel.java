package com.lumbridgeguide.ui;

import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginTileData;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The board's tiles as a vertical list, in board order or with unclaimed tiles first.
 */
public class TileListPanel extends JPanel {

    private static final int GAP = 6;

    public TileListPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }

    public void update(PluginBoardData board, boolean unclaimedFirst, Consumer<PluginTileData> onTileSelected) {
        removeAll();

        if (board != null && board.getTiles() != null) {
            Comparator<PluginTileData> order = Comparator.comparingInt(PluginTileData::getPosition);
            if (unclaimedFirst) {
                order = Comparator.comparing(PluginTileData::isClaimed).thenComparing(order);
            }

            List<PluginTileData> tiles = board.getTiles().stream().sorted(order).collect(Collectors.toList());
            for (PluginTileData tile : tiles) {
                TileRowPanel row = new TileRowPanel(tile, board, () -> onTileSelected.accept(tile));
                row.setAlignmentX(LEFT_ALIGNMENT);
                add(row);
                add(Box.createVerticalStrut(GAP));
            }
        }

        revalidate();
        repaint();
    }
}
