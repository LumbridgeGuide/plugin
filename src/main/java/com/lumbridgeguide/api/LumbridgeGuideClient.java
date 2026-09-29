package com.lumbridgeguide.api;

import com.google.gson.Gson;
import com.lumbridgeguide.LumbridgeGuideConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class LumbridgeGuideClient {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_BASE_PROPERTY = "lumbridgeguide.api.base";
    private static final String DEFAULT_API_BASE_URL = "https://api.lumbridge.guide/api";
    private static final String WEB_BASE_PROPERTY = "lumbridgeguide.web.base";
    private static final String DEFAULT_WEB_BASE_URL = "https://lumbridge.guide";

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");
    private static final MediaType PNG_MEDIA_TYPE = MediaType.parse("image/png");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final LumbridgeGuideConfig config;
    @Getter
    private final HttpUrl apiBaseUrl;

    @Inject
    public LumbridgeGuideClient(OkHttpClient httpClient, Gson gson, LumbridgeGuideConfig config) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.config = config;
        this.apiBaseUrl = buildApiBaseUrl();
    }

    /** The website's home page, overridable like the API base for local development. */
    public static String websiteUrl() {
        return System.getProperty(WEB_BASE_PROPERTY, DEFAULT_WEB_BASE_URL);
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

    /** Posts form fields and a PNG image as multipart form data, for uploads such as proof screenshots. */
    public void postImage(String path, Map<String, String> fields, String imagePart, byte[] png,
                          Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
        MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM);
        fields.forEach(body::addFormDataPart);
        body.addFormDataPart(imagePart, imagePart + ".png", RequestBody.create(PNG_MEDIA_TYPE, png));
        Request request = newRequestBuilder(resolveUrl(path))
                .removeHeader("Content-Type")
                .post(body.build())
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
