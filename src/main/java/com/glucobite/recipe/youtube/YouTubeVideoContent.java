package com.glucobite.recipe.youtube;

public record YouTubeVideoContent(
        String videoId,
        String canonicalUrl,
        String title,
        String thumbnailUrl,
        String transcript
) {
}
