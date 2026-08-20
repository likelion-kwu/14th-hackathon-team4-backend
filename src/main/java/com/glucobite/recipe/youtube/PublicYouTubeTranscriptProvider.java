package com.glucobite.recipe.youtube;

import com.glucobite.recipe.exception.YouTubeFetchException;
import com.glucobite.recipe.exception.YouTubeTranscriptUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class PublicYouTubeTranscriptProvider implements YouTubeTranscriptProvider {

    private static final int MAX_PLAYER_PAGE_BYTES = 5_000_000;
    private static final int MAX_TRANSCRIPT_BYTES = 2_000_000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; GlucobiteRecipeImporter/1.0)";
    private static final List<InnerTubeClient> INNERTUBE_CLIENTS = List.of(
            new InnerTubeClient(
                    "ANDROID",
                    "20.10.38",
                    "com.google.android.youtube/20.10.38 (Linux; U; Android 13) gzip"
            ),
            new InnerTubeClient(
                    "IOS",
                    "20.10.4",
                    "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)"
            )
    );

    private final HttpClient httpClient;
    private final YouTubePlayerPageParser parser;

    @Autowired
    public PublicYouTubeTranscriptProvider(YouTubePlayerPageParser parser) {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                parser
        );
    }

    PublicYouTubeTranscriptProvider(HttpClient httpClient, YouTubePlayerPageParser parser) {
        this.httpClient = httpClient;
        this.parser = parser;
    }

    @Override
    public YouTubeVideoContent fetch(YouTubeVideoReference reference) {
        String page = fetchText(URI.create(reference.canonicalUrl()), MAX_PLAYER_PAGE_BYTES);
        String apiKey = parser.extractInnerTubeApiKey(page);
        YouTubeFetchException lastFetchFailure = null;
        YouTubeTranscriptUnavailableException lastUnavailable = null;
        for (InnerTubeClient client : INNERTUBE_CLIENTS) {
            try {
                String playerJson = fetchInnerTubePlayer(reference.videoId(), apiKey, client);
                YouTubePlayerPageParser.PlayerData playerData = parser.parsePlayerJson(playerJson);
                URI transcriptUri = parser.validateTranscriptUri(playerData.transcriptUrl());
                String transcriptXml = fetchText(transcriptUri, MAX_TRANSCRIPT_BYTES);
                if (transcriptXml.isBlank()) {
                    throw new YouTubeTranscriptUnavailableException(
                            "YouTube 자막 응답이 비어 있습니다."
                    );
                }
                String transcript = parser.parseTranscript(transcriptXml);
                return new YouTubeVideoContent(
                        reference.videoId(),
                        reference.canonicalUrl(),
                        playerData.title(),
                        playerData.thumbnailUrl(),
                        transcript
                );
            } catch (YouTubeTranscriptUnavailableException exception) {
                lastUnavailable = exception;
            } catch (YouTubeFetchException exception) {
                lastFetchFailure = exception;
            }
        }
        if (lastFetchFailure != null) {
            throw lastFetchFailure;
        }
        if (lastUnavailable != null) {
            throw lastUnavailable;
        }
        throw new YouTubeTranscriptUnavailableException("사용 가능한 YouTube 자막이 없습니다.");
    }

    private String fetchInnerTubePlayer(
            String videoId,
            String apiKey,
            InnerTubeClient client
    ) {
        URI uri = URI.create("https://www.youtube.com/youtubei/v1/player?key=" + apiKey);
        String requestBody = """
                {
                  "context": {
                    "client": {
                      "clientName": "%s",
                      "clientVersion": "%s"
                    }
                  },
                  "videoId": "%s",
                  "contentCheckOk": true,
                  "racyCheckOk": true
                }
                """.formatted(client.name(), client.version(), videoId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", client.userAgent())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        return sendText(request, MAX_PLAYER_PAGE_BYTES);
    }

    private String fetchText(URI uri, int maximumBytes) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8")
                .GET()
                .build();
        return sendText(request, maximumBytes);
    }

    private String sendText(HttpRequest request, int maximumBytes) {
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new YouTubeFetchException(
                            "YouTube 응답 상태가 올바르지 않습니다: " + response.statusCode()
                    );
                }
                byte[] bytes = body.readNBytes(maximumBytes + 1);
                if (bytes.length > maximumBytes) {
                    throw new YouTubeFetchException("YouTube 응답 크기가 제한을 초과했습니다.");
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new YouTubeFetchException("YouTube 요청이 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new YouTubeFetchException("YouTube 요청에 실패했습니다.", exception);
        }
    }

    private record InnerTubeClient(
            String name,
            String version,
            String userAgent
    ) {
    }
}
