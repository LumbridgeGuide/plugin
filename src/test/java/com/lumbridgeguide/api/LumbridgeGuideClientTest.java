package com.lumbridgeguide.api;

import com.google.gson.Gson;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LumbridgeGuideClientTest {

    private HttpServer server;
    private final Map<String, List<String>> apiKeyHeaders = new ConcurrentHashMap<>();
    private volatile int status = 200;
    private volatile String body = "{}";

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            List<String> keys = exchange.getRequestHeaders().get("X-API-Key");
            apiKeyHeaders.put(exchange.getRequestURI().getPath(), keys == null ? List.of() : keys);
            if (exchange.getRequestURI().getPath().endsWith("/moved")) {
                exchange.getResponseHeaders().add("Location", "/elsewhere");
                exchange.sendResponseHeaders(302, -1);
            } else {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        System.setProperty("lumbridgeguide.api.base",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/api");
    }

    @After
    public void stopServer() {
        server.stop(0);
        System.clearProperty("lumbridgeguide.api.base");
    }

    @Test
    public void sendsTheTrimmedApiKey() throws InterruptedException {
        get(client("  key-123  "), "/plugin/sync");

        assertEquals(List.of("key-123"), apiKeyHeaders.get("/api/plugin/sync"));
    }

    @Test
    public void sendsNoApiKeyHeaderForABlankKey() throws InterruptedException {
        get(client("   "), "/plugin/sync");

        assertEquals(List.of(), apiKeyHeaders.get("/api/plugin/sync"));
    }

    @Test
    public void reportsANetworkErrorAsMinusOne() throws InterruptedException {
        server.stop(0);

        ApiResponse response = get(client("key-123"), "/plugin/sync");

        assertEquals(-1, response.getStatusCode());
    }

    private LumbridgeGuideClient client(String apiKey) {
        LumbridgeGuideConfig config = new LumbridgeGuideConfig() {
            @Override
            public String apiKey() {
                return apiKey;
            }
        };
        return new LumbridgeGuideClient(new OkHttpClient(), new Gson(), config);
    }

    /** Waits for either callback and returns its response, or null when the success callback ran */
    private static ApiResponse get(LumbridgeGuideClient client, String path) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<ApiResponse> failure = new AtomicReference<>();
        client.get(path, response -> done.countDown(), response -> {
            failure.set(response);
            done.countDown();
        });
        assertTrue("no callback ran", done.await(5, TimeUnit.SECONDS));
        return failure.get();
    }
}
