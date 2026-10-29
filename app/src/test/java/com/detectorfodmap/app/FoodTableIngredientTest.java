package com.detectorfodmap.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.detectorfodmap.app.data.Food;
import com.detectorfodmap.app.data.IngredientAnalysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Prueba el análisis con la tabla real que se empaqueta en la app, no con una
 * lista inventada. Así se detecta si un cambio en foods.json rompe las etiquetas
 * habituales.
 */
public class FoodTableIngredientTest {

    private static List<Food> foods;

    @BeforeClass
    public static void loadTable() throws Exception {
        File file = new File("src/main/assets/foods.json");
        assertTrue("No se encuentra " + file.getAbsolutePath(), file.exists());

        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        JSONArray array = new JSONObject(json).getJSONArray("foods");
        List<Food> loaded = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            loaded.add(new Food(
                    object.getString("id"),
                    object.getString("name"),
                    readList(object.optJSONArray("aliases")),
                    object.optString("category", "Otro"),
                    object.optString("level", Food.LEVEL_LOW),
                    readList(object.optJSONArray("fodmaps")),
                    object.optString("portion", ""),
                    object.optString("note", ""),
                    object.optString("emoji", ""),
                    object.optString("wiki", "")));
        }
        foods = loaded;
    }

    private static List<String> readList(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                values.add(array.optString(i));
            }
        }
        return values;
    }

    private static IngredientAnalysis.Result analyze(String text) {
        return IngredientAnalysis.analyze(foods, text);
    }

    @Test
    public void loadsTheRealTable() {
        assertTrue("La tabla debería tener los 158 alimentos", foods.size() >= 150);
        assertTrue(foods.stream().anyMatch(food -> Food.LEVEL_HIGH.equals(food.getLevel())));
    }

    @Test
    public void nutellaLabelWarnsAboutMilkPowder() {
        IngredientAnalysis.Result result = analyze(
                "AZÚCAR, ACEITE VEGETAL, AVELLANAS (13%), LECHE DESCREMADA EN POLVO (8,7%), "
                        + "CACAO MAGRO EN POLVO7,4%), EMULSIONANTE: LECITINA, "
                        + "SABORIZANTE IDENTICO AL NATURAL: VAINILLINA");
        assertEquals(1, result.getWarnings().size());
        assertEquals(Food.LEVEL_HIGH, result.getWarnings().get(0).getLevel());
        assertTrue(result.getWarnings().get(0).getLabel().toLowerCase().contains("leche"));
    }

    @Test
    public void onionAndGarlicPowderAreHigh() {
        IngredientAnalysis.Result result = analyze(
                "Agua, sal, cebolla en polvo, ajo en polvo, aceite de oliva");
        assertEquals(2, result.getWarnings().size());
        assertEquals(Food.LEVEL_HIGH, result.getWarnings().get(0).getLevel());
        assertEquals(Food.LEVEL_HIGH, result.getWarnings().get(1).getLevel());
    }

    @Test
    public void riceDrinkAndBuckwheatAreNotWheatOrMilk() {
        IngredientAnalysis.Result result = analyze(
                "Bebida de arroz, trigo sarraceno, cacao, aceite de girasol");
        assertFalse(result.hasWarnings());
        assertTrue(result.isAvailable());
    }

    @Test
    public void spanishFreeLabelIsNotAnAlert() {
        IngredientAnalysis.Result result = analyze(
                "Leche sin lactosa, fermentos lácteos, sal");
        assertFalse(result.hasWarnings());
    }

    @Test
    public void emptyOrMissingLabelKeepsOnlyNameMatchOption() {
        assertFalse(analyze("").isAvailable());
        assertFalse(analyze(null).isAvailable());
    }

    @Test
    public void duplicatedIngredientsProduceOneRow() {
        IngredientAnalysis.Result result = analyze("Cebolla, cebolla, CEBOLLA (5%)");
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    public void labelTextIsReturnedAsIs() {
        String label = "Ingredientes: agua, cebolla (10%), sal.";
        assertEquals(label, analyze(label).getText());
    }

    @Test
    public void emptyListHasNoHits() {
        assertEquals(Collections.emptyList(), IngredientAnalysis.analyze(
                Collections.<Food>emptyList(), "cebolla, ajo").getHits());
    }
}
