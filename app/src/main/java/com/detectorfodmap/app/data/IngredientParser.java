package com.detectorfodmap.app.data;

import com.detectorfodmap.app.util.TextNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Trocea la lista de ingredientes de una etiqueta en ingredientes sueltos.
 *
 * Las etiquetas reales vienen con porcentajes («avellanas (12%)»), notas entre
 * paréntesis, asteriscos y marcas de alérgenos. Aquí se limpia todo eso y se
 * separa por comas, puntos, puntos y comas y paréntesis, que es como se separan
 * los ingredientes en la práctica.
 */
public final class IngredientParser {

    private static final Pattern LABEL = Pattern.compile(
            "(?i)\\b(ingredientes|ingredients|ingrédients|ingredienti)\\s*:");
    private static final Pattern PERCENT = Pattern.compile("\\d+([.,]\\d+)?\\s*%");
    private static final Pattern SPLIT = Pattern.compile("[,;.:()\\[\\]{}/|+&]");
    private static final Pattern MARKS = Pattern.compile("[*†‡•·]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private IngredientParser() {
    }

    /**
     * Devuelve los trozos de la etiqueta, sin limpiar tildes ni mayúsculas, para
     * poder enseñarlos tal y como aparecen en el envase.
     */
    public static List<String> split(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return segments;
        }

        String clean = LABEL.matcher(text).replaceAll(" ");
        // Se conserva el número para poder reconocer «0% lactosa».
        clean = PERCENT.matcher(clean).replaceAll(" $0 ");
        clean = MARKS.matcher(clean).replaceAll(" ");

        for (String raw : SPLIT.split(clean)) {
            String segment = SPACES.matcher(raw).replaceAll(" ").trim();
            if (segment.isEmpty()) {
                continue;
            }
            // Descarta trozos que no son ingredientes («y», «de», «puede contener»...)
            if (TextNormalizer.normalize(segment).length() < 3) {
                continue;
            }
            segments.add(segment);
        }
        return segments;
    }
}
