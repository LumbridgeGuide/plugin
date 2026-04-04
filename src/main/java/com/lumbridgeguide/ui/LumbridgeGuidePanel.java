package com.lumbridgeguide.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.service.BoardDataService;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

public class LumbridgeGuidePanel extends PluginPanel {

    private final LumbridgeGuideConfig config;
    private final BoardDataService boardDataService;

    private final JPanel noKeyCard;
    private final JPanel boardViewCard;

    private final JComboBox<PluginBoardData> boardSelector;
    private final BoardHeaderPanel headerPanel;
    private final BoardGridPanel gridPanel;
    private final JButton refreshButton;
    private final JLabel emptyLabel;

    public LumbridgeGuidePanel(BoardDataService boardDataService, LumbridgeGuideConfig config) {
        this.config = config;
        this.boardDataService = boardDataService;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(LumbridgeGuideTheme.PANEL_BACKGROUND);

        JLabel pluginTitle = new JLabel("Lumbridge Guide");
        pluginTitle.setForeground(LumbridgeGuideTheme.TEXT_PRIMARY);
        pluginTitle.setFont(pluginTitle.getFont().deriveFont(Font.BOLD, 16f));
        pluginTitle.setHorizontalAlignment(SwingConstants.CENTER);
        pluginTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        add(pluginTitle, BorderLayout.NORTH);

        noKeyCard = buildNoKeyCard();
        boardViewCard = new JPanel(new BorderLayout());
        boardViewCard.setOpaque(false);

        boardSelector = new JComboBox<>();
        boardSelector.setRenderer(new BoardSelectorRenderer());
        boardSelector.setBackground(LumbridgeGuideTheme.PANEL_BACKGROUND_LIGHTER);
        boardSelector.setForeground(LumbridgeGuideTheme.TEXT_PRIMARY);
        boardSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        boardSelector.setFocusable(false);
        boardSelector.addActionListener(event -> onBoardSelected());

        headerPanel = new BoardHeaderPanel();
        gridPanel = new BoardGridPanel();

        emptyLabel = new JLabel("No active boards");
        emptyLabel.setForeground(LumbridgeGuideTheme.TEXT_MUTED);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setBorder(new EmptyBorder(30, 0, 30, 0));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.add(boardSelector);

        JPanel headerWrapper = new JPanel();
        headerWrapper.setOpaque(false);
        headerWrapper.setLayout(new BoxLayout(headerWrapper, BoxLayout.Y_AXIS));
        headerWrapper.setBorder(new EmptyBorder(6, 0, 0, 0));
        headerWrapper.add(headerPanel);
        topSection.add(headerWrapper);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.add(gridPanel);
        contentPanel.add(emptyLabel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        refreshButton = new JButton("Refresh");
        refreshButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        refreshButton.setPreferredSize(new Dimension(0, 32));
        refreshButton.setFocusPainted(false);
        refreshButton.setBackground(LumbridgeGuideTheme.BRAND_ACCENT);
        refreshButton.setForeground(LumbridgeGuideTheme.TEXT_PRIMARY);
        refreshButton.addActionListener(event -> onRefreshClicked());

        boardViewCard.add(topSection, BorderLayout.NORTH);
        boardViewCard.add(scrollPane, BorderLayout.CENTER);
        boardViewCard.add(refreshButton, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(noKeyCard, BorderLayout.CENTER);
        centerPanel.add(boardViewCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        SwingUtilities.invokeLater(() ->
        {
            boolean hasKey = config.apiKey() != null && !config.apiKey().trim().isEmpty();

            noKeyCard.setVisible(!hasKey);
            boardViewCard.setVisible(hasKey);

            if (hasKey) {
                populateBoardSelector();
            }

            revalidate();
            repaint();
        });
    }

    private void populateBoardSelector() {
        List<PluginBoardData> boards = boardDataService.getBoards();
        PluginBoardData previousSelection = (PluginBoardData) boardSelector.getSelectedItem();

        DefaultComboBoxModel<PluginBoardData> model = new DefaultComboBoxModel<>();
        for (PluginBoardData board : boards) {
            model.addElement(board);
        }
        boardSelector.setModel(model);

        boolean hasBoards = !boards.isEmpty();
        boardSelector.setVisible(boards.size() > 1);
        emptyLabel.setVisible(!hasBoards);
        gridPanel.setVisible(hasBoards);
        headerPanel.setVisible(hasBoards);

        if (hasBoards) {
            PluginBoardData toSelect = boards.get(0);
            if (previousSelection != null) {
                for (PluginBoardData board : boards) {
                    if (board.getId().equals(previousSelection.getId())) {
                        toSelect = board;
                        break;
                    }
                }
            }
            boardSelector.setSelectedItem(toSelect);
            displayBoard(toSelect);
        } else {
            headerPanel.update((PluginBoardData) null);
            gridPanel.update((PluginBoardData) null);
        }
    }

    private void onBoardSelected() {
        PluginBoardData selected = (PluginBoardData) boardSelector.getSelectedItem();
        if (selected != null) {
            displayBoard(selected);
        }
    }

    private void displayBoard(PluginBoardData board) {
        headerPanel.update(board);
        gridPanel.update(board);
        revalidate();
        repaint();
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

    private static JPanel buildNoKeyCard() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel icon = new JLabel("🔑");
        icon.setFont(icon.getFont().deriveFont(28f));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setBorder(new EmptyBorder(30, 0, 10, 0));

        JLabel message = new JLabel(
                "<html><center>Set your API key in the<br>plugin settings to get started.</center></html>");
        message.setForeground(LumbridgeGuideTheme.TEXT_SECONDARY);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(icon);
        panel.add(message);
        return panel;
    }

    private static class BoardSelectorRenderer extends JLabel implements ListCellRenderer<PluginBoardData> {

        BoardSelectorRenderer() {
            setOpaque(true);
            setFont(getFont().deriveFont(Font.PLAIN, 12f));
            setBorder(new EmptyBorder(4, 6, 4, 6));
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends PluginBoardData> list,
                PluginBoardData value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            if (value != null) {
                setText(value.getTitle());
            } else {
                setText("");
            }

            if (isSelected) {
                setBackground(LumbridgeGuideTheme.BRAND_PRIMARY);
                setForeground(LumbridgeGuideTheme.TEXT_PRIMARY);
            } else {
                setBackground(LumbridgeGuideTheme.PANEL_BACKGROUND_LIGHTER);
                setForeground(LumbridgeGuideTheme.TEXT_SECONDARY);
            }

            return this;
        }
    }
}
