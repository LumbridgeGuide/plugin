package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.ProofService;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A section at the top of the Bingo tab offering to send proof for a tile. Offers queue up, oldest first: it shows
 * one at a time with how many are waiting, and sending or dismissing it brings up the next.
 */
class ProofOfferPanel extends Section {

    private static final int THUMBNAIL_WIDTH = Components.CONTENT_WIDTH - 2;

    private final ProofService proofService;
    private String lastResult;
    private String shownOfferId;

    ProofOfferPanel(ProofService proofService) {
        super(0);
        setBorder(new EmptyBorder(0, 0, 14, 0));
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
        removeAll();
        if (lastResult != null) {
            add(result(lastResult));
            add(Box.createVerticalStrut(8));
            lastResult = null;
        }

        addOfferDetails(offer, waiting);
        WrapText status = Components.wrapped(" ", THUMBNAIL_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED);
        add(buttons(offer, status));
        add(Box.createVerticalStrut(6));
        add(status);

        setVisible(true);
        refresh();
    }

    private void addOfferDetails(ProofService.Offer offer, int waiting) {
        PluginBoardData board = offer.getBoard();
        PluginTileData tile = offer.getTile();
        JLabel count = Components.label(countText(waiting), 11f, Font.PLAIN, Theme.TEXT_MUTED);
        count.setName("count");
        add(Components.sectionHeader("Proof to send", count));
        add(Box.createVerticalStrut(8));
        add(Components.wrapped(offer.getReason(), THUMBNAIL_WIDTH, 12f, Font.BOLD, Theme.TEXT_PRIMARY));
        add(Box.createVerticalStrut(2));
        String points = board.isTilePointsEnabled() && tile.getPoints() > 0 ? "  ·  " + tile.getPoints() + " pts" : "";
        add(Components.wrapped(tile.getTitle() + points + "  ·  " + board.getTitle(), THUMBNAIL_WIDTH, 11f,
                Font.PLAIN, Theme.TEXT_SECONDARY));
        add(Box.createVerticalStrut(8));
        add(thumbnail(offer.getScreenshot()));
        add(Box.createVerticalStrut(10));
    }

    private JPanel buttons(ProofService.Offer offer, WrapText status) {
        JButton submit = Components.primaryButton("Send proof");
        JButton dismiss = Components.secondaryButton("Not now");
        submit.addActionListener(event -> send(offer, submit, dismiss, status));
        dismiss.addActionListener(event -> proofService.dismiss(offer));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(submit);
        buttons.add(dismiss);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));
        return buttons;
    }

    private void send(ProofService.Offer offer, JButton submit, JButton dismiss, WrapText status) {
        submit.setEnabled(false);
        dismiss.setEnabled(false);
        submit.setText("Sending...");
        proofService.submit(offer, message -> SwingUtilities.invokeLater(() -> {
            if (proofService.getQueue().stream().noneMatch(queued -> queued.getId().equals(offer.getId()))) {
                lastResult = offer.getTile().getTitle() + ": " + message;
                showQueue(proofService.getQueue());
                return;
            }
            status.setText(message);
            submit.setText("Try again");
            submit.setEnabled(true);
            dismiss.setEnabled(true);
            refresh();
        }));
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
        image.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        image.setAlignmentX(LEFT_ALIGNMENT);
        return image;
    }
}
