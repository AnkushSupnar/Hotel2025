package com.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Item entity
 */
public class ItemDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("itemName")
    private String itemName;

    @JsonProperty("categoryId")
    private Integer categoryId;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("rate")
    private Float rate;

    @JsonProperty("itemCode")
    private Integer itemCode;

    // Constructors
    public ItemDto() {
    }

    public ItemDto(Integer id, String itemName, Integer categoryId, Float rate, Integer itemCode) {
        this.id = id;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.rate = rate;
        this.itemCode = itemCode;
    }

    public ItemDto(Integer id, String itemName, Integer categoryId, String categoryName, Float rate, Integer itemCode) {
        this.id = id;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.rate = rate;
        this.itemCode = itemCode;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Float getRate() {
        return rate;
    }

    public void setRate(Float rate) {
        this.rate = rate;
    }

    public Integer getItemCode() {
        return itemCode;
    }

    public void setItemCode(Integer itemCode) {
        this.itemCode = itemCode;
    }

    @Override
    public String toString() {
        return "ItemDto{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", rate=" + rate +
                ", itemCode=" + itemCode +
                '}';
    }
}
