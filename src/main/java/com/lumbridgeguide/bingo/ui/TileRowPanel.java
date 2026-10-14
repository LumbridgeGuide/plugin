package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.ui.Badge;
import com.lumbridgeguide.ui.CheckIcon;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.IconWell;
import com.lumbridgeguide.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.OptionalLong;

/** Clickable one-tile summary in the board overview */
class TileRowPanel extends JPanel {

    TileRowPanel(PluginTileData tile, PluginBoardData board, TileIcons icons, OptionalLong tracked, Runnable onClick) {
        super(new BorderLayout(8, 0));
        setBackground(Theme.SURFACE_RAISED);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE), new EmptyBorder(6, 2, 6, 4)));

        boolean mine = TileState.isMine(tile, board);
        boolean takenByOther = tile.isClaimed() && !mine;
        Color teamColor = mine ? Theme.parseTeamColor(board.getMyTeam().getColor()) : null;

        add(iconHolder(tile, icons, takenByOther), BorderLayout.WEST);
        add(textColumn(tile, board, tracked, mine, takenByOther, teamColor), BorderLayout.CENTER);
        add(trailing(tile, mine, teamColor), BorderLayout.EAST);
        installHover(onClick);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private static JPanel iconHolder(PluginTileData tile, TileIcons icons, boolean takenByOther) {
        IconWell icon = new IconWell(28);
        icon.setImage(icons.imageFor(tile));
        if (takenByOther) {
            icon.setIconOpacity(0.45f);
        }
        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(icon, BorderLayout.CENTER);
        return iconHolder;
    }

    private static JPanel textColumn(PluginTileData tile, PluginBoardData board, OptionalLong tracked, boolean mine,
                                     boolean takenByOther, Color teamColor) {
        JPanel textColumn = new JPanel();
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
        textColumn.setOpaque(false);
        JLabel title = Components.label(tile.getTitle(), 12f, Font.PLAIN,
                takenByOther ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY);
        title.setToolTipText(tile.getTitle());
        textColumn.add(title);
        JLabel meta = Components.label(metaText(tile, board, mine, tracked), 11f, Font.PLAIN,
                mine ? teamColor : Theme.TEXT_MUTED);
        meta.setBorder(new EmptyBorder(1, 0, 0, 0));
        textColumn.add(meta);
        return textColumn;
    }

    private void installHover(Runnable onClick) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                setOpaque(true);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                setOpaque(false);
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                onClick.run();
            }
        });
    }

    private static JComponent trailing(PluginTileData tile, boolean mine, Color teamColor) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        JComponent end;
        if (mine) {
            end = new JLabel(new CheckIcon(teamColor));
        } else if (!tile.isClaimed() && tile.isProofPending()) {
            end = Badge.status("Pending", Theme.WARNING);
        } else if (!tile.isClaimed() && tile.isProofRejected()) {
            end = Badge.status("Not accepted", Theme.ERROR);
        } else {
            end = Components.label("›", 13f, Font.PLAIN, Theme.TEXT_MUTED);
        }
        JPanel centre = new JPanel(new java.awt.GridBagLayout());
        centre.setOpaque(false);
        centre.add(end);
        holder.add(centre, BorderLayout.CENTER);
        return holder;
    }

    private static String metaText(PluginTileData tile, PluginBoardData board, boolean mine, OptionalLong tracked) {
        if (mine) {
            return "Your team";
        }
        if (tile.isClaimed()) {
            return TileState.claimingTeamName(tile, board);
        }
        String progress = TileState.progressText(tile, tracked);
        if (!progress.isEmpty()) {
            return progress;
        }
        String points = TileState.pointsLabel(tile, board);
        return points != null ? points : TileText.typeName(tile);
    }
}
