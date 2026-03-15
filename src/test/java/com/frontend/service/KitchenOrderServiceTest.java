package com.frontend.service;

import com.frontend.entity.KitchenOrder;
import com.frontend.entity.TempTransaction;
import com.frontend.repository.KitchenOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenOrderServiceTest {

    @Mock
    private KitchenOrderRepository kitchenOrderRepository;

    @Mock
    private ItemService itemService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private KitchenOrderService kitchenOrderService;

    @Test
    void createKitchenOrder_shouldCreateWithSentStatus() {
        // Arrange
        List<TempTransaction> items = new ArrayList<>();
        TempTransaction temp = new TempTransaction();
        temp.setItemName("Biryani");
        temp.setPrintQty(2f);
        temp.setRate(250f);
        items.add(temp);

        KitchenOrder savedKo = new KitchenOrder();
        savedKo.setId(1);
        savedKo.setTableNo(5);
        savedKo.setTableName("Table 5");
        savedKo.setStatus("SENT");

        when(itemService.getItemByName("Biryani")).thenReturn(Optional.empty());
        when(kitchenOrderRepository.save(any(KitchenOrder.class))).thenReturn(savedKo);

        // Act
        KitchenOrder result = kitchenOrderService.createKitchenOrder(5, "Table 5", 3, items);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("SENT", result.getStatus());
        verify(kitchenOrderRepository).save(any(KitchenOrder.class));
        verify(auditLogService).logAsync(eq("KitchenOrder"), eq("1"), eq("CREATE"), anyString(), anyString());
    }

    @Test
    void markAsReady_shouldTransitionFromSentToReady() {
        // Arrange
        KitchenOrder ko = new KitchenOrder();
        ko.setId(1);
        ko.setTableNo(5);
        ko.setStatus("SENT");
        ko.setItems(new ArrayList<>());

        when(kitchenOrderRepository.findById(1)).thenReturn(Optional.of(ko));
        when(kitchenOrderRepository.findByIdWithItems(1)).thenReturn(Optional.of(ko));

        // Act
        KitchenOrder result = kitchenOrderService.markAsReady(1);

        // Assert
        assertNotNull(result);
        assertEquals("READY", result.getStatus());
        assertNotNull(result.getReadyAt());
        verify(kitchenOrderRepository).save(ko);
        verify(auditLogService).logAsync(eq("KitchenOrder"), eq("1"), eq("STATUS_CHANGE"),
                contains("SENT -> READY"), anyString());
        verify(notificationService).notifyKitchenOrderUpdate(1, "READY", 5);
    }

    @Test
    void markAsReady_shouldThrowWhenNotFound() {
        // Arrange
        when(kitchenOrderRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> kitchenOrderService.markAsReady(999));
    }

    @Test
    void markAsServed_shouldTransitionFromReadyToServe() {
        // Arrange
        KitchenOrder ko = new KitchenOrder();
        ko.setId(1);
        ko.setTableNo(5);
        ko.setStatus("READY");
        ko.setItems(new ArrayList<>());

        when(kitchenOrderRepository.findById(1)).thenReturn(Optional.of(ko));
        when(kitchenOrderRepository.findByIdWithItems(1)).thenReturn(Optional.of(ko));

        // Act
        KitchenOrder result = kitchenOrderService.markAsServed(1);

        // Assert
        assertNotNull(result);
        assertEquals("SERVE", result.getStatus());
        verify(kitchenOrderRepository).save(ko);
        verify(notificationService).notifyKitchenOrderUpdate(1, "SERVE", 5);
    }

    @Test
    void markAllAsReadyForTable_shouldUpdateAllSentOrders() {
        // Arrange
        List<KitchenOrder> sentOrders = new ArrayList<>();
        KitchenOrder ko1 = new KitchenOrder();
        ko1.setId(1);
        ko1.setStatus("SENT");
        sentOrders.add(ko1);

        KitchenOrder ko2 = new KitchenOrder();
        ko2.setId(2);
        ko2.setStatus("SENT");
        sentOrders.add(ko2);

        when(kitchenOrderRepository.findByTableNoAndStatusOrderBySentAtAsc(5, "SENT"))
                .thenReturn(sentOrders);

        // Act
        kitchenOrderService.markAllAsReadyForTable(5);

        // Assert
        assertEquals("READY", ko1.getStatus());
        assertEquals("READY", ko2.getStatus());
        assertNotNull(ko1.getReadyAt());
        assertNotNull(ko2.getReadyAt());
        verify(kitchenOrderRepository).saveAll(sentOrders);
    }

    @Test
    void markAllAsServedForTable_shouldUpdateAllReadyOrders() {
        // Arrange
        List<KitchenOrder> readyOrders = new ArrayList<>();
        KitchenOrder ko = new KitchenOrder();
        ko.setId(1);
        ko.setStatus("READY");
        readyOrders.add(ko);

        when(kitchenOrderRepository.findByTableNoAndStatusOrderBySentAtAsc(5, "READY"))
                .thenReturn(readyOrders);

        // Act
        kitchenOrderService.markAllAsServedForTable(5);

        // Assert
        assertEquals("SERVE", ko.getStatus());
        verify(kitchenOrderRepository).saveAll(readyOrders);
    }

    @Test
    void clearKitchenOrdersForTable_shouldDeleteAllOrders() {
        // Arrange
        List<KitchenOrder> orders = new ArrayList<>();
        orders.add(new KitchenOrder());

        when(kitchenOrderRepository.findByTableNoOrderBySentAtAsc(5)).thenReturn(orders);

        // Act
        kitchenOrderService.clearKitchenOrdersForTable(5);

        // Assert
        verify(kitchenOrderRepository).deleteAll(orders);
    }

    @Test
    void clearKitchenOrdersForTable_shouldDoNothingWhenNoOrders() {
        // Arrange
        when(kitchenOrderRepository.findByTableNoOrderBySentAtAsc(5)).thenReturn(new ArrayList<>());

        // Act
        kitchenOrderService.clearKitchenOrdersForTable(5);

        // Assert
        verify(kitchenOrderRepository, never()).deleteAll(anyList());
    }
}
