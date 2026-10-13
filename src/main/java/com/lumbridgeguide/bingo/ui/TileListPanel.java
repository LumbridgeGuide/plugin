package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/** A board's tiles in order, optionally with unclaimed tiles first */
public class TileListPanel extends JPanel {

    private final TileIcons icons;
    private final TileProgressTracker progressTracker;

    TileListPanel(TileIcons icons, TileProgressTracker progressTracker) {
        this.icons = icons;
        this.progressTracker = progressTracker;
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
                OptionalLong tracked = progressTracker == null ? OptionalLong.empty()
                        : progressTracker.progressFor(tile.getId());
                TileRowPanel row = new TileRowPanel(tile, board, icons, tracked, () -> onTileSelected.accept(tile));
                row.setAlignmentX(LEFT_ALIGNMENT);
                add(row);
            }
        }

        revalidate();
        repaint();
    }
}
