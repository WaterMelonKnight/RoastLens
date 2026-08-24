package com.roastlens.content;

public class ContentLanguageConflictException extends RuntimeException {
    public ContentLanguageConflictException(String sourceEventId, String persistedLanguage, String requestedLanguage) {
        super("Event " + sourceEventId + " was already processed in " + persistedLanguage
                + " and cannot be returned as " + requestedLanguage);
    }
}
