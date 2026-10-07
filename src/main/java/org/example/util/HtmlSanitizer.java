package org.example.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public final class HtmlSanitizer {

    // Политика: разрешены только базовое форматирование и ссылки
    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS);

    private HtmlSanitizer() {}

    /** Очищает пользовательский ввод от потенциально опасных тегов/скриптов. */
    public static String sanitize(String input) {
        if (input == null) return null;
        return POLICY.sanitize(input);
    }
}
