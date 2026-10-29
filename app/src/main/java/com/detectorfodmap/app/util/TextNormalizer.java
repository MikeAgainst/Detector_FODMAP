package com.detectorfodmap.app.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Normaliza texto (tildes, mayúsculas, signos) y calcula similitud entre cadenas
 * para que la búsqueda tolere errores de escritura.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        StringBuilder builder = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            builder.append(Character.isLetterOrDigit(c) ? c : ' ');
        }
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    /** Devuelve true si todas las palabras de la consulta empiezan alguna palabra del candidato. */
    public static boolean matchesAllWords(String query, String candidate) {
        String[] queryWords = query.split(" ");
        String[] candidateWords = candidate.split(" ");
        for (String word : queryWords) {
            if (word.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (String candidateWord : candidateWords) {
                if (candidateWord.startsWith(word)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /** Coeficiente de Dice sobre bigramas: 1 = idénticas, 0 = sin parecido. */
    public static double similarity(String a, String b) {
        if (a.equals(b)) {
            return 1d;
        }
        if (a.length() < 2 || b.length() < 2) {
            return 0d;
        }
        Map<String, Integer> bigrams = new HashMap<>();
        for (int i = 0; i < a.length() - 1; i++) {
            String gram = a.substring(i, i + 2);
            Integer count = bigrams.get(gram);
            bigrams.put(gram, count == null ? 1 : count + 1);
        }
        int intersection = 0;
        for (int i = 0; i < b.length() - 1; i++) {
            String gram = b.substring(i, i + 2);
            Integer count = bigrams.get(gram);
            if (count != null && count > 0) {
                intersection++;
                bigrams.put(gram, count - 1);
            }
        }
        return (2d * intersection) / (double) (a.length() + b.length() - 2);
    }
}
