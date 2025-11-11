package com.hokori.web.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {
    private SlugUtil() {}
    public static String toSlug(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+","")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]","")
                .trim()
                .replaceAll("\\s+","-");
        return s.length() > 180 ? s.substring(0,180) : s;
    }
}
