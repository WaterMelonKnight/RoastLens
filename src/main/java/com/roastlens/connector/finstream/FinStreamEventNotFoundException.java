package com.roastlens.connector.finstream;

public class FinStreamEventNotFoundException extends FinStreamClientException {
    public FinStreamEventNotFoundException(String eventId) {
        super("FinStream event not found: " + eventId);
    }
}
