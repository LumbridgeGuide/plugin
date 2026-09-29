package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.bingo.BoardDataService;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.EmptyState;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
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
    private final EmptyState emptyState;
    private final JButton emptyRefreshButton;
    private final JLabel tilesCaption;
    private final JScrollPane listScroll;
    private final JPanel buttonBar;
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

        emptyRefreshButton = Components.secondaryButton("Refresh");
        emptyRefreshButton.addActionListener(event -> onRefreshClicked());
        JButton openWebsiteButton = Components.primaryButton("Open website");
        openWebsiteButton.addActionListener(event -> LinkBrowser.browse(LumbridgeGuideClient.websiteUrl()));
        emptyState = new EmptyState("No active boards",
                "Boards you join on the website show up here once they start.", emptyRefreshButton,
                openWebsiteButton);

        tilesCaption = Components.sectionLabel("Tiles");
        tilesCaption.setBorder(new EmptyBorder(0, 0, 2, 0));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        navigator.setAlignmentX(LEFT_ALIGNMENT);
        headerPanel.setAlignmentX(LEFT_ALIGNMENT);
        topSection.add(headerPanel);
        topSection.add(navigator);
        topSection.add(Box.createVerticalStrut(12));
        topSection.add(tilesCaption);

        JPanel listContent = new JPanel(new BorderLayout());
        listContent.setOpaque(false);
        listContent.add(tileList, BorderLayout.NORTH);
        JPanel emptyHolder = new JPanel(new BorderLayout());
        emptyHolder.setOpaque(false);
        emptyHolder.setBorder(new EmptyBorder(24, 0, 0, 0));
        emptyHolder.add(emptyState, BorderLayout.NORTH);
        listContent.add(emptyHolder, BorderLayout.CENTER);

        listScroll = new JScrollPane(listContent);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        openButton = Components.primaryButton("Open board");
        openButton.addActionListener(event -> openBoardInBrowser());
        refreshButton = Components.secondaryButton("Refresh");
        refreshButton.addActionListener(event -> onRefreshClicked());

        buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonBar.setOpaque(false);
        buttonBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        buttonBar.add(refreshButton);
        buttonBar.add(openButton);

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
        emptyState.getParent().setVisible(!hasBoards);
        tileList.setVisible(hasBoards);
        headerPanel.setVisible(hasBoards);
        tilesCaption.setVisible(hasBoards);
        buttonBar.setVisible(hasBoards);
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
        setRefreshing(true);
        boardDataService.refresh(() -> SwingUtilities.invokeLater(() ->
        {
            setRefreshing(false);
            refresh();
        }));
    }

    private void setRefreshing(boolean refreshing) {
        for (JButton button : new JButton[]{refreshButton, emptyRefreshButton}) {
            button.setEnabled(!refreshing);
            button.setText(refreshing ? "Refreshing..." : "Refresh");
        }
    }
}
