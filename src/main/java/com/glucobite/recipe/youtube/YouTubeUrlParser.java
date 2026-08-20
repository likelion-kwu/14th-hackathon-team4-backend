package com.glucobite.recipe.youtube;

import com.glucobite.recipe.exception.InvalidYouTubeUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class YouTubeUrlParser {

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{11}");

    public YouTubeVideoReference parse(String rawUrl) {
        try {
            URI uri = new URI(rawUrl.trim());
            validateAuthority(uri);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            String videoId = switch (host) {
                case "youtu.be" -> parseShortUrl(uri);
                case "youtube.com", "www.youtube.com", "m.youtube.com" -> parseYouTubeUrl(uri);
                default -> throw new InvalidYouTubeUrlException();
            };
            validateVideoId(videoId);
            return new YouTubeVideoReference(
                    videoId,
                    "https://www.youtube.com/watch?v=" + videoId
            );
        } catch (URISyntaxException | NullPointerException exception) {
            throw new InvalidYouTubeUrlException();
        }
    }

    private void validateAuthority(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new InvalidYouTubeUrlException();
        }
    }

    private String parseShortUrl(URI uri) {
        String path = uri.getPath();
        if (path == null || path.length() != 12 || path.charAt(0) != '/') {
            throw new InvalidYouTubeUrlException();
        }
        return path.substring(1);
    }

    private String parseYouTubeUrl(URI uri) {
        String path = uri.getPath();
        if ("/watch".equals(path)) {
            return findVideoIdQuery(uri.getRawQuery());
        }
        if (path != null) {
            for (String prefix : new String[]{"/shorts/", "/embed/", "/live/"}) {
                if (path.startsWith(prefix) && path.length() == prefix.length() + 11) {
                    return path.substring(prefix.length());
                }
            }
        }
        throw new InvalidYouTubeUrlException();
    }

    private String findVideoIdQuery(String rawQuery) {
        if (rawQuery == null) {
            throw new InvalidYouTubeUrlException();
        }
        for (String parameter : rawQuery.split("&")) {
            int separator = parameter.indexOf('=');
            if (separator > 0 && "v".equals(parameter.substring(0, separator))) {
                return parameter.substring(separator + 1);
            }
        }
        throw new InvalidYouTubeUrlException();
    }

    private void validateVideoId(String videoId) {
        if (videoId == null || !VIDEO_ID_PATTERN.matcher(videoId).matches()) {
            throw new InvalidYouTubeUrlException();
        }
    }
}
