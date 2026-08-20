package com.glucobite.recipe.youtube;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeImportTimeoutBudgetTest {

    @Test
    void keepsYouTubeFetchWithinTwentySeconds() {
        Duration maximumYouTubeDuration = PublicYouTubeTranscriptProvider.REQUEST_TIMEOUT
                .multipliedBy(PublicYouTubeTranscriptProvider.MAX_EXTERNAL_REQUESTS);

        assertThat(maximumYouTubeDuration).isLessThanOrEqualTo(Duration.ofSeconds(20));
    }
}
