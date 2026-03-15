package com.frontend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending real-time notifications via WebSocket.
 * Broadcasts updates to subscribed clients (mobile app, kitchen display, dashboard).
 */
@Service
@Profile("server")
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Notify clients that a table's status has changed.
     * Clients subscribe to: /topic/tables/{tableId}
     */
    public void notifyTableStatusChange(Integer tableId, String status) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tableId", tableId);
            payload.put("status", status);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/tables/" + tableId, payload);
            LOG.debug("Notified table {} status change: {}", tableId, status);
        } catch (Exception e) {
            LOG.warn("Failed to send table status notification: {}", e.getMessage());
        }
    }

    /**
     * Notify clients of kitchen order updates.
     * Clients subscribe to: /topic/kitchen-orders
     */
    public void notifyKitchenOrderUpdate(Integer kotId, String status, Integer tableNo) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("kotId", kotId);
            payload.put("status", status);
            payload.put("tableNo", tableNo);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/kitchen-orders", payload);
            LOG.debug("Notified kitchen order update: KOT #{} -> {}", kotId, status);
        } catch (Exception e) {
            LOG.warn("Failed to send kitchen order notification: {}", e.getMessage());
        }
    }

    /**
     * Notify dashboard clients of general updates (new bill, payment, etc.).
     * Clients subscribe to: /topic/dashboard
     */
    public void notifyDashboardUpdate(String event, Map<String, Object> data) {
        try {
            Map<String, Object> payload = new HashMap<>(data);
            payload.put("event", event);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/dashboard", payload);
            LOG.debug("Notified dashboard: {}", event);
        } catch (Exception e) {
            LOG.warn("Failed to send dashboard notification: {}", e.getMessage());
        }
    }
}
