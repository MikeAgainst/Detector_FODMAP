package com.detectorfodmap.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.detectorfodmap.app.data.Food;
import com.detectorfodmap.app.util.TextNormalizer;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class FoodSearchTest {

    private Food aguacate() {
        return new Food("aguacate", "Aguacate", Arrays.asList("palta", "avocado"),
                "Fruta", Food.LEVEL_LOW, Collections.emptyList(), "1/2 unidad", "", "🥑", "");
    }

    @Test
    public void ignoresCaseAndAccents() {
        assertEquals(1000, aguacate().score(TextNormalizer.normalize("AGUÁCATE")));
    }

    @Test
    public void findsFoodDespiteTypo() {
        assertTrue(aguacate().score(TextNormalizer.normalize("Aguaxate")) > 0);
    }

    @Test
    public void findsFoodByAlias() {
        assertEquals(1000, aguacate().score(TextNormalizer.normalize("palta")));
    }

    @Test
    public void ignoresUnrelatedWords() {
        assertEquals(0, aguacate().score(TextNormalizer.normalize("zapato")));
    }

    @Test
    public void normalizesPunctuationAndSpaces() {
        assertEquals("leche de almendra", TextNormalizer.normalize("  Leche   de ALMENDRA!! "));
    }

    private Food lecheDeAlmendra() {
        return new Food("leche-almendra", "Leche de almendra",
                Arrays.asList("bebida de almendra"), "Bebida", Food.LEVEL_LOW,
                Collections.emptyList(), "1 vaso", "", "🥛", "");
    }

    @Test
    public void matchesFoodInsideProductName() {
        Food food = lecheDeAlmendra();
        int score = food.matchInText(TextNormalizer.normalize("Bebida de leche de almendra Alpro sin azúcar"));
        assertTrue(score >= 700);
    }

    @Test
    public void matchesAliasInsideProductName() {
        Food food = lecheDeAlmendra();
        int score = food.matchInText(TextNormalizer.normalize("Bebida de almendra Alpro"));
        assertTrue(score >= 700);
    }

    @Test
    public void ignoresUnrelatedProductName() {
        assertEquals(0, aguacate().matchInText(TextNormalizer.normalize("Crema de avellanas Nutella")));
    }
}
