package com.roastlens.content;

public class ContentItemNotFoundException extends RuntimeException {
    public ContentItemNotFoundException(String id) { super("Content item not found: " + id); }
}
