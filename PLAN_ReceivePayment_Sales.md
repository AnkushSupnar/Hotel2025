# Plan: Receive Payment Screen for Sales (Credit Bill Payments)

## Current System Understanding

### Bill Entity (Sales)
```
Bill
├── billNo (PK)
├── customerId (FK to Customer)
├── billAmt (total amount)
├── discount
├── netAmount (billAmt - discount)
├── cashReceived (amount paid at time of billing)
├── returnAmount (change given)
├── paymode (CASH, CREDIT, etc.)
├── status (PAID, CREDIT, CLOSE)
├── billDate, billTime
├── transactions (items sold)
```

### Credit Bill Flow (Current)
1. Sales made → Bill created with status="CREDIT", paymode="CREDIT"
2. Customer ID is required for credit bills
3. `cashReceived` = 0 (or partial if some paid upfront)
4. Credit balance = `netAmount - (cashReceived - returnAmount)`

### Problem
- No way to track **subsequent payments** on credit bills
- No "Receive Payment" screen exists
- Bill entity lacks `paidAmount` field to track cumulative payments

---

## Proposed Solution: Mirror the Purchase Payment System

### Comparison with Purchase System

| Purchase System | Sales System (To Create) |
|-----------------|-------------------------|
| `PurchaseBill` | `Bill` (modify) |
| `BillPayment` | `SalesBillPayment` (new) |
| `PaymentReceipt` | `SalesPaymentReceipt` (new) |
| `PayReceiptController` | `ReceivePaymentController` (new) |
| Supplier | Customer |
| "Pay Receipt" | "Receive Payment" |

---

## Phase 1: Database & Entity Changes

### 1.1 Modify Bill Entity - Add Payment Tracking Fields
```java
// Add to Bill.java
@Column(name = "paid_amount")
private Float paidAmount = 0.0f;

// Helper method
public Float getBalanceAmount() {
    Float net = netAmount != null ? netAmount : 0f;
    Float paid = paidAmount != null ? paidAmount : 0f;
    return net - paid;
}
```

### 1.2 Create SalesPaymentReceipt Entity (Master)
```java
@Entity
@Table(name = "sales_payment_receipt")
public class SalesPaymentReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_no")
    private Integer receiptNo;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "bank_id", nullable = false)
    private Integer bankId;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "cheque_no")
    private String chequeNo;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "bank_transaction_id")
    private Integer bankTransactionId;

    @Column(name = "bills_count")
    private Integer billsCount = 1;

    @OneToMany(mappedBy = "salesPaymentReceipt", cascade = CascadeType.ALL)
    private List<SalesBillPayment> billPayments = new ArrayList<>();

    // timestamps, getters, setters...
}
```

### 1.3 Create SalesBillPayment Entity (Detail)
```java
@Entity
@Table(name = "sales_bill_payment")
public class SalesBillPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "receipt_no")
    private Integer receiptNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_no", insertable = false, updatable = false)
    private SalesPaymentReceipt salesPaymentReceipt;

    @Column(name = "bill_no", nullable = false)
    private Integer billNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_no", insertable = false, updatable = false)
    private Bill bill;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_amount", nullable = false)
    private Double paymentAmount;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "bank_id")
    private Integer bankId;

    @Column(name = "payment_mode")
    private String paymentMode;

    // timestamps, getters, setters...
}
```

---

## Phase 2: Repository Layer

### 2.1 Create SalesPaymentReceiptRepository
```java
public interface SalesPaymentReceiptRepository extends JpaRepository<SalesPaymentReceipt, Integer> {
    List<SalesPaymentReceipt> findByCustomerIdOrderByPaymentDateDescReceiptNoDesc(Integer customerId);
    List<SalesPaymentReceipt> findByPaymentDateBetweenOrderByPaymentDateDescReceiptNoDesc(LocalDate start, LocalDate end);

    @Query("SELECT spr FROM SalesPaymentReceipt spr LEFT JOIN FETCH spr.billPayments WHERE spr.receiptNo = :receiptNo")
    Optional<SalesPaymentReceipt> findByIdWithBillPayments(@Param("receiptNo") Integer receiptNo);

    @Query("SELECT COALESCE(SUM(spr.totalAmount), 0) FROM SalesPaymentReceipt spr WHERE spr.paymentDate = :date")
    Double getTotalReceiptsByDate(@Param("date") LocalDate date);
}
```

### 2.2 Create SalesBillPaymentRepository
```java
public interface SalesBillPaymentRepository extends JpaRepository<SalesBillPayment, Integer> {
    List<SalesBillPayment> findByBillNoOrderByPaymentDateDesc(Integer billNo);
    List<SalesBillPayment> findByCustomerIdOrderByPaymentDateDesc(Integer customerId);
    List<SalesBillPayment> findByReceiptNoOrderByBillNoAsc(Integer receiptNo);

    @Query("SELECT COALESCE(SUM(sbp.paymentAmount), 0) FROM SalesBillPayment sbp WHERE sbp.billNo = :billNo")
    Double getTotalPaymentsByBillNo(@Param("billNo") Integer billNo);
}
```

### 2.3 Modify BillRepository - Add Credit Bill Queries
```java
// Add to BillRepository.java

/**
 * Find credit bills with pending balance for a customer
 */
@Query("SELECT b FROM Bill b WHERE b.customerId = :customerId AND b.status = 'CREDIT' " +
       "AND (b.netAmount - COALESCE(b.paidAmount, 0)) > 0 ORDER BY b.billNo ASC")
List<Bill> findCreditBillsWithPendingBalanceByCustomerId(@Param("customerId") Integer customerId);

/**
 * Get total pending amount for a customer (credit bills)
 */
@Query("SELECT COALESCE(SUM(b.netAmount - COALESCE(b.paidAmount, 0)), 0) FROM Bill b " +
       "WHERE b.customerId = :customerId AND b.status = 'CREDIT'")
Double getTotalPendingAmountByCustomerId(@Param("customerId") Integer customerId);

/**
 * Get all customers with pending credit bills
 */
@Query("SELECT DISTINCT b.customerId FROM Bill b WHERE b.status = 'CREDIT' " +
       "AND (b.netAmount - COALESCE(b.paidAmount, 0)) > 0")
List<Integer> findCustomerIdsWithPendingBills();
```

---

## Phase 3: Service Layer

### 3.1 Create SalesPaymentReceiptService
```java
@Service
public class SalesPaymentReceiptService {

    public static class BillPaymentAllocation {
        private final Integer billNo;
        private final Double amount;
        // constructor, getters...
    }

    /**
     * Record a grouped payment from customer for one or more credit bills
     * Creates ONE SalesPaymentReceipt + ONE BankTransaction (deposit)
     * Allocates payment across bills in SalesBillPayment records
     */
    @Transactional
    public SalesPaymentReceipt recordGroupedPayment(
            Integer customerId,
            Double totalAmount,
            Integer bankId,
            String paymentMode,
            String chequeNo,
            String referenceNo,
            String remarks,
            List<BillPaymentAllocation> allocations) {

        // 1. Create single bank transaction (DEPOSIT for receiving payment)
        BankTransaction bankTxn = bankTransactionService.recordDeposit(
            bankId, totalAmount,
            "Customer Payment Receipt - " + allocations.size() + " bills",
            "CUSTOMER_PAYMENT", null, remarks
        );

        // 2. Create SalesPaymentReceipt (master record)
        SalesPaymentReceipt receipt = new SalesPaymentReceipt();
        receipt.setCustomerId(customerId);
        receipt.setPaymentDate(LocalDate.now());
        receipt.setTotalAmount(totalAmount);
        receipt.setBankId(bankId);
        receipt.setPaymentMode(paymentMode);
        receipt.setChequeNo(chequeNo);
        receipt.setReferenceNo(referenceNo);
        receipt.setRemarks(remarks);
        receipt.setBankTransactionId(bankTxn.getId());
        receipt.setBillsCount(allocations.size());

        SalesPaymentReceipt savedReceipt = salesPaymentReceiptRepository.save(receipt);

        // 3. Create SalesBillPayment allocation records and update bills
        for (BillPaymentAllocation alloc : allocations) {
            SalesBillPayment payment = new SalesBillPayment();
            payment.setReceiptNo(savedReceipt.getReceiptNo());
            payment.setBillNo(alloc.getBillNo());
            payment.setPaymentAmount(alloc.getAmount());
            payment.setPaymentDate(LocalDate.now());
            payment.setBankId(bankId);
            payment.setPaymentMode(paymentMode);
            payment.setCustomerId(customerId);

            salesBillPaymentRepository.save(payment);

            // 4. Update Bill paid amount and status
            updateBillPaidStatus(alloc.getBillNo(), alloc.getAmount());
        }

        return savedReceipt;
    }

    private void updateBillPaidStatus(Integer billNo, Double paymentAmount) {
        Bill bill = billRepository.findById(billNo).orElseThrow();
        Float currentPaid = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0f;
        Float newPaid = currentPaid + paymentAmount.floatValue();
        bill.setPaidAmount(newPaid);

        // Update status
        Float netAmount = bill.getNetAmount() != null ? bill.getNetAmount() : 0f;
        if (newPaid >= netAmount - 0.01f) {
            bill.setStatus("PAID");  // Fully paid
        }
        // Keep as CREDIT if partially paid

        billRepository.save(bill);
    }
}
```

---

## Phase 4: UI - Controller & FXML

### 4.1 Create ReceivePaymentFrame.fxml
Layout similar to PayReceiptFrame:
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  RECEIVE PAYMENT FROM CUSTOMER                                              │
├─────────────────────────────────┬───────────────────────────────────────────┤
│  LEFT PANEL: Payment Entry      │  RIGHT PANEL: Payment History             │
│  ┌───────────────────────────┐  │  ┌───────────────────────────────────────┐│
│  │ Customer: [AutoComplete▼] │  │  │ Date Range: [From] [To] [Search]     ││
│  │ Contact: 9876543210       │  │  │                                       ││
│  │ Total Pending: Rs. 5,000  │  │  │ ┌─────────────────────────────────┐  ││
│  └───────────────────────────┘  │  │ │Rcpt#│Date│Customer│Bills│Amount │  ││
│                                 │  │ │ 1  │01-01│ABC    │ 3  │4,500  │  ││
│  ┌───────────────────────────┐  │  │ │ 2  │02-01│XYZ    │ 1  │2,000  │  ││
│  │ PENDING BILLS             │  │  │ └─────────────────────────────────┘  ││
│  │ ┌─────────────────────┐   │  │  │                                       ││
│  │ │☑ #101│01-01│Rs.1000 │   │  │  │ Today: Rs. 6,500                     ││
│  │ │☑ #102│02-01│Rs.2000 │   │  │  │ This Month: Rs. 45,000               ││
│  │ │☐ #103│03-01│Rs.2000 │   │  │  └───────────────────────────────────────┘│
│  │ └─────────────────────┘   │  │                                           │
│  │ Selected: 2 bills         │  │                                           │
│  │ Total: Rs. 3,000          │  │                                           │
│  └───────────────────────────┘  │                                           │
│                                 │                                           │
│  ┌───────────────────────────┐  │                                           │
│  │ PAYMENT DETAILS           │  │                                           │
│  │ Amount: [3000         ]   │  │                                           │
│  │ Mode: [Cash Bank    ▼]    │  │                                           │
│  │ Cheque No: [          ]   │  │                                           │
│  │ Reference: [          ]   │  │                                           │
│  │ Remarks: [            ]   │  │                                           │
│  │                           │  │                                           │
│  │ [  RECEIVE PAYMENT  ]     │  │                                           │
│  └───────────────────────────┘  │                                           │
└─────────────────────────────────┴───────────────────────────────────────────┘
```

### 4.2 Create ReceivePaymentController.java
Key features:
- Customer AutoComplete selection
- Load pending credit bills for selected customer
- Multi-select bills with checkboxes
- Auto-calculate total from selected bills
- Payment mode selection (Cash/Bank/UPI)
- Process payment → create SalesPaymentReceipt
- Show payment history (grouped by receipt)
- Print receipt option

---

## Phase 5: Print Receipt

### 5.1 Create ReceivePaymentPrint.java
Similar to PayReceiptPrint but for customer payments:
- Shows "Payment Received" instead of "Payment Made"
- Customer info instead of Supplier info
- Bill allocations table
- Bank deposit reference

---

## Files to Create

| File | Type | Description |
|------|------|-------------|
| `SalesPaymentReceipt.java` | Entity | Master receipt for grouped payments |
| `SalesBillPayment.java` | Entity | Payment allocation to individual bills |
| `SalesPaymentReceiptRepository.java` | Repository | Queries for receipts |
| `SalesBillPaymentRepository.java` | Repository | Queries for bill payments |
| `SalesPaymentReceiptService.java` | Service | Business logic |
| `ReceivePaymentController.java` | Controller | UI logic |
| `ReceivePaymentFrame.fxml` | FXML | UI layout |
| `ReceivePaymentPrint.java` | Print | Receipt PDF generation |

## Files to Modify

| File | Changes |
|------|---------|
| `Bill.java` | Add `paidAmount` field + getter/setter + `getBalanceAmount()` |
| `BillRepository.java` | Add credit bill queries |
| `BillService.java` | Add methods for credit bill management |
| `BankTransactionService.java` | Add `recordDeposit()` method if not exists |

---

## Implementation Order

1. **Phase 1**: Entity changes (Bill, SalesPaymentReceipt, SalesBillPayment)
2. **Phase 2**: Repositories (SalesPaymentReceiptRepository, SalesBillPaymentRepository, BillRepository updates)
3. **Phase 3**: Services (SalesPaymentReceiptService)
4. **Phase 4**: UI (ReceivePaymentFrame.fxml, ReceivePaymentController.java)
5. **Phase 5**: Print (ReceivePaymentPrint.java)
6. **Phase 6**: Menu integration + Testing

---

## Key Differences from Purchase Payment

| Aspect | Purchase Payment | Sales Payment |
|--------|------------------|---------------|
| Direction | Money OUT (pay supplier) | Money IN (receive from customer) |
| Bank Transaction | Withdrawal | **Deposit** |
| Entity | Supplier | Customer |
| Bill Entity | PurchaseBill | Bill |
| Screen Name | "Pay Receipt" | "Receive Payment" |
| Receipt Title | paOsao BarlyacaI paavataI | paOsao imaLalyacaI paavataI |

---

## Questions for Clarification

1. **Bank Transaction**: Should receiving payment create a DEPOSIT in bank transaction?
   - Recommended: Yes, money is coming IN

2. **Partial Credit at Billing**: Currently `cashReceived` can have partial amount. Should we:
   - a) Initialize `paidAmount` = `cashReceived` for existing credit bills?
   - b) Keep `paidAmount` separate from `cashReceived`?
   - Recommended: Option (a) for consistency

3. **Status Update**: When credit bill is fully paid:
   - Change status from "CREDIT" to "PAID"?
   - Recommended: Yes

Please review and confirm to proceed with implementation.
