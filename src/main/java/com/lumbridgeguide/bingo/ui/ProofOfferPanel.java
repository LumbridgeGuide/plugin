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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;

/** A card at the top of the Bingo tab offering to send a fresh screenshot as proof for a tile. */
class ProofOfferPanel extends Card {

    private static final int THUMBNAIL_WIDTH = Components.CONTENT_WIDTH - 24;

    private final ProofService proofService;
    private ProofService.Offer offer;

    ProofOfferPanel(ProofService proofService) {
        super(12);
        this.proofService = proofService;
        setVisible(false);
    }

    void show(ProofService.Offer newOffer) {
        offer = newOffer;
        PluginBoardData board = newOffer.getBoard();
        PluginTileData tile = newOffer.getTile();
        removeAll();

        JLabel reason = Components.sectionLabel(newOffer.getReason());
        reason.setIcon(new DotIcon(Theme.accent()));
        reason.setIconTextGap(6);
        add(reason);
        add(Box.createVerticalStrut(6));
        add(Components.wrapped(tile.getTitle(), THUMBNAIL_WIDTH, 14f, Font.BOLD, Theme.TEXT_PRIMARY));
        String team = board.getMyTeam() == null ? "" : " for " + board.getMyTeam().getName();
        String points = board.isTilePointsEnabled() && tile.getPoints() > 0 ? "  ·  " + tile.getPoints() + " points" : "";
        add(Components.wrapped(board.getTitle() + points + team, THUMBNAIL_WIDTH, 11f, Font.PLAIN,
                Theme.TEXT_SECONDARY));
        add(Box.createVerticalStrut(8));
        add(thumbnail(newOffer.getScreenshot()));
        add(Box.createVerticalStrut(4));
        add(Components.wrapped("Screenshot taken with the board code in view.", THUMBNAIL_WIDTH, 10f, Font.PLAIN,
                Theme.TEXT_MUTED));
        add(Box.createVerticalStrut(10));

        JButton submit = Components.primaryButton("Send proof");
        JButton dismiss = Components.secondaryButton("Not now");
        WrapText result = Components.wrapped(" ", THUMBNAIL_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED);
        submit.addActionListener(event -> {
            submit.setEnabled(false);
            dismiss.setEnabled(false);
            submit.setText("Sending...");
            proofService.submit(offer, message -> SwingUtilities.invokeLater(() -> {
                result.setText(message);
                submit.setText("Sent");
                dismiss.setText("Close");
                dismiss.setEnabled(true);
                revalidate();
            }));
        });
        dismiss.addActionListener(event -> close());

        JPanel buttons = new JPanel(new BorderLayout(6, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(submit, BorderLayout.CENTER);
        buttons.add(dismiss, BorderLayout.EAST);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));
        add(buttons);
        add(Box.createVerticalStrut(6));
        add(result);

        setVisible(true);
        revalidate();
        repaint();
    }

    private void close() {
        offer = null;
        setVisible(false);
        getParent().revalidate();
    }

    private static JLabel thumbnail(BufferedImage screenshot) {
        int height = Math.max(1, screenshot.getHeight() * THUMBNAIL_WIDTH / Math.max(1, screenshot.getWidth()));
        JLabel image = new JLabel(new ImageIcon(screenshot.getScaledInstance(THUMBNAIL_WIDTH, height,
                Image.SCALE_SMOOTH)));
        image.setAlignmentX(LEFT_ALIGNMENT);
        return image;
    }
}
