package com.anjing.aigc.provider.mock;

import com.anjing.aigc.model.enums.ContentType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class MockAssetFactory {

    private MockAssetFactory() {
    }

    static String createSvgDataUri(ContentType contentType, String prompt) {
        String title = switch (contentType) {
            case IMAGE -> "Mock Image";
            case VIDEO -> "Mock Video";
            case AUDIO -> "Mock Audio";
            case TEXT -> "Mock Text";
        };
        String accent = switch (contentType) {
            case IMAGE -> "#2f80ed";
            case VIDEO -> "#27ae60";
            case AUDIO -> "#9b51e0";
            case TEXT -> "#f2994a";
        };
        String safePrompt = escapeXml(truncate(prompt, 72));
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720">
                  <rect width="1280" height="720" fill="#101828"/>
                  <rect x="80" y="80" width="1120" height="560" rx="24" fill="#f8fafc"/>
                  <circle cx="1080" cy="180" r="88" fill="%s" opacity="0.18"/>
                  <circle cx="210" cy="545" r="120" fill="%s" opacity="0.12"/>
                  <text x="140" y="230" font-family="Arial, Helvetica, sans-serif" font-size="56" font-weight="700" fill="#111827">%s</text>
                  <text x="140" y="315" font-family="Arial, Helvetica, sans-serif" font-size="30" fill="#475467">agent-aigc local preview</text>
                  <text x="140" y="410" font-family="Arial, Helvetica, sans-serif" font-size="34" fill="#1f2937">%s</text>
                  <rect x="140" y="500" width="260" height="10" rx="5" fill="%s"/>
                  <rect x="140" y="530" width="520" height="10" rx="5" fill="#d0d5dd"/>
                </svg>
                """.formatted(accent, accent, title, safePrompt, accent);
        return "data:image/svg+xml;charset=UTF-8," + URLEncoder.encode(svg, StandardCharsets.UTF_8);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
