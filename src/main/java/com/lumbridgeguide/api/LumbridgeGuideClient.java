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
    private static final String DEFAULT_API_BASE_URL = "https://api.lumbridge.guide/api";

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

    public boolean hasApiKey() {
        String apiKey = config.apiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public void get(String path, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        Request request = newRequestBuilder(resolveUrl(path))
                .get()
                .build();
        executeAsync(request, onSuccess, onFailure);
    }

    public <T> void post(String path, T body, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        Request request = newRequestBuilder(resolveUrl(path))
                .post(jsonBody(body))
                .build();
        executeAsync(request, onSuccess, onFailure);
    }

    public <T> T deserialize(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    private <T> RequestBody jsonBody(T body) {
        return RequestBody.create(JSON_MEDIA_TYPE, gson.toJson(body));
    }

    private static HttpUrl buildApiBaseUrl() {
        HttpUrl url = HttpUrl.parse(System.getProperty(API_BASE_PROPERTY, DEFAULT_API_BASE_URL));
        return url != null ? url : HttpUrl.get(DEFAULT_API_BASE_URL);
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
}
