package com.detectorfodmap.app.data;

import com.detectorfodmap.app.util.TextNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cruza la lista de ingredientes de una etiqueta con la tabla FODMAP.
 *
 * Para cada ingrediente se busca la coincidencia más específica de la tabla (la
 * más larga gana, así «leche de almendra» no se confunde con «leche») y se
 * revisan además una serie de marcadores que no son alimentos pero sí son
 * FODMAP: inulina, fibra de achicoria, jarabes de fructosa, polioles...
 */
public final class IngredientAnalysis {

    /** Puntuación mínima para aceptar que un trozo de la etiqueta es un alimento de la tabla. */
    private static final int MIN_SCORE = 700;

    /** {término normalizado, nivel, etiqueta visible}. Ganan los términos más largos. */
    private static final String[][] MARKERS = {
            {"fructooligosacaridos", Food.LEVEL_HIGH, "Fructooligosacáridos"},
            {"galactooligosacaridos", Food.LEVEL_HIGH, "Galactooligosacáridos"},
            {"oligofructosa", Food.LEVEL_HIGH, "Oligofructosa"},
            {"fibra de achicoria", Food.LEVEL_HIGH, "Fibra de achicoria"},
            {"raiz de achicoria", Food.LEVEL_HIGH, "Raíz de achicoria"},
            {"achicoria", Food.LEVEL_HIGH, "Achicoria"},
            {"inulina", Food.LEVEL_HIGH, "Inulina"},
            {"jarabe de glucosa y fructosa", Food.LEVEL_HIGH, "Jarabe de glucosa y fructosa"},
            {"jarabe de glucosa fructosa", Food.LEVEL_HIGH, "Jarabe de glucosa-fructosa"},
            {"jarabe de fructosa", Food.LEVEL_HIGH, "Jarabe de fructosa"},
            {"jarabe de maiz", Food.LEVEL_HIGH, "Jarabe de maíz"},
            {"sirope de maiz", Food.LEVEL_HIGH, "Sirope de maíz"},
            {"fructosa", Food.LEVEL_HIGH, "Fructosa"},
            {"manitol", Food.LEVEL_HIGH, "Manitol (E421)"},
            {"maltitol", Food.LEVEL_HIGH, "Maltitol (E965)"},
            {"isomalt", Food.LEVEL_HIGH, "Isomalt (E953)"},
            {"lactitol", Food.LEVEL_HIGH, "Lactitol (E966)"},
            {"lactosa", Food.LEVEL_HIGH, "Lactosa"},
            {"e421", Food.LEVEL_HIGH, "Manitol (E421)"},
            {"e953", Food.LEVEL_HIGH, "Isomalt (E953)"},
            {"e965", Food.LEVEL_HIGH, "Maltitol (E965)"},
            {"e966", Food.LEVEL_HIGH, "Lactitol (E966)"},
            {"agave", Food.LEVEL_HIGH, "Agave"},
    };

    /**
     * Frases que descartan el trozo: son ingredientes bajos en FODMAP o leche sin
     * lactosa, y sin esto darían un falso aviso por contener «leche» o «trigo».
     */
    private static final String[] IGNORED = {
            "sin lactosa",
            "0 lactosa",
            "bajo en lactosa",
            "deslactosad",
            "suero de leche",
            "proteina de suero",
            "suero lacteo",
            "lactosuero",
            "caseinato",
            "trigo sarraceno",
            "alforfon",
            "leche de arroz",
            "bebida de arroz",
            "goma arabiga",
            "fibra de acacia",
            // Almidón, harina y aceite de maíz son bajos; el jarabe de maíz sí es alto.
            "almidon de maiz",
            "harina de maiz",
            "semola de maiz",
            "aceite de maiz",
            // La proteína aislada de guisante es baja en FODMAP.
            "proteina de guisante",
    };

    private IngredientAnalysis() {
    }

    public static Result analyze(List<Food> foods, String text) {
        if (text == null || text.trim().isEmpty()) {
            return new Result(Collections.<Hit>emptyList(), "", false);
        }

        Map<String, Hit> unique = new LinkedHashMap<>();
        for (String raw : IngredientParser.split(text)) {
            String segment = TextNormalizer.normalize(raw);
            if (segment.length() < 3 || isIgnored(segment)) {
                continue;
            }
            Hit hit = match(foods, segment, raw);
            if (hit != null && !unique.containsKey(hit.getKey())) {
                unique.put(hit.getKey(), hit);
            }
        }

        List<Hit> hits = new ArrayList<>(unique.values());
        // Primero los altos, después los moderados; dentro del mismo nivel, el orden de la etiqueta.
        hits.sort((left, right) -> Integer.compare(severity(right.getLevel()), severity(left.getLevel())));
        return new Result(hits, text.trim(), true);
    }

    private static Hit match(List<Food> foods, String segment, String matched) {
        Hit marker = matchMarker(segment, matched);

        Food best = null;
        int bestScore = 0;
        for (Food food : foods) {
            int score = food.matchInText(segment);
            if (score > bestScore) {
                bestScore = score;
                best = food;
            }
        }
        if (best == null || bestScore < MIN_SCORE) {
            return marker;
        }

        Hit foodHit = new Hit("food:" + best.getId(), best.getName(), best.getLevel(), best, matched);
        // Un marcador más grave manda: «jarabe de maíz» es alto aunque «maíz» sea moderado.
        if (marker != null && severity(marker.getLevel()) > severity(foodHit.getLevel())) {
            return marker;
        }
        return foodHit;
    }

    private static Hit matchMarker(String segment, String matched) {
        String bestTerm = null;
        String[] best = null;
        for (String[] marker : MARKERS) {
            if (marker[0].length() <= (bestTerm == null ? 0 : bestTerm.length())) {
                continue;
            }
            if (containsWord(segment, marker[0])) {
                bestTerm = marker[0];
                best = marker;
            }
        }
        if (best == null) {
            return null;
        }
        // La clave usa la etiqueta visible: «maltitol (E965)» se trocea en dos
        // segmentos que dan el mismo marcador y así no se duplica la fila.
        return new Hit("marker:" + best[2], best[2], best[1], null, matched);
    }

    private static boolean isIgnored(String segment) {
        for (String phrase : IGNORED) {
            if (segment.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    /** Busca el término como palabra completa, para que «ajo» no case con «ajonjolí». */
    private static boolean containsWord(String text, String term) {
        int index = text.indexOf(term);
        while (index >= 0) {
            boolean startsWord = index == 0 || text.charAt(index - 1) == ' ';
            int end = index + term.length();
            boolean endsWord = end == text.length() || text.charAt(end) == ' ';
            if (startsWord && endsWord) {
                return true;
            }
            index = text.indexOf(term, index + 1);
        }
        return false;
    }

    private static int severity(String level) {
        if (Food.LEVEL_HIGH.equals(level)) {
            return 3;
        }
        if (Food.LEVEL_MODERATE.equals(level)) {
            return 2;
        }
        return 1;
    }

    /** Un ingrediente de la etiqueta que merece un aviso. */
    public static final class Hit {

        private final String key;
        private final String label;
        private final String level;
        private final Food food;
        private final String matched;

        Hit(String key, String label, String level, Food food, String matched) {
            this.key = key;
            this.label = label;
            this.level = level;
            this.food = food;
            this.matched = matched;
        }

        public String getKey() {
            return key;
        }

        /** Nombre para enseñar: «Cebolla», «Maltitol (E965)»... */
        public String getLabel() {
            return label;
        }

        public String getLevel() {
            return level;
        }

        /** Alimento de la tabla al que se puede abrir la ficha, o null si es un marcador. */
        public Food getFood() {
            return food;
        }

        /** Trozo literal de la etiqueta que provocó el aviso. */
        public String getMatched() {
            return matched;
        }
    }

    /** Resultado del análisis de una etiqueta. */
    public static final class Result {

        private final List<Hit> hits;
        private final String text;
        private final boolean available;

        Result(List<Hit> hits, String text, boolean available) {
            this.hits = Collections.unmodifiableList(hits);
            this.text = text;
            this.available = available;
        }

        /** false si el producto no trae lista de ingredientes en Open Food Facts. */
        public boolean isAvailable() {
            return available;
        }

        /** Texto original de la etiqueta, para mostrarlo tal cual. */
        public String getText() {
            return text;
        }

        public List<Hit> getHits() {
            return hits;
        }

        public List<Hit> byLevel(String level) {
            List<Hit> filtered = new ArrayList<>();
            for (Hit hit : hits) {
                if (hit.getLevel().equals(level)) {
                    filtered.add(hit);
                }
            }
            return filtered;
        }

        /** Ingredientes que merecen aviso: moderados y altos, sin los que son bajos. */
        public List<Hit> getWarnings() {
            List<Hit> warnings = new ArrayList<>();
            for (Hit hit : hits) {
                if (!Food.LEVEL_LOW.equals(hit.getLevel())) {
                    warnings.add(hit);
                }
            }
            return warnings;
        }

        public boolean hasWarnings() {
            return !getWarnings().isEmpty();
        }
    }
}
