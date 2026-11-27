package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProofOfferStore {

    public static final int MAX_SAVED = 10;
    private static final File ROOT = new File(new File(RuneLite.RUNELITE_DIR, "lumbridge-guide"), "proof");

    private final Gson gson;

    public void save(SavedProof saved, byte[] png) {
        File folder = folder(saved.getAccountHash());
        try {
            Files.createDirectories(folder.toPath());
            Files.write(new File(folder, saved.getId() + ".png").toPath(), png);
            Files.write(new File(folder, saved.getId() + ".json").toPath(),
                    gson.toJson(saved).getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            log.warn("Couldn't save proof for later", exception);
            return;
        }
        List<SavedProof> all = readAll(folder);
        all.stream()
                .sorted(Comparator.comparingLong(SavedProof::getTakenAt).reversed())
                .skip(MAX_SAVED)
                .forEach(old -> delete(old.getAccountHash(), old.getId()));
    }

    public void delete(long accountHash, String id) {
        File folder = folder(accountHash);
        new File(folder, id + ".png").delete();
        new File(folder, id + ".json").delete();
    }

    public List<LoadedProof> load(long accountHash) {
        File folder = folder(accountHash);
        List<LoadedProof> loaded = new ArrayList<>();
        for (SavedProof saved : readAll(folder)) {
            try {
                byte[] png = Files.readAllBytes(new File(folder, saved.getId() + ".png").toPath());
                BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(png));
                if (screenshot != null) {
                    loaded.add(new LoadedProof(saved, screenshot, png));
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

    private List<SavedProof> readAll(File folder) {
        File[] files = folder.listFiles((directory, name) -> name.endsWith(".json"));
        List<SavedProof> all = new ArrayList<>();
        if (files == null) {
            return all;
        }
        for (File file : files) {
            try {
                SavedProof saved = gson.fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8),
                        SavedProof.class);
                if (saved != null && saved.getId() != null) {
                    all.add(saved);
                }
            } catch (IOException | JsonParseException exception) {
                file.delete();
            }
        }
        return all;
    }

    private static File folder(long accountHash) {
        return new File(ROOT, Long.toString(accountHash));
    }
}
