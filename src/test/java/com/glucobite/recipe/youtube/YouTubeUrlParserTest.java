package com.glucobite.recipe.youtube;

import com.glucobite.recipe.exception.InvalidYouTubeUrlException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouTubeUrlParserTest {

    private final YouTubeUrlParser parser = new YouTubeUrlParser();

    @Test
    void normalizesSupportedYouTubeUrls() {
        List<String> urls = List.of(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=10",
                "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ?t=10",
                "https://www.youtube.com/shorts/dQw4w9WgXcQ",
                "https://www.youtube.com/embed/dQw4w9WgXcQ",
                "https://www.youtube.com/live/dQw4w9WgXcQ"
        );

        urls.forEach(url -> assertThat(parser.parse(url))
                .isEqualTo(new YouTubeVideoReference(
                        "dQw4w9WgXcQ",
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                )));
    }

    @Test
    void rejectsUnsafeOrUnsupportedUrls() {
        List<String> urls = List.of(
                "http://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://www.youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
                "https://user@www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://www.youtube.com:444/watch?v=dQw4w9WgXcQ",
                "https://www.youtube.com/watch?v=too-short",
                "https://www.youtube.com/channel/dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ/extra",
                "not-a-url"
        );

        urls.forEach(url -> assertThatThrownBy(() -> parser.parse(url))
                .isInstanceOf(InvalidYouTubeUrlException.class));
    }
}
