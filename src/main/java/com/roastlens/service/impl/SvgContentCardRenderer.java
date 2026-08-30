package com.roastlens.service.impl;

import com.roastlens.service.ContentCardRenderer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class SvgContentCardRenderer implements ContentCardRenderer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);
    private static final int MAX_LINE_UNITS = 32;
    private static final int MAX_LINES = 9;

    @Override
    public String render(CardContent content) {
        List<String> lines = wrap(content.reviewedText());
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            text.append("<tspan x=\"96\" dy=\"").append(i == 0 ? "0" : "72").append("\">")
                    .append(escape(lines.get(i))).append("</tspan>");
        }
        Instant timestamp = content.eventTime() != null ? content.eventTime() : content.detectedAt();
        String time = timestamp == null ? "Time unavailable" : TIME.format(timestamp);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1200" viewBox="0 0 1200 1200" role="img" aria-label="RoastLens social card">
                  <rect width="1200" height="1200" fill="#0f1115"/>
                  <rect x="48" y="48" width="1104" height="1104" rx="42" fill="#171b22" stroke="#2b3240" stroke-width="3"/>
                  <circle cx="104" cy="116" r="22" fill="#4f8cff"/>
                  <text x="144" y="132" fill="#e9edf4" font-size="50" font-weight="700" font-family="system-ui,-apple-system,'Segoe UI','Noto Sans CJK SC','Noto Sans',sans-serif">RoastLens</text>
                  <text x="96" y="238" fill="#4f8cff" font-size="34" font-weight="700" font-family="system-ui,-apple-system,'Segoe UI','Noto Sans CJK SC','Noto Sans',sans-serif">%s</text>
                  <text x="96" y="294" fill="#98a2b3" font-size="28" font-family="system-ui,-apple-system,'Segoe UI','Noto Sans CJK SC','Noto Sans',sans-serif">%s</text>
                  <text x="96" y="400" fill="#e9edf4" font-size="48" font-weight="600" font-family="system-ui,-apple-system,'Segoe UI','Noto Sans CJK SC','Noto Sans',sans-serif">%s</text>
                  <line x1="96" y1="1060" x2="1104" y2="1060" stroke="#2b3240" stroke-width="2"/>
                  <text x="96" y="1110" fill="#98a2b3" font-size="25" font-family="system-ui,-apple-system,'Segoe UI','Noto Sans CJK SC','Noto Sans',sans-serif">%s · %s</text>
                </svg>
                """.formatted(escape(value(content.symbol(), "MARKET")), escape(value(content.eventType(), "MARKET EVENT")),
                text, escape(value(content.source(), "FinStream")), escape(time));
    }

    private List<String> wrap(String input) {
        List<String> result = new ArrayList<>();
        String normalized = input.replace("\r\n", "\n").replace('\r', '\n').trim();
        StringBuilder line = new StringBuilder();
        int units = 0;
        for (int offset = 0; offset < normalized.length();) {
            int cp = normalized.codePointAt(offset);
            offset += Character.charCount(cp);
            if (cp == '\n') {
                result.add(line.toString()); line.setLength(0); units = 0;
                if (result.size() == MAX_LINES) break;
                continue;
            }
            int width = cp <= 0x7f ? 1 : 2;
            if (units + width > MAX_LINE_UNITS && line.length() > 0) {
                result.add(line.toString().stripTrailing()); line.setLength(0); units = 0;
                if (result.size() == MAX_LINES) break;
                if (Character.isWhitespace(cp)) continue;
            }
            line.appendCodePoint(cp); units += width;
        }
        if (result.size() < MAX_LINES && line.length() > 0) result.add(line.toString().stripTrailing());
        boolean truncated = result.size() == MAX_LINES && normalized.codePoints().map(cp -> cp <= 0x7f ? 1 : 2).sum() > MAX_LINE_UNITS * MAX_LINES;
        if (truncated) {
            int last = result.size() - 1;
            result.set(last, result.get(last).stripTrailing() + "…");
        }
        return result;
    }

    private String value(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input;
    }

    static String escape(String value) {
        if (value == null) return "";
        StringBuilder xmlSafe = new StringBuilder();
        value.codePoints().filter(SvgContentCardRenderer::isValidXmlCharacter).forEach(xmlSafe::appendCodePoint);
        return xmlSafe.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static boolean isValidXmlCharacter(int codePoint) {
        return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
                || codePoint >= 0x20 && codePoint <= 0xD7FF
                || codePoint >= 0xE000 && codePoint <= 0xFFFD
                || codePoint >= 0x10000 && codePoint <= 0x10FFFF;
    }
}
