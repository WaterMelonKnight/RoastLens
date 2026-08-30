package com.roastlens.service.impl;

import com.roastlens.service.ContentCardRenderer.CardContent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgContentCardRendererTest {
    private final SvgContentCardRenderer renderer = new SvgContentCardRenderer();

    @Test void rendersBrandEventAndApprovedText() {
        String svg = renderer.render(new CardContent("BTCUSDT", "RAPID_DROP", "Approved words",
                Instant.parse("2026-08-30T09:00:00Z"), null, "BINANCE"));
        assertTrue(svg.contains("RoastLens"));
        assertTrue(svg.contains("BTCUSDT"));
        assertTrue(svg.contains("RAPID_DROP"));
        assertTrue(svg.contains("Approved words"));
        assertTrue(svg.contains("1200"));
    }

    @Test void escapesEveryXmlSensitiveCharacterInsteadOfInjectingMarkup() {
        String svg = renderer.render(new CardContent("BTC<>&\"'", "DROP<script>",
                "approved <script>alert(1)</script> & \"quote\" 'apostrophe'", null, null, "FIN&STREAM"));
        assertTrue(svg.contains("BTC&lt;&gt;&amp;&quot;&apos;"));
        assertTrue(svg.contains("DROP&lt;script&gt;"));
        assertTrue(svg.contains("&lt;script&gt;"));
        assertTrue(svg.contains("FIN&amp;STREAM"));
        assertFalse(svg.contains("<script>"));
    }

    @Test void wrapsChineseAndEnglishIntoTspans() {
        String text = "市场正在快速变化而且成交量显著增加，需要保持冷静。 This is a longer English sentence that wraps deterministically.";
        String svg = renderer.render(new CardContent("BTC", "VOLUME", text, null, null, "FinStream"));
        assertTrue(svg.indexOf("<tspan") != svg.lastIndexOf("<tspan"));
    }
}
