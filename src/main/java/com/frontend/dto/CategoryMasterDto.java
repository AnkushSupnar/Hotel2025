package com.frontend.dto;

public class CategoryMasterDto {
    private Integer id;
    private String category;
    private String stock;
    private String purchase;

    // Constructors
    public CategoryMasterDto() {
    }

    public CategoryMasterDto(String category, String stock) {
        this.category = category;
        this.stock = stock;
    }

    public CategoryMasterDto(String category, String stock, String purchase) {
        this.category = category;
        this.stock = stock;
        this.purchase = purchase;
    }

    public CategoryMasterDto(Integer id, String category, String stock) {
        this.id = id;
        this.category = category;
        this.stock = stock;
    }

    public CategoryMasterDto(Integer id, String category, String stock, String purchase) {
        this.id = id;
        this.category = category;
        this.stock = stock;
        this.purchase = purchase;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getPurchase() {
        return purchase;
    }

    public void setPurchase(String purchase) {
        this.purchase = purchase;
    }

    @Override
    public String toString() {
        return "CategoryMasterDto{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", stock='" + stock + '\'' +
                ", purchase='" + purchase + '\'' +
                '}';
    }
}