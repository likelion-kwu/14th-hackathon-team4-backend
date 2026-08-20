package com.glucobite.recipe.youtube;

import com.glucobite.recipe.exception.YouTubeFetchException;
import com.glucobite.recipe.exception.YouTubeTranscriptUnavailableException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class YouTubePlayerPageParser {

    private static final int MAX_TRANSCRIPT_CHARS = 50_000;

    private final ObjectMapper objectMapper;

    public YouTubePlayerPageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PlayerData parsePlayerData(String page) {
        try {
            String playerJson = extractPlayerJson(page);
            JsonNode root = objectMapper.readTree(playerJson);
            String status = text(root.path("playabilityStatus").path("status"));
            if (status != null && !"OK".equals(status)) {
                throw new YouTubeTranscriptUnavailableException(
                        "재생할 수 없는 YouTube 영상입니다."
                );
            }
            JsonNode details = root.path("videoDetails");
            String title = text(details.path("title"));
            String thumbnailUrl = lastThumbnail(details.path("thumbnail").path("thumbnails"));
            String transcriptUrl = selectTranscriptUrl(
                    root.path("captions")
                            .path("playerCaptionsTracklistRenderer")
                            .path("captionTracks")
            );
            return new PlayerData(title, thumbnailUrl, transcriptUrl);
        } catch (YouTubeTranscriptUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new YouTubeFetchException("YouTube 영상 정보를 해석하지 못했습니다.", exception);
        }
    }

    public String parseTranscript(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader(xml))
            );
            NodeList nodes = document.getElementsByTagName("text");
            StringBuilder transcript = new StringBuilder();
            for (int index = 0; index < nodes.getLength(); index++) {
                String content = nodes.item(index).getTextContent().trim();
                if (content.isEmpty()) {
                    continue;
                }
                if (!transcript.isEmpty()) {
                    transcript.append(' ');
                }
                transcript.append(content);
                if (transcript.length() > MAX_TRANSCRIPT_CHARS) {
                    throw new YouTubeTranscriptUnavailableException(
                            "YouTube 자막이 분석 허용 길이를 초과합니다."
                    );
                }
            }
            if (transcript.isEmpty()) {
                throw new YouTubeTranscriptUnavailableException(
                        "사용 가능한 YouTube 자막이 없습니다."
                );
            }
            return transcript.toString();
        } catch (YouTubeTranscriptUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new YouTubeFetchException("YouTube 자막을 해석하지 못했습니다.", exception);
        }
    }

    public URI validateTranscriptUri(String rawUri) {
        try {
            URI uri = URI.create(rawUri);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !("youtube.com".equalsIgnoreCase(host)
                    || "www.youtube.com".equalsIgnoreCase(host))
                    || !"/api/timedtext".equals(uri.getPath())
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw new YouTubeFetchException("허용되지 않은 YouTube 자막 URL입니다.");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new YouTubeFetchException("YouTube 자막 URL이 올바르지 않습니다.", exception);
        }
    }

    private String extractPlayerJson(String page) {
        for (String marker : List.of("var ytInitialPlayerResponse =", "ytInitialPlayerResponse =")) {
            int markerIndex = page.indexOf(marker);
            if (markerIndex >= 0) {
                int start = page.indexOf('{', markerIndex + marker.length());
                if (start >= 0) {
                    return extractBalancedObject(page, start);
                }
            }
        }
        int quotedKey = page.indexOf("\"ytInitialPlayerResponse\"");
        if (quotedKey >= 0) {
            int start = page.indexOf('{', quotedKey);
            if (start >= 0) {
                return extractBalancedObject(page, start);
            }
        }
        throw new YouTubeFetchException("YouTube player 응답이 없습니다.");
    }

    private String extractBalancedObject(String source, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < source.length(); index++) {
            char current = source.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new YouTubeFetchException("YouTube player JSON이 완전하지 않습니다.");
    }

    private String selectTranscriptUrl(JsonNode tracks) {
        if (!tracks.isArray() || tracks.isEmpty()) {
            throw new YouTubeTranscriptUnavailableException(
                    "사용 가능한 YouTube 자막이 없습니다."
            );
        }
        List<JsonNode> candidates = new ArrayList<>();
        tracks.forEach(candidates::add);
        return candidates.stream()
                .sorted((left, right) -> Integer.compare(priority(left), priority(right)))
                .map(track -> text(track.path("baseUrl")))
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElseThrow(() -> new YouTubeTranscriptUnavailableException(
                        "사용 가능한 YouTube 자막이 없습니다."
                ));
    }

    private int priority(JsonNode track) {
        String languageCode = text(track.path("languageCode"));
        if (languageCode != null && languageCode.startsWith("ko")) {
            return 0;
        }
        if (languageCode != null && languageCode.startsWith("en")) {
            return 1;
        }
        return 2;
    }

    private String lastThumbnail(JsonNode thumbnails) {
        if (!thumbnails.isArray() || thumbnails.isEmpty()) {
            return null;
        }
        String rawUrl = text(thumbnails.get(thumbnails.size() - 1).path("url"));
        if (rawUrl == null || rawUrl.length() > 500) {
            return null;
        }
        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !("ytimg.com".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".ytimg.com"))
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return null;
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    public record PlayerData(
            String title,
            String thumbnailUrl,
            String transcriptUrl
    ) {
    }
}
