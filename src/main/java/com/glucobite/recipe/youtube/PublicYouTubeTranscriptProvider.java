package com.glucobite.recipe.youtube;

import com.glucobite.recipe.exception.YouTubeFetchException;
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

@Component
public class PublicYouTubeTranscriptProvider implements YouTubeTranscriptProvider {

    private static final int MAX_PLAYER_PAGE_BYTES = 5_000_000;
    private static final int MAX_TRANSCRIPT_BYTES = 2_000_000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; GlucobiteRecipeImporter/1.0)";

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
        YouTubePlayerPageParser.PlayerData playerData = parser.parsePlayerData(page);
        URI transcriptUri = parser.validateTranscriptUri(playerData.transcriptUrl());
        String transcriptXml = fetchText(transcriptUri, MAX_TRANSCRIPT_BYTES);
        String transcript = parser.parseTranscript(transcriptXml);
        return new YouTubeVideoContent(
                reference.videoId(),
                reference.canonicalUrl(),
                playerData.title(),
                playerData.thumbnailUrl(),
                transcript
        );
    }

    private String fetchText(URI uri, int maximumBytes) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8")
                .GET()
                .build();
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
}
