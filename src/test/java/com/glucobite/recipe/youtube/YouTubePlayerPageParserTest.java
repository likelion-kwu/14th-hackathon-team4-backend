package com.glucobite.recipe.youtube;

import com.glucobite.recipe.exception.YouTubeFetchException;
import com.glucobite.recipe.exception.YouTubeTranscriptUnavailableException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouTubePlayerPageParserTest {

    private final YouTubePlayerPageParser parser = new YouTubePlayerPageParser(
            new ObjectMapper()
    );

    @Test
    void extractsMetadataAndPrefersKoreanTranscript() {
        String page = """
                <script>
                var ytInitialPlayerResponse = {
                  "playabilityStatus":{"status":"OK"},
                  "videoDetails":{
                    "title":"{한식} 계란 볶음밥",
                    "thumbnail":{"thumbnails":[
                      {"url":"https://i.ytimg.com/low.jpg"},
                      {"url":"https://i.ytimg.com/high.jpg"}
                    ]}
                  },
                  "captions":{"playerCaptionsTracklistRenderer":{"captionTracks":[
                    {"baseUrl":"https://www.youtube.com/api/timedtext?v=test&lang=en","languageCode":"en"},
                    {"baseUrl":"https://www.youtube.com/api/timedtext?v=test&lang=ko","languageCode":"ko"}
                  ]}}
                };
                </script>
                """;

        YouTubePlayerPageParser.PlayerData result = parser.parsePlayerData(page);

        assertThat(result.title()).isEqualTo("{한식} 계란 볶음밥");
        assertThat(result.thumbnailUrl()).isEqualTo("https://i.ytimg.com/high.jpg");
        assertThat(result.transcriptUrl()).contains("lang=ko");
    }

    @Test
    void extractsInnerTubeApiKey() {
        assertThat(parser.extractInnerTubeApiKey(
                "{\"INNERTUBE_API_KEY\":\"AIza-test_key123\"}"
        )).isEqualTo("AIza-test_key123");
    }

    @Test
    void prefersOriginalAsrTrackOverTranslatedTrack() {
        YouTubePlayerPageParser.PlayerData result = parser.parsePlayerJson("""
                {
                  "playabilityStatus":{"status":"OK"},
                  "videoDetails":{"title":"영어 영상"},
                  "captions":{"playerCaptionsTracklistRenderer":{"captionTracks":[
                    {"baseUrl":"https://www.youtube.com/api/timedtext?v=test&lang=en&translated=true","languageCode":"en"},
                    {"baseUrl":"https://www.youtube.com/api/timedtext?v=test&lang=en&kind=asr","languageCode":"en","kind":"asr"}
                  ]}}
                }
                """);

        assertThat(result.transcriptUrl()).contains("kind=asr");
    }

    @Test
    void parsesAndDecodesTranscriptXml() {
        String result = parser.parseTranscript("""
                <?xml version="1.0" encoding="utf-8" ?>
                <transcript>
                  <text start="0" dur="1">양파를 &amp; 당근을 썹니다.</text>
                  <text start="1" dur="2">팬에서 볶습니다.</text>
                </transcript>
                """);

        assertThat(result).isEqualTo("양파를 & 당근을 썹니다. 팬에서 볶습니다.");
    }

    @Test
    void rejectsMissingTranscriptAndUnplayableVideo() {
        assertThatThrownBy(() -> parser.parsePlayerData("""
                var ytInitialPlayerResponse = {
                  "playabilityStatus":{"status":"OK"},
                  "videoDetails":{"title":"자막 없음"}
                };
                """))
                .isInstanceOf(YouTubeTranscriptUnavailableException.class);

        assertThatThrownBy(() -> parser.parsePlayerData("""
                var ytInitialPlayerResponse = {
                  "playabilityStatus":{"status":"ERROR"}
                };
                """))
                .isInstanceOf(YouTubeTranscriptUnavailableException.class);
    }

    @Test
    void rejectsExternalTranscriptUrisAndXmlEntities() {
        assertThatThrownBy(() -> parser.validateTranscriptUri(
                "https://www.youtube.com.evil.example/api/timedtext?v=test"
        )).isInstanceOf(YouTubeFetchException.class);
        assertThatThrownBy(() -> parser.validateTranscriptUri(
                "http://www.youtube.com/api/timedtext?v=test"
        )).isInstanceOf(YouTubeFetchException.class);

        assertThatThrownBy(() -> parser.parseTranscript("""
                <!DOCTYPE transcript [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <transcript><text>&xxe;</text></transcript>
                """))
                .isInstanceOf(YouTubeFetchException.class);
    }

    @Test
    void dropsUntrustedThumbnailUrl() {
        YouTubePlayerPageParser.PlayerData result = parser.parsePlayerData("""
                var ytInitialPlayerResponse = {
                  "playabilityStatus":{"status":"OK"},
                  "videoDetails":{
                    "title":"레시피",
                    "thumbnail":{"thumbnails":[{"url":"https://evil.example/image.jpg"}]}
                  },
                  "captions":{"playerCaptionsTracklistRenderer":{"captionTracks":[
                    {"baseUrl":"https://www.youtube.com/api/timedtext?v=test&lang=ko","languageCode":"ko"}
                  ]}}
                };
                """);

        assertThat(result.thumbnailUrl()).isNull();
    }

}
