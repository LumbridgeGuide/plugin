package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Keeps proof the player hasn't sent yet on disk, a PNG and a JSON file per offer in the account's own folder, so
 * closing the client doesn't lose it and the next login offers it again. Each account keeps at most
 * {@link #MAX_SAVED}, dropping the oldest.
 */
@Slf4j
@Singleton
class ProofOfferStore {

    static final int MAX_SAVED = 10;
    private static final File ROOT = new File(new File(RuneLite.RUNELITE_DIR, "lumbridge-guide"), "proof");

    /** Everything needed to rebuild an offer, apart from the screenshot. */
    @Value
    static class Saved {
        String id;
        String boardId;
        String tileId;
        String reason;
        String xpSkill;
        Long xpGained;
        Long xpTotal;
        long accountHash;
        long takenAt;
    }

    @Value
    static class Loaded {
        Saved saved;
        BufferedImage screenshot;
    }

    private final Gson gson;

    @Inject
    ProofOfferStore(Gson gson) {
        this.gson = gson;
    }

    void save(Saved saved, BufferedImage screenshot) {
        File folder = folder(saved.getAccountHash());
        try {
            Files.createDirectories(folder.toPath());
            ImageIO.write(screenshot, "png", new File(folder, saved.getId() + ".png"));
            Files.write(new File(folder, saved.getId() + ".json").toPath(),
                    gson.toJson(saved).getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            log.warn("Couldn't save proof for later", exception);
            return;
        }
        List<Saved> all = readAll(folder);
        all.stream()
                .sorted(Comparator.comparingLong(Saved::getTakenAt).reversed())
                .skip(MAX_SAVED)
                .forEach(old -> delete(old.getAccountHash(), old.getId()));
    }

    void delete(long accountHash, String id) {
        File folder = folder(accountHash);
        new File(folder, id + ".png").delete();
        new File(folder, id + ".json").delete();
    }

    /** The account's saved offers, oldest first. Unreadable ones are deleted. */
    List<Loaded> load(long accountHash) {
        File folder = folder(accountHash);
        List<Loaded> loaded = new ArrayList<>();
        for (Saved saved : readAll(folder)) {
            try {
                BufferedImage screenshot = ImageIO.read(new File(folder, saved.getId() + ".png"));
                if (screenshot != null) {
                    loaded.add(new Loaded(saved, screenshot));
                    continue;
                }
            } catch (IOException exception) {
                log.debug("Couldn't read saved proof {}", saved.getId(), exception);
            }
            delete(accountHash, saved.getId());
        }
        loaded.sort(Comparator.comparingLong(entry -> entry.getSaved().getTakenAt()));
        return loaded;
    }

    private List<Saved> readAll(File folder) {
        File[] files = folder.listFiles((directory, name) -> name.endsWith(".json"));
        List<Saved> all = new ArrayList<>();
        if (files == null) {
            return all;
        }
        for (File file : files) {
            try {
                Saved saved = gson.fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8),
                        Saved.class);
                if (saved != null && saved.getId() != null) {
                    all.add(saved);
                }
            } catch (IOException | RuntimeException exception) {
                file.delete();
            }
        }
        return all;
    }

    private static File folder(long accountHash) {
        return new File(ROOT, Long.toString(accountHash));
    }
}
