package com.lumbridgeguide.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lumbridgeguide.LumbridgeGuideConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class LumbridgeGuideClient {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_BASE_PROPERTY = "lumbridgeguide.api.base";

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final LumbridgeGuideConfig config;
    @Getter
    private final HttpUrl apiBaseUrl;

    @Inject
    public LumbridgeGuideClient(OkHttpClient httpClient, LumbridgeGuideConfig config) {
        this.httpClient = httpClient;
        this.gson = new GsonBuilder().create();
        this.config = config;
        this.apiBaseUrl = buildApiBaseUrl();
    }

    public HttpUrl resolveUrl(String path) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        HttpUrl.Builder urlBuilder = apiBaseUrl.newBuilder();
        for (String segment : cleanPath.split("/")) {
            if (!segment.isEmpty()) {
                urlBuilder.addPathSegment(segment);
            }
        }
        return urlBuilder.build();
    }

    public boolean isAuthenticated() {
        String apiKey = config.apiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Sends an asynchronous {@code GET} request
    */
    public void get(String path, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        Request request = newRequestBuilder(resolveUrl(path))
                .get()
                .build();
        executeAsync(request, onSuccess, onFailure);
    }
    /**
     * Sends an asynchronous {@code POST} request with a JSON body
     */
    public <T> void post(String path, T body, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        Request request = newRequestBuilder(resolveUrl(path))
                .post(jsonBody(body))
                .build();
        executeAsync(request, onSuccess, onFailure);
    }

    /**
     * Sends an asynchronous {@code PUT} request with a JSON body
     */
    public <T> void put(String path, T body, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        Request request = newRequestBuilder(resolveUrl(path))
                .put(jsonBody(body))
                .build();
        executeAsync(request, onSuccess, onFailure);
    }

    /**
     * Sends an asynchronous {@code PATCH} request with a JSON body
     */
    public <T> void patch(String path, T body, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        Request request = newRequestBuilder(resolveUrl(path))
                .patch(jsonBody(body))
                .build();
        executeAsync(request, onSuccess, onFailure);
    }

    /**
     * Sends an asynchronous {@code DELETE} request
     */
    public void delete(String path, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        Request request = newRequestBuilder(resolveUrl(path))
                .delete()
                .build();
        executeAsync(request, onSuccess, onFailure);
    }

    /**
     * Sends a synchronous {@code POST} request
     * Prefer the async variant; use this only on a background thread
     */
    public <T> ApiResponse postSync(String path, T body) {
        Request request = newRequestBuilder(resolveUrl(path))
                .post(jsonBody(body))
                .build();
        return executeSync(request);
    }

    /**
     * Sends a synchronous {@code GET} request
     * Prefer the async variant; use this only on a background thread
     */
    public ApiResponse getSync(String path) {
        Request request = newRequestBuilder(resolveUrl(path))
                .get()
                .build();
        return executeSync(request);
    }

    public <T> T deserialize(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    private <T> RequestBody jsonBody(T body) {
        return RequestBody.create(JSON_MEDIA_TYPE, gson.toJson(body));
    }

    private static HttpUrl buildApiBaseUrl() {
        String obfuscatedDefault = ApiConfig.getApiBaseUrl();
        String base = System.getProperty(API_BASE_PROPERTY, obfuscatedDefault);
        HttpUrl url = HttpUrl.parse(base);
        if (url == null) {
            url = HttpUrl.parse(obfuscatedDefault);
        }
        if (url == null) {
            throw new IllegalStateException("Both configured and generated API base URLs are invalid");
        }
        return url;
    }

    private Request.Builder newRequestBuilder(HttpUrl url) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        String apiKey = config.apiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestBuilder.header(API_KEY_HEADER, apiKey.trim());
        }
        return requestBuilder;
    }

    private void executeAsync(Request request, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException exception) {
                if (onFailure != null) {
                    onFailure.accept(ApiResponse.error(-1, "Network error: " + exception.getMessage()));
                }
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String bodyContent = responseBody != null ? responseBody.string() : "";
                    ApiResponse apiResponse = new ApiResponse(
                            response.code(), bodyContent, response.isSuccessful());

                    if (response.isSuccessful()) {
                        if (onSuccess != null) {
                            onSuccess.accept(apiResponse);
                        }
                    } else {
                        if (onFailure != null) {
                            onFailure.accept(apiResponse);
                        }
                    }
                }
            }
        });
    }

    private ApiResponse executeSync(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String bodyContent = responseBody != null ? responseBody.string() : "";
            return new ApiResponse(response.code(), bodyContent, response.isSuccessful());
        } catch (IOException exception) {
            return ApiResponse.error(-1, "Network error: " + exception.getMessage());
        }
    }
}
