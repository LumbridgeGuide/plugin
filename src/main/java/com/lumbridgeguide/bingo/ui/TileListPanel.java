package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TileListPanel extends JPanel {

    private final TileIcons icons;
    private final TileProgressTracker progressTracker;

    public TileListPanel(TileIcons icons, TileProgressTracker progressTracker) {
        this.icons = icons;
        this.progressTracker = progressTracker;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }

    public void update(PluginBoardData board, boolean unclaimedFirst, Consumer<PluginTileData> onTileSelected) {
        removeAll();

        if (board != null) {
            Comparator<PluginTileData> order = Comparator.comparingInt(PluginTileData::getPosition);
            if (unclaimedFirst) {
                order = Comparator.comparing(PluginTileData::isClaimed).thenComparing(order);
            }

            List<PluginTileData> tiles = board.tiles().stream().sorted(order).collect(Collectors.toList());
            for (PluginTileData tile : tiles) {
                Long tracked = progressTracker.progressFor(tile.getId());
                TileRowPanel row = new TileRowPanel(tile, board, icons, tracked, () -> onTileSelected.accept(tile));
                row.setAlignmentX(LEFT_ALIGNMENT);
                add(row);
            }
        }

        revalidate();
        repaint();
    }
}
