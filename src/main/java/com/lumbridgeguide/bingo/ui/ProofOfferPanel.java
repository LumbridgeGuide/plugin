package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.ProofService;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A card at the top of the Bingo tab offering to send proof for a tile. Offers queue up, oldest first: the card shows
 * one at a time with how many are waiting, and sending or dismissing it brings up the next.
 */
class ProofOfferPanel extends Card {

    private static final int THUMBNAIL_WIDTH = Components.CONTENT_WIDTH - 24;

    private final ProofService proofService;
    private String lastResult;
    private String shownOfferId;

    ProofOfferPanel(ProofService proofService) {
        super(12);
        this.proofService = proofService;
        setVisible(false);
    }

    /** Shows the oldest waiting offer. Called on the Swing thread whenever the queue changes. */
    void showQueue(List<ProofService.Offer> queue) {
        if (queue.isEmpty()) {
            shownOfferId = null;
            removeAll();
            if (lastResult == null) {
                setVisible(false);
            } else {
                add(result(lastResult));
                lastResult = null;
                setVisible(true);
            }
            refresh();
            return;
        }
        ProofService.Offer offer = queue.get(0);
        if (offer.getId().equals(shownOfferId)) {
            updateCount(queue.size());
            return;
        }
        shownOfferId = offer.getId();
        render(offer, queue.size());
    }

    private void render(ProofService.Offer offer, int waiting) {
        PluginBoardData board = offer.getBoard();
        PluginTileData tile = offer.getTile();
        removeAll();

        if (lastResult != null) {
            add(result(lastResult));
            add(Box.createVerticalStrut(8));
            lastResult = null;
        }

        JLabel reason = Components.sectionLabel(offer.getReason());
        reason.setIcon(new DotIcon(Theme.accent()));
        reason.setIconTextGap(6);
        JLabel count = Components.label(countText(waiting), 10f, Font.PLAIN, Theme.TEXT_MUTED);
        count.setName("count");
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.add(reason, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        add(header);
        add(Box.createVerticalStrut(6));
        add(Components.wrapped(tile.getTitle(), THUMBNAIL_WIDTH, 14f, Font.BOLD, Theme.TEXT_PRIMARY));
        String team = board.getMyTeam() == null ? "" : " for " + board.getMyTeam().getName();
        String points = board.isTilePointsEnabled() && tile.getPoints() > 0 ? "  ·  " + tile.getPoints() + " points" : "";
        add(Components.wrapped(board.getTitle() + points + team, THUMBNAIL_WIDTH, 11f, Font.PLAIN,
                Theme.TEXT_SECONDARY));
        add(Box.createVerticalStrut(8));
        add(thumbnail(offer.getScreenshot()));
        add(Box.createVerticalStrut(4));
        add(Components.wrapped("Screenshot taken with the board code in view.", THUMBNAIL_WIDTH, 10f, Font.PLAIN,
                Theme.TEXT_MUTED));
        add(Box.createVerticalStrut(10));

        JButton submit = Components.primaryButton("Send proof");
        JButton dismiss = Components.secondaryButton("Not now");
        WrapText status = Components.wrapped(" ", THUMBNAIL_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED);
        submit.addActionListener(event -> {
            submit.setEnabled(false);
            dismiss.setEnabled(false);
            submit.setText("Sending...");
            proofService.submit(offer, message -> SwingUtilities.invokeLater(() -> {
                if (proofService.getQueue().stream().noneMatch(queued -> queued.getId().equals(offer.getId()))) {
                    lastResult = tile.getTitle() + ": " + message;
                    showQueue(proofService.getQueue());
                    return;
                }
                status.setText(message);
                submit.setText("Try again");
                submit.setEnabled(true);
                dismiss.setEnabled(true);
                refresh();
            }));
        });
        dismiss.addActionListener(event -> proofService.dismiss(offer));

        JPanel buttons = new JPanel(new BorderLayout(6, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(submit, BorderLayout.CENTER);
        buttons.add(dismiss, BorderLayout.EAST);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));
        add(buttons);
        add(Box.createVerticalStrut(6));
        add(status);

        setVisible(true);
        refresh();
    }

    private void updateCount(int waiting) {
        for (Component header : getComponents()) {
            if (header instanceof JPanel) {
                for (Component child : ((JPanel) header).getComponents()) {
                    if ("count".equals(child.getName())) {
                        ((JLabel) child).setText(countText(waiting));
                        refresh();
                        return;
                    }
                }
            }
        }
    }

    private static String countText(int waiting) {
        return waiting > 1 ? "1 of " + waiting : "";
    }

    private static WrapText result(String text) {
        return Components.wrapped(text, THUMBNAIL_WIDTH, 11f, Font.PLAIN, Theme.TEXT_SECONDARY);
    }

    private void refresh() {
        revalidate();
        repaint();
        if (getParent() != null) {
            getParent().revalidate();
        }
    }

    private static JLabel thumbnail(BufferedImage screenshot) {
        int height = Math.max(1, screenshot.getHeight() * THUMBNAIL_WIDTH / Math.max(1, screenshot.getWidth()));
        JLabel image = new JLabel(new ImageIcon(screenshot.getScaledInstance(THUMBNAIL_WIDTH, height,
                Image.SCALE_SMOOTH)));
        image.setAlignmentX(LEFT_ALIGNMENT);
        return image;
    }
}
