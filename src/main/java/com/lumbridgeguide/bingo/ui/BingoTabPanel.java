package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.bingo.BoardDataService;
import com.lumbridgeguide.bingo.ProofService;
import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.EmptyState;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.LinkBrowser;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.util.List;

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
    private final JScrollPane listScroll;
    private final JPanel buttonBar;
    private final JButton openButton;
    private final JButton refreshButton;

    private int currentIndex;
    private int shownOffers;

    public BingoTabPanel(
            BoardDataService boardDataService,
            TileProgressTracker tileProgressTracker,
            ProofService proofService,
            LumbridgeGuideConfig config,
            ItemManager itemManager,
            SkillIconManager skillIconManager) {
        this.boardDataService = boardDataService;
        this.config = config;

        setLayout(cards);
        setOpaque(false);

        TileIcons icons = new TileIcons(itemManager, skillIconManager);
        navigator = new BoardNavigatorPanel(this::stepBoard);
        headerPanel = new BoardHeaderPanel(navigator);
        tileList = new TileListPanel(icons, tileProgressTracker);

        emptyRefreshButton = Components.secondaryButton("Refresh");
        emptyRefreshButton.addActionListener(event -> onRefreshClicked());
        emptyState = emptyState(emptyRefreshButton);
        listScroll = Components.scrolling(listContent());

        openButton = Components.primaryButton("Open board");
        openButton.addActionListener(event -> openBoardInBrowser());
        refreshButton = Components.secondaryButton("Refresh");
        refreshButton.addActionListener(event -> onRefreshClicked());
        buttonBar = buttonBar();

        JPanel overviewRoot = new JPanel(new BorderLayout());
        overviewRoot.setOpaque(false);
        ProofOfferPanel proofOffer = new ProofOfferPanel(proofService);
        overviewRoot.add(overviewTop(proofOffer), BorderLayout.NORTH);
        proofService.setOnQueueChanged(queue ->
                SwingUtilities.invokeLater(() -> onProofQueueChanged(proofOffer, queue)));
        overviewRoot.add(tilesView(), BorderLayout.CENTER);

        detailPanel = new TileDetailPanel(icons, tileProgressTracker, this::closeDetail,
                (tile, board) -> {
                    closeDetail();
                    proofService.captureManually(board, tile);
                });

        add(overviewRoot, OVERVIEW_CARD);
        add(Components.scrolling(detailPanel), DETAIL_CARD);
        cards.show(this, OVERVIEW_CARD);
    }

    private static EmptyState emptyState(JButton refresh) {
        JButton openWebsiteButton = Components.primaryButton("Open website");
        openWebsiteButton.addActionListener(event -> LinkBrowser.browse(LumbridgeGuideClient.websiteUrl()));
        return new EmptyState("No active boards",
                "Boards you join on the website show up here once they start.", refresh, openWebsiteButton);
    }

    private JPanel listContent() {
        JPanel listContent = new JPanel(new BorderLayout());
        listContent.setOpaque(false);
        listContent.add(tileList, BorderLayout.NORTH);
        JPanel emptyHolder = new JPanel(new BorderLayout());
        emptyHolder.setOpaque(false);
        emptyHolder.setBorder(new EmptyBorder(24, 0, 0, 0));
        emptyHolder.add(emptyState, BorderLayout.NORTH);
        listContent.add(emptyHolder, BorderLayout.CENTER);
        return listContent;
    }

    private JPanel buttonBar() {
        JPanel bar = new JPanel(new GridLayout(1, 2, 6, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(10, 0, 0, 0));
        bar.add(refreshButton);
        bar.add(openButton);
        return bar;
    }

    private JPanel tilesView() {
        JPanel tilesView = new JPanel(new BorderLayout());
        tilesView.setOpaque(false);
        tilesView.add(listScroll, BorderLayout.CENTER);
        tilesView.add(buttonBar, BorderLayout.SOUTH);

        return tilesView;
    }

    private JPanel overviewTop(ProofOfferPanel proofOffer) {
        JPanel overviewTop = new JPanel();
        overviewTop.setLayout(new BoxLayout(overviewTop, BoxLayout.Y_AXIS));
        overviewTop.setOpaque(false);
        proofOffer.setAlignmentX(LEFT_ALIGNMENT);
        headerPanel.setAlignmentX(LEFT_ALIGNMENT);

        overviewTop.add(proofOffer);
        overviewTop.add(headerPanel);
        overviewTop.add(Box.createVerticalStrut(12));

        return overviewTop;
    }

    private void onProofQueueChanged(ProofOfferPanel proofOffer, List<ProofService.Offer> queue) {
        boolean grew = queue.size() > shownOffers;
        shownOffers = queue.size();
        proofOffer.showQueue(queue);
        if (grew) {
            cards.show(this, OVERVIEW_CARD);
        }
    }

    public void refresh() {
        List<PluginBoardData> boards = boardDataService.getBoards();
        currentIndex = Math.max(0, boards.indexOf(boardDataService.getSelectedBoard()));

        boolean hasBoards = !boards.isEmpty();
        emptyState.getParent().setVisible(!hasBoards);
        tileList.setVisible(hasBoards);
        headerPanel.setVisible(hasBoards);
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
        if (boards.isEmpty()) return;

        currentIndex = (currentIndex + direction + boards.size()) % boards.size();
        displayBoard(boards);
    }

    private void displayBoard(List<PluginBoardData> boards) {
        PluginBoardData board = boards.get(currentIndex);
        config.setSelectedBoardId(board.getId());
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
