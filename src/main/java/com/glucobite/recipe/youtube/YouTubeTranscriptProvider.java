package com.glucobite.recipe.youtube;

public interface YouTubeTranscriptProvider {

    YouTubeVideoContent fetch(YouTubeVideoReference reference);
}
