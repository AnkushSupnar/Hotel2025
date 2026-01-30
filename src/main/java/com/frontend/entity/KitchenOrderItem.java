package com.frontend.entity;

import jakarta.persistence.*;

/**
 * Entity representing a single item within a Kitchen Order Ticket (KOT).
 * Stores both item_name (always set, used for display) and item_id (nullable FK).
 */
@Entity
@Table(name = "kitchen_order_item")
public class KitchenOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_order_id", nullable = false)
    private KitchenOrder kitchenOrder;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "qty", nullable = false)
    private Float qty;

    @Column(name = "rate")
    private Float rate;

    // Constructors
    public KitchenOrderItem() {
    }

    public KitchenOrderItem(String itemName, Integer itemId, Float qty, Float rate) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.qty = qty;
        this.rate = rate;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public KitchenOrder getKitchenOrder() {
        return kitchenOrder;
    }

    public void setKitchenOrder(KitchenOrder kitchenOrder) {
        this.kitchenOrder = kitchenOrder;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Float getQty() {
        return qty;
    }

    public void setQty(Float qty) {
        this.qty = qty;
    }

    public Float getRate() {
        return rate;
    }

    public void setRate(Float rate) {
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "KitchenOrderItem{" +
                "id=" + id +
                ", itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", qty=" + qty +
                ", rate=" + rate +
                '}';
    }
}
