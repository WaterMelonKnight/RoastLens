package com.roastlens.connector.finstream;

public class FinStreamClientException extends RuntimeException {
    public FinStreamClientException(String message) { super(message); }
    public FinStreamClientException(String message, Throwable cause) { super(message, cause); }
}
