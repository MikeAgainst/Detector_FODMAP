package com.detectorfodmap.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.detectorfodmap.app.data.Food;
import com.detectorfodmap.app.data.IngredientAnalysis;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IngredientAnalysisTest {

    private static Food food(String id, String name, String level, String... aliases) {
        return new Food(id, name, Arrays.asList(aliases), "Test", level,
                Collections.emptyList(), "", "", "", "");
    }

    private static List<Food> foods() {
        return Arrays.asList(
                food("cebolla", "Cebolla", Food.LEVEL_HIGH, "cebollas"),
                food("ajo", "Ajo", Food.LEVEL_HIGH, "ajos"),
                food("trigo", "Trigo", Food.LEVEL_HIGH, "harina de trigo"),
                food("leche-entera", "Leche entera", Food.LEVEL_HIGH, "leche"),
                food("leche-almendra", "Leche de almendra", Food.LEVEL_LOW, "bebida de almendra"),
                food("almendra", "Almendra", Food.LEVEL_MODERATE, "almendras"),
                food("maiz-dulce", "Maíz dulce", Food.LEVEL_MODERATE, "maiz", "elote"),
                food("agua", "Agua", Food.LEVEL_LOW),
                food("miel", "Miel", Food.LEVEL_HIGH)
        );
    }

    private static IngredientAnalysis.Result analyze(String text) {
        return IngredientAnalysis.analyze(foods(), text);
    }

    @Test
    public void detectsHighIngredientInsideLongerLabel() {
        IngredientAnalysis.Result result = analyze("Harina de trigo, agua, sal, cebolla en polvo.");
        assertTrue(result.isAvailable());
        assertEquals(2, result.getWarnings().size());
        assertEquals("Trigo", result.getWarnings().get(0).getLabel());
        assertEquals("Cebolla", result.getWarnings().get(1).getLabel());
        assertEquals(Food.LEVEL_HIGH, result.getWarnings().get(1).getLevel());
    }

    @Test
    public void prefersMostSpecificMatch() {
        IngredientAnalysis.Result result = analyze("Agua, leche de almendra, sal");
        assertFalse(result.hasWarnings());
        assertTrue(result.isAvailable());
    }

    @Test
    public void reportsEveryHighIngredientOnlyOnce() {
        IngredientAnalysis.Result result = analyze("Cebolla, cebolla en polvo, ajo, sal");
        assertEquals(2, result.getWarnings().size());
        assertEquals("Cebolla", result.getWarnings().get(0).getLabel());
        assertEquals("Ajo", result.getWarnings().get(1).getLabel());
    }

    @Test
    public void detectsMarkersThatAreNotFoods() {
        IngredientAnalysis.Result result = analyze("Inulina, fibra de achicoria, maltitol (E965)");
        assertEquals(3, result.getWarnings().size());
        assertEquals("Inulina", result.getWarnings().get(0).getLabel());
        assertEquals("Fibra de achicoria", result.getWarnings().get(1).getLabel());
        assertEquals("Maltitol (E965)", result.getWarnings().get(2).getLabel());
    }

    @Test
    public void markerOutranksModerateFood() {
        IngredientAnalysis.Result result = analyze("Jarabe de maíz, agua");
        assertEquals(1, result.getWarnings().size());
        assertEquals("Jarabe de maíz", result.getWarnings().get(0).getLabel());
        assertEquals(Food.LEVEL_HIGH, result.getWarnings().get(0).getLevel());
    }

    @Test
    public void ignoresLactoseFreeAndLowFodmapPhrases() {
        IngredientAnalysis.Result result = analyze(
                "Leche sin lactosa, trigo sarraceno, leche de arroz, goma arábiga");
        assertFalse(result.hasWarnings());
    }

    @Test
    public void ignoresLowFodmapCornAndPeaDerivatives() {
        IngredientAnalysis.Result result = analyze(
                "Almidón de maíz, harina de maíz, aceite de maíz, proteína de guisante");
        assertFalse(result.hasWarnings());
    }

    @Test
    public void cornSyrupStillWarns() {
        IngredientAnalysis.Result result = analyze("Jarabe de maíz, almidón de maíz");
        assertEquals(1, result.getWarnings().size());
        assertEquals("Jarabe de maíz", result.getWarnings().get(0).getLabel());
    }

    @Test
    public void keepsModerateIngredientsAsWarnings() {
        IngredientAnalysis.Result result = analyze("Almendra (10%), sal");
        assertEquals(1, result.getWarnings().size());
        assertEquals(Food.LEVEL_MODERATE, result.getWarnings().get(0).getLevel());
    }

    @Test
    public void emptyLabelIsNotAvailable() {
        assertFalse(analyze(null).isAvailable());
        assertFalse(analyze("   ").isAvailable());
    }

    @Test
    public void showsOriginalLabelText() {
        String text = "Ingredientes: agua, cebolla.";
        assertEquals(text, analyze(text).getText());
    }
}
