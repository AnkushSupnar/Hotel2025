package com.frontend.service;

import com.frontend.entity.Bill;
import com.frontend.entity.Item;
import com.frontend.entity.TempTransaction;
import com.frontend.repository.BillRepository;
import com.frontend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TempTransactionService tempTransactionService;

    @Mock
    private ItemService itemService;

    @Mock
    private ItemStockService itemStockService;

    @Mock
    private KitchenOrderService kitchenOrderService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BillService billService;

    private List<TempTransaction> testTempTransactions;

    @BeforeEach
    void setUp() {
        testTempTransactions = new ArrayList<>();

        TempTransaction temp1 = new TempTransaction();
        temp1.setItemName("Chicken Biryani");
        temp1.setQty(2f);
        temp1.setRate(250f);
        temp1.setAmt(500f);
        temp1.setTableNo(1);
        testTempTransactions.add(temp1);

        TempTransaction temp2 = new TempTransaction();
        temp2.setItemName("Cold Drink");
        temp2.setQty(3f);
        temp2.setRate(50f);
        temp2.setAmt(150f);
        temp2.setTableNo(1);
        testTempTransactions.add(temp2);
    }

    @Test
    void createClosedBill_shouldCreateBillWithCloseStatus() {
        // Arrange
        Bill savedBill = new Bill();
        savedBill.setBillNo(1);
        savedBill.setBillAmt(650f);
        savedBill.setStatus("CLOSE");
        savedBill.setTableNo(1);
        savedBill.setTransactions(new ArrayList<>());

        when(billRepository.save(any(Bill.class))).thenReturn(savedBill);
        when(itemService.getItemByName(anyString())).thenReturn(Optional.empty());

        // Act
        Bill result = billService.createClosedBill(1, null, 3, 1, testTempTransactions);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBillNo());
        assertEquals("CLOSE", result.getStatus());
        verify(billRepository, times(2)).save(any(Bill.class));
        verify(tempTransactionService).clearTransactionsForTable(1);
        verify(auditLogService).logAsync(eq("Bill"), anyString(), eq("CREATE"), anyString(), anyString());
    }

    @Test
    void markBillAsPaid_shouldUpdateStatusToPaid() {
        // Arrange
        Bill existingBill = new Bill();
        existingBill.setBillNo(1);
        existingBill.setBillAmt(650f);
        existingBill.setStatus("CLOSE");
        existingBill.setTransactions(new ArrayList<>());

        when(billRepository.findById(1)).thenReturn(Optional.of(existingBill));
        when(billRepository.save(any(Bill.class))).thenReturn(existingBill);

        // Act
        Bill result = billService.markBillAsPaid(1, 700f, 50f, 0f, "CASH", 1);

        // Assert
        assertNotNull(result);
        assertEquals("PAID", result.getStatus());
        assertEquals("CASH", result.getPaymode());
        assertEquals(700f, result.getCashReceived());
        assertEquals(50f, result.getReturnAmount());
        verify(auditLogService).logAsync(eq("Bill"), eq("1"), eq("PAID"), anyString(), anyString());
    }

    @Test
    void markBillAsPaid_shouldThrowWhenBillNotFound() {
        // Arrange
        when(billRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                billService.markBillAsPaid(999, 100f, 0f, 0f, "CASH", 1));
    }

    @Test
    void markBillAsCredit_shouldUpdateStatusToCredit() {
        // Arrange
        Bill existingBill = new Bill();
        existingBill.setBillNo(1);
        existingBill.setBillAmt(650f);
        existingBill.setStatus("CLOSE");
        existingBill.setTransactions(new ArrayList<>());

        when(billRepository.findById(1)).thenReturn(Optional.of(existingBill));
        when(billRepository.save(any(Bill.class))).thenReturn(existingBill);

        // Act
        Bill result = billService.markBillAsCredit(1, 5, 0f, 0f, 0f);

        // Assert
        assertNotNull(result);
        assertEquals("CREDIT", result.getStatus());
        assertEquals(5, result.getCustomerId());
        verify(auditLogService).logAsync(eq("Bill"), eq("1"), eq("CREDIT"), anyString(), anyString());
    }

    @Test
    void markBillAsCredit_shouldThrowWhenNoCustomer() {
        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                billService.markBillAsCredit(1, null, 0f, 0f, 0f));
    }

    @Test
    void deleteBill_shouldDeleteAndAuditLog() {
        // Act
        billService.deleteBill(1);

        // Assert
        verify(billRepository).deleteById(1);
        verify(auditLogService).logAsync(eq("Bill"), eq("1"), eq("DELETE"), anyString(), anyString());
    }

    @Test
    void getBillByBillNo_shouldReturnBillWhenExists() {
        // Arrange
        Bill bill = new Bill();
        bill.setBillNo(1);
        when(billRepository.findById(1)).thenReturn(Optional.of(bill));

        // Act
        Bill result = billService.getBillByBillNo(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBillNo());
    }

    @Test
    void getBillByBillNo_shouldReturnNullWhenNotExists() {
        // Arrange
        when(billRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        Bill result = billService.getBillByBillNo(999);

        // Assert
        assertNull(result);
    }

    @Test
    void hasClosedBill_shouldReturnTrueWhenClosedBillExists() {
        // Arrange
        Bill closedBill = new Bill();
        closedBill.setStatus("CLOSE");
        when(billRepository.findFirstByTableNoAndStatusOrderByBillNoDesc(1, "CLOSE"))
                .thenReturn(Optional.of(closedBill));

        // Act
        boolean result = billService.hasClosedBill(1);

        // Assert
        assertTrue(result);
    }

    @Test
    void hasClosedBill_shouldReturnFalseWhenNoClosedBill() {
        // Arrange
        when(billRepository.findFirstByTableNoAndStatusOrderByBillNoDesc(1, "CLOSE"))
                .thenReturn(Optional.empty());

        // Act
        boolean result = billService.hasClosedBill(1);

        // Assert
        assertFalse(result);
    }
}
