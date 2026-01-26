package com.frontend.dto;

import java.util.List;

/**
 * DTOs for Billing API requests and responses
 */
public class BillingDto {

    /**
     * DTO for table status response
     */
    public static class TableStatusDto {
        private Integer tableId;
        private String tableName;
        private String section;
        private String status; // Available, Ongoing, Closed
        private Integer sequence;

        public TableStatusDto() {}

        public TableStatusDto(Integer tableId, String tableName, String section, String status, Integer sequence) {
            this.tableId = tableId;
            this.tableName = tableName;
            this.section = section;
            this.status = status;
            this.sequence = sequence;
        }

        public Integer getTableId() { return tableId; }
        public void setTableId(Integer tableId) { this.tableId = tableId; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getSequence() { return sequence; }
        public void setSequence(Integer sequence) { this.sequence = sequence; }
    }

    /**
     * DTO for a single item in an order
     */
    public static class OrderItemDto {
        private String itemName;
        private Float quantity;
        private Float rate;  // Optional - will be fetched from DB if not provided

        public OrderItemDto() {}

        public OrderItemDto(String itemName, Float quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }

        public OrderItemDto(String itemName, Float quantity, Float rate) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.rate = rate;
        }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public Float getQuantity() { return quantity; }
        public void setQuantity(Float quantity) { this.quantity = quantity; }
        public Float getRate() { return rate; }
        public void setRate(Float rate) { this.rate = rate; }
    }

    /**
     * DTO for adding multiple items to order (bulk request)
     * Works like desktop application:
     * - If item with same name and rate exists, quantity is ADDED to existing
     * - If item doesn't exist, new transaction is created
     * - Negative quantity reduces the existing item
     * - If KOT already printed and item is reduced, it tracks in reduced_item table
     */
    public static class AddItemsRequest {
        private Integer waitorId;
        private Integer userId;       // User ID for tracking reduced items
        private String userName;      // Username for tracking reduced items
        private List<OrderItemDto> items;

        public Integer getWaitorId() { return waitorId; }
        public void setWaitorId(Integer waitorId) { this.waitorId = waitorId; }
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public List<OrderItemDto> getItems() { return items; }
        public void setItems(List<OrderItemDto> items) { this.items = items; }
    }

    /**
     * DTO for adding single item to order (kept for backward compatibility)
     */
    public static class AddItemRequest {
        private Integer tableId;
        private String itemName;
        private Float quantity;
        private Float rate;
        private Integer waitorId;

        public Integer getTableId() { return tableId; }
        public void setTableId(Integer tableId) { this.tableId = tableId; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public Float getQuantity() { return quantity; }
        public void setQuantity(Float quantity) { this.quantity = quantity; }
        public Float getRate() { return rate; }
        public void setRate(Float rate) { this.rate = rate; }
        public Integer getWaitorId() { return waitorId; }
        public void setWaitorId(Integer waitorId) { this.waitorId = waitorId; }
    }

    /**
     * DTO for transaction item response
     */
    public static class TransactionItemDto {
        private Integer id;
        private String itemName;
        private Float quantity;
        private Float rate;
        private Float amount;
        private Integer tableNo;
        private Integer waitorId;
        private Float printQty;

        public TransactionItemDto() {}

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public Float getQuantity() { return quantity; }
        public void setQuantity(Float quantity) { this.quantity = quantity; }
        public Float getRate() { return rate; }
        public void setRate(Float rate) { this.rate = rate; }
        public Float getAmount() { return amount; }
        public void setAmount(Float amount) { this.amount = amount; }
        public Integer getTableNo() { return tableNo; }
        public void setTableNo(Integer tableNo) { this.tableNo = tableNo; }
        public Integer getWaitorId() { return waitorId; }
        public void setWaitorId(Integer waitorId) { this.waitorId = waitorId; }
        public Float getPrintQty() { return printQty; }
        public void setPrintQty(Float printQty) { this.printQty = printQty; }
    }

    /**
     * DTO for close table request
     */
    public static class CloseTableRequest {
        private Integer tableId;
        private Integer customerId;
        private Integer waitorId;
        private Integer userId;

        public Integer getTableId() { return tableId; }
        public void setTableId(Integer tableId) { this.tableId = tableId; }
        public Integer getCustomerId() { return customerId; }
        public void setCustomerId(Integer customerId) { this.customerId = customerId; }
        public Integer getWaitorId() { return waitorId; }
        public void setWaitorId(Integer waitorId) { this.waitorId = waitorId; }
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
    }

    /**
     * DTO for mark bill as paid request
     */
    public static class PayBillRequest {
        private Float cashReceived;
        private Float returnAmount;
        private Float discount;
        private String paymode; // CASH or BANK
        private Integer bankId;

        public Float getCashReceived() { return cashReceived; }
        public void setCashReceived(Float cashReceived) { this.cashReceived = cashReceived; }
        public Float getReturnAmount() { return returnAmount; }
        public void setReturnAmount(Float returnAmount) { this.returnAmount = returnAmount; }
        public Float getDiscount() { return discount; }
        public void setDiscount(Float discount) { this.discount = discount; }
        public String getPaymode() { return paymode; }
        public void setPaymode(String paymode) { this.paymode = paymode; }
        public Integer getBankId() { return bankId; }
        public void setBankId(Integer bankId) { this.bankId = bankId; }
    }

    /**
     * DTO for mark bill as credit request
     */
    public static class CreditBillRequest {
        private Integer customerId;
        private Float cashReceived;
        private Float returnAmount;
        private Float discount;

        public Integer getCustomerId() { return customerId; }
        public void setCustomerId(Integer customerId) { this.customerId = customerId; }
        public Float getCashReceived() { return cashReceived; }
        public void setCashReceived(Float cashReceived) { this.cashReceived = cashReceived; }
        public Float getReturnAmount() { return returnAmount; }
        public void setReturnAmount(Float returnAmount) { this.returnAmount = returnAmount; }
        public Float getDiscount() { return discount; }
        public void setDiscount(Float discount) { this.discount = discount; }
    }

    /**
     * DTO for bill response
     */
    public static class BillResponseDto {
        private Integer billNo;
        private String billDate;
        private Integer tableNo;
        private String tableName;
        private Integer customerId;
        private String customerName;
        private Integer waitorId;
        private String waitorName;
        private Float billAmount;
        private Float netAmount;
        private Float cashReceived;
        private Float returnAmount;
        private Float discount;
        private String status;
        private String paymode;
        private Integer bankId;
        private String bankName;
        private List<TransactionItemDto> items;
        private String pdfBase64;

        public Integer getBillNo() { return billNo; }
        public void setBillNo(Integer billNo) { this.billNo = billNo; }
        public String getBillDate() { return billDate; }
        public void setBillDate(String billDate) { this.billDate = billDate; }
        public Integer getTableNo() { return tableNo; }
        public void setTableNo(Integer tableNo) { this.tableNo = tableNo; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public Integer getCustomerId() { return customerId; }
        public void setCustomerId(Integer customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Integer getWaitorId() { return waitorId; }
        public void setWaitorId(Integer waitorId) { this.waitorId = waitorId; }
        public String getWaitorName() { return waitorName; }
        public void setWaitorName(String waitorName) { this.waitorName = waitorName; }
        public Float getBillAmount() { return billAmount; }
        public void setBillAmount(Float billAmount) { this.billAmount = billAmount; }
        public Float getNetAmount() { return netAmount; }
        public void setNetAmount(Float netAmount) { this.netAmount = netAmount; }
        public Float getCashReceived() { return cashReceived; }
        public void setCashReceived(Float cashReceived) { this.cashReceived = cashReceived; }
        public Float getReturnAmount() { return returnAmount; }
        public void setReturnAmount(Float returnAmount) { this.returnAmount = returnAmount; }
        public Float getDiscount() { return discount; }
        public void setDiscount(Float discount) { this.discount = discount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPaymode() { return paymode; }
        public void setPaymode(String paymode) { this.paymode = paymode; }
        public Integer getBankId() { return bankId; }
        public void setBankId(Integer bankId) { this.bankId = bankId; }
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        public List<TransactionItemDto> getItems() { return items; }
        public void setItems(List<TransactionItemDto> items) { this.items = items; }
        public String getPdfBase64() { return pdfBase64; }
        public void setPdfBase64(String pdfBase64) { this.pdfBase64 = pdfBase64; }
    }

    /**
     * DTO for shift table request
     */
    public static class ShiftTableRequest {
        private Integer sourceTableId;
        private Integer targetTableId;

        public Integer getSourceTableId() { return sourceTableId; }
        public void setSourceTableId(Integer sourceTableId) { this.sourceTableId = sourceTableId; }
        public Integer getTargetTableId() { return targetTableId; }
        public void setTargetTableId(Integer targetTableId) { this.targetTableId = targetTableId; }
    }

    /**
     * DTO for bill search request
     */
    public static class BillSearchRequest {
        private String date;
        private Integer billNo;
        private Integer customerId;
        private String status;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Integer getBillNo() { return billNo; }
        public void setBillNo(Integer billNo) { this.billNo = billNo; }
        public Integer getCustomerId() { return customerId; }
        public void setCustomerId(Integer customerId) { this.customerId = customerId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * DTO for bill summary response
     */
    public static class BillSummaryDto {
        private Float totalCash;
        private Float totalCredit;
        private Float totalAmount;
        private Integer billCount;

        public Float getTotalCash() { return totalCash; }
        public void setTotalCash(Float totalCash) { this.totalCash = totalCash; }
        public Float getTotalCredit() { return totalCredit; }
        public void setTotalCredit(Float totalCredit) { this.totalCredit = totalCredit; }
        public Float getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Float totalAmount) { this.totalAmount = totalAmount; }
        public Integer getBillCount() { return billCount; }
        public void setBillCount(Integer billCount) { this.billCount = billCount; }
    }

    /**
     * DTO for update bill request
     */
    public static class UpdateBillRequest {
        private List<TransactionItemDto> items;
        private Integer customerId;
        private Integer waitorId;
        private Float cashReceived;
        private Float returnAmount;
        private Float discount;
        private String paymode;
        private Integer bankId;

        public List<TransactionItemDto> getItems() { return items; }
        public void setItems(List<TransactionItemDto> items) { this.items = items; }
        public Integer getCustomerId() { return customerId; }
        public void setCustomerId(Integer customerId) { this.customerId = customerId; }
        public Integer getWaitorId() { return waitorId; }
        public void setWaitorId(Integer waitorId) { this.waitorId = waitorId; }
        public Float getCashReceived() { return cashReceived; }
        public void setCashReceived(Float cashReceived) { this.cashReceived = cashReceived; }
        public Float getReturnAmount() { return returnAmount; }
        public void setReturnAmount(Float returnAmount) { this.returnAmount = returnAmount; }
        public Float getDiscount() { return discount; }
        public void setDiscount(Float discount) { this.discount = discount; }
        public String getPaymode() { return paymode; }
        public void setPaymode(String paymode) { this.paymode = paymode; }
        public Integer getBankId() { return bankId; }
        public void setBankId(Integer bankId) { this.bankId = bankId; }
    }
}
