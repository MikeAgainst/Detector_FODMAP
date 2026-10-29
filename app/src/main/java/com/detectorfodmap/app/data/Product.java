package com.detectorfodmap.app.data;

/** Producto tal y como lo devuelve Open Food Facts. */
public final class Product {

    private final String barcode;
    private final String name;
    private final String brand;
    private final String quantity;
    private final String imageUrl;
    private final String ingredientsText;

    public Product(String barcode, String name, String brand, String quantity, String imageUrl,
                   String ingredientsText) {
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.ingredientsText = ingredientsText == null ? "" : ingredientsText;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    /** Lista de ingredientes de la etiqueta, vacía si Open Food Facts no la tiene. */
    public String getIngredientsText() {
        return ingredientsText;
    }
}
