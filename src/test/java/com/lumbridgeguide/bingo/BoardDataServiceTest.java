package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoardDataServiceTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void refreshWithoutAKeyClearsTheCachedBoards() throws IOException {
        File cacheFile = cachedBoard();
        BoardDataService service = new BoardDataService(client("  "), new Gson(), cacheFile);
        assertTrue(service.hasCachedData());

        service.refresh();

        assertTrue(service.getBoards().isEmpty());
        assertFalse(cacheFile.exists());
    }

    @Test
    public void rejectedKeyClearsTheCachedBoards() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        server.start();
        System.setProperty("lumbridgeguide.api.base", "http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        try {
            File cacheFile = cachedBoard();
            BoardDataService service = new BoardDataService(client("revoked-key"), new Gson(), cacheFile);
            CountDownLatch done = new CountDownLatch(1);

            service.refresh(done::countDown);

            assertTrue("refresh never finished", done.await(5, TimeUnit.SECONDS));
            assertTrue(service.getBoards().isEmpty());
            assertFalse(cacheFile.exists());
        } finally {
            server.stop(0);
            System.clearProperty("lumbridgeguide.api.base");
        }
    }

    private File cachedBoard() throws IOException {
        File cacheFile = folder.newFile("board-cache.json");
        Files.write(cacheFile.toPath(), "{\"boards\":[{}]}".getBytes(StandardCharsets.UTF_8));
        return cacheFile;
    }

    private static LumbridgeGuideClient client(String apiKey) {
        LumbridgeGuideConfig config = new LumbridgeGuideConfig() {
            @Override
            public String apiKey() {
                return apiKey;
            }
        };
        return new LumbridgeGuideClient(new OkHttpClient(), new Gson(), config);
    }
}
