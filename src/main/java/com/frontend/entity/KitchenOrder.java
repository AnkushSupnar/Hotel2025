package com.frontend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Kitchen Order Ticket (KOT) record.
 * Created when items are sent to kitchen, tracks completion status.
 */
@Entity
@Table(name = "kitchen_order")
public class KitchenOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "table_no", nullable = false)
    private Integer tableNo;

    @Column(name = "table_name", length = 50)
    private String tableName;

    @Column(name = "waitor_id")
    private Integer waitorId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "total_qty")
    private Float totalQty;

    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @OneToMany(mappedBy = "kitchenOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KitchenOrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    // Constructors
    public KitchenOrder() {
    }

    /**
     * Add an item and set the bidirectional link.
     */
    public void addItem(KitchenOrderItem item) {
        items.add(item);
        item.setKitchenOrder(this);
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTableNo() {
        return tableNo;
    }

    public void setTableNo(Integer tableNo) {
        this.tableNo = tableNo;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getWaitorId() {
        return waitorId;
    }

    public void setWaitorId(Integer waitorId) {
        this.waitorId = waitorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Float getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Float totalQty) {
        this.totalQty = totalQty;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(LocalDateTime readyAt) {
        this.readyAt = readyAt;
    }

    public List<KitchenOrderItem> getItems() {
        return items;
    }

    public void setItems(List<KitchenOrderItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "KitchenOrder{" +
                "id=" + id +
                ", tableNo=" + tableNo +
                ", tableName='" + tableName + '\'' +
                ", status='" + status + '\'' +
                ", itemCount=" + itemCount +
                ", sentAt=" + sentAt +
                '}';
    }
}
