package com.detectorfodmap.app.data;

import com.detectorfodmap.app.util.TextNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Food {

    public static final String LEVEL_LOW = "low";
    public static final String LEVEL_MODERATE = "moderate";
    public static final String LEVEL_HIGH = "high";

    private final String id;
    private final String name;
    private final String category;
    private final String level;
    private final String portion;
    private final String note;
    private final String emoji;
    private final String wikiTitle;
    private final List<String> fodmaps;
    private final List<String> searchTerms;
    private final List<String> matchTerms;

    public Food(String id,
                String name,
                List<String> aliases,
                String category,
                String level,
                List<String> fodmaps,
                String portion,
                String note,
                String emoji,
                String wikiTitle) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.level = level;
        this.fodmaps = Collections.unmodifiableList(new ArrayList<>(fodmaps));
        this.portion = portion;
        this.note = note;
        this.emoji = emoji == null ? "" : emoji;
        this.wikiTitle = wikiTitle == null || wikiTitle.trim().isEmpty() ? name : wikiTitle.trim();

        List<String> terms = new ArrayList<>();
        terms.add(TextNormalizer.normalize(name));
        terms.add(TextNormalizer.normalize(category));
        for (String alias : aliases) {
            terms.add(TextNormalizer.normalize(alias));
        }
        this.searchTerms = Collections.unmodifiableList(terms);

        List<String> matchOnly = new ArrayList<>();
        matchOnly.add(TextNormalizer.normalize(name));
        for (String alias : aliases) {
            matchOnly.add(TextNormalizer.normalize(alias));
        }
        this.matchTerms = Collections.unmodifiableList(matchOnly);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getLevel() {
        return level;
    }

    public String getPortion() {
        return portion;
    }

    public String getNote() {
        return note;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getWikiTitle() {
        return wikiTitle;
    }

    public List<String> getFodmaps() {
        return fodmaps;
    }

    /**
     * Puntúa el parecido con la consulta. 0 significa que no coincide.
     * Cuanto más alto, mejor posición en los resultados.
     */
    public int score(String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return 1;
        }
        int best = 0;
        for (String term : searchTerms) {
            if (term.equals(normalizedQuery)) {
                return 1000;
            }
            if (term.startsWith(normalizedQuery)) {
                best = Math.max(best, 900 - Math.min(80, term.length() - normalizedQuery.length()));
            } else if (term.contains(normalizedQuery)) {
                best = Math.max(best, 760 - Math.min(80, term.length() - normalizedQuery.length()));
            } else if (normalizedQuery.length() >= 4) {
                double similarity = TextNormalizer.similarity(normalizedQuery, term);
                if (similarity >= 0.65d) {
                    best = Math.max(best, (int) Math.round(520 * similarity));
                }
            }
            if (normalizedQuery.indexOf(' ') > 0 && TextNormalizer.matchesAllWords(normalizedQuery, term)) {
                best = Math.max(best, 820);
            }
        }
        return best;
    }

    /**
     * Busca el nombre o los sinónimos de este alimento dentro del nombre de un
     * producto (por ejemplo, «Bebida de leche de almendra Alpro»).
     * Devuelve 0 si no aparece.
     */
    public int matchInText(String normalizedText) {
        if (normalizedText.isEmpty()) {
            return 0;
        }
        int best = 0;
        for (String term : matchTerms) {
            if (term.length() < 3) {
                continue;
            }
            int index = normalizedText.indexOf(term);
            while (index >= 0) {
                boolean startsWord = index == 0 || normalizedText.charAt(index - 1) == ' ';
                int end = index + term.length();
                boolean endsWord = end == normalizedText.length() || normalizedText.charAt(end) == ' ';
                int score = 680 + Math.min(180, term.length() * 6);
                if (startsWord && endsWord) {
                    score += 90;
                }
                if (term.indexOf(' ') >= 0) {
                    score += 30;
                }
                best = Math.max(best, score);
                index = normalizedText.indexOf(term, index + 1);
            }
            if (term.indexOf(' ') > 0 && containsAllWords(term, normalizedText)) {
                best = Math.max(best, 660);
            }
        }
        return best;
    }

    private static boolean containsAllWords(String term, String text) {
        for (String word : term.split(" ")) {
            if (word.length() < 3) {
                continue;
            }
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
