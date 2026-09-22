package com.lumbridgeguide.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginTileData;
import com.lumbridgeguide.service.BoardDataService;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * The Bingo tab. The overview shows the active board with its tiles as a list,
 * and clicking a tile swaps the whole tab for that tile's details.
 */
public class BingoTabPanel extends JPanel {

    private static final String OVERVIEW_CARD = "overview";
    private static final String DETAIL_CARD = "detail";

    private final BoardDataService boardDataService;
    private final LumbridgeGuideConfig config;

    private final CardLayout cards = new CardLayout();
    private final BoardNavigatorPanel navigator;
    private final BoardHeaderPanel headerPanel;
    private final TileListPanel tileList;
    private final TileDetailPanel detailPanel;
    private final JLabel emptyLabel;
    private final JScrollPane listScroll;
    private final JButton openButton;
    private final JButton refreshButton;

    private int currentIndex;

    public BingoTabPanel(
            BoardDataService boardDataService,
            LumbridgeGuideConfig config,
            ItemManager itemManager,
            SkillIconManager skillIconManager) {
        this.boardDataService = boardDataService;
        this.config = config;

        setLayout(cards);
        setOpaque(false);

        navigator = new BoardNavigatorPanel(this::stepBoard);
        headerPanel = new BoardHeaderPanel();
        tileList = new TileListPanel();

        emptyLabel = Ui.label("No active boards", 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_MUTED);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setBorder(new EmptyBorder(30, 0, 30, 0));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        navigator.setAlignmentX(LEFT_ALIGNMENT);
        headerPanel.setAlignmentX(LEFT_ALIGNMENT);
        topSection.add(navigator);
        topSection.add(headerPanel);

        JPanel listContent = new JPanel(new BorderLayout());
        listContent.setOpaque(false);
        listContent.add(tileList, BorderLayout.NORTH);
        listContent.add(emptyLabel, BorderLayout.CENTER);

        listScroll = new JScrollPane(listContent);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        openButton = Ui.primaryButton("Open board");
        openButton.addActionListener(event -> openBoardInBrowser());
        refreshButton = Ui.secondaryButton("Refresh");
        refreshButton.addActionListener(event -> onRefreshClicked());

        JPanel buttonBar = new JPanel(new GridLayout(1, 2, 6, 0));
        buttonBar.setOpaque(false);
        buttonBar.setBorder(new EmptyBorder(8, 0, 0, 0));
        buttonBar.add(openButton);
        buttonBar.add(refreshButton);

        JPanel overview = new JPanel(new BorderLayout());
        overview.setOpaque(false);
        overview.add(topSection, BorderLayout.NORTH);
        overview.add(listScroll, BorderLayout.CENTER);
        overview.add(buttonBar, BorderLayout.SOUTH);

        detailPanel = new TileDetailPanel(itemManager, skillIconManager, this::closeDetail);
        JScrollPane detailScroll = new JScrollPane(detailPanel);
        detailScroll.setOpaque(false);
        detailScroll.getViewport().setOpaque(false);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);

        add(overview, OVERVIEW_CARD);
        add(detailScroll, DETAIL_CARD);
        cards.show(this, OVERVIEW_CARD);
    }

    public void refresh() {
        List<PluginBoardData> boards = boardDataService.getBoards();
        currentIndex = boards.isEmpty() ? 0 : Math.min(currentIndex, boards.size() - 1);

        boolean hasBoards = !boards.isEmpty();
        emptyLabel.setVisible(!hasBoards);
        tileList.setVisible(hasBoards);
        headerPanel.setVisible(hasBoards);
        openButton.setEnabled(false);

        if (hasBoards) {
            displayBoard(boards);
        } else {
            navigator.update(0, 0);
            headerPanel.update((PluginBoardData) null);
            tileList.update(null, false, tile -> { });
            cards.show(this, OVERVIEW_CARD);
        }
    }

    private void stepBoard(int direction) {
        List<PluginBoardData> boards = boardDataService.getBoards();
        if (boards.isEmpty()) {
            return;
        }
        currentIndex = (currentIndex + direction + boards.size()) % boards.size();
        displayBoard(boards);
    }

    private void displayBoard(List<PluginBoardData> boards) {
        PluginBoardData board = boards.get(currentIndex);
        navigator.update(currentIndex, boards.size());
        headerPanel.update(board);
        tileList.update(board, config.unclaimedFirst(), tile -> openDetail(tile, board));
        openButton.setEnabled(board.getWebUrl() != null && !board.getWebUrl().isEmpty());
        cards.show(this, OVERVIEW_CARD);
        listScroll.getVerticalScrollBar().setValue(0);
        revalidate();
        repaint();
    }

    private void openDetail(PluginTileData tile, PluginBoardData board) {
        detailPanel.show(tile, board);
        cards.show(this, DETAIL_CARD);
    }

    private void closeDetail() {
        cards.show(this, OVERVIEW_CARD);
    }

    private void openBoardInBrowser() {
        List<PluginBoardData> boards = boardDataService.getBoards();
        if (boards.isEmpty()) {
            return;
        }
        String webUrl = boards.get(currentIndex).getWebUrl();
        if (webUrl != null && !webUrl.isEmpty()) {
            LinkBrowser.browse(webUrl);
        }
    }

    private void onRefreshClicked() {
        refreshButton.setEnabled(false);
        refreshButton.setText("Refreshing...");

        boardDataService.refresh(() -> SwingUtilities.invokeLater(() ->
        {
            refreshButton.setEnabled(true);
            refreshButton.setText("Refresh");
            refresh();
        }));
    }
}
