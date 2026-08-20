package com.glucobite.recipe.exception;

public class InvalidYouTubeUrlException extends RuntimeException {

    public InvalidYouTubeUrlException() {
        super("지원하는 YouTube URL이 아닙니다.");
    }
}
