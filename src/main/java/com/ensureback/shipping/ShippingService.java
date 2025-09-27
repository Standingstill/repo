package com.ensureback.shipping;

import com.ensureback.config.EnsurebackProperties;
import com.ensureback.merchant.Merchant;
import com.ensureback.order.Order;
import com.ensureback.order.OrderRepository;
import com.ensureback.timer.Timer;
import com.ensureback.timer.TimerRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Transactional
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final TimerRepository timerRepository;
    private final WebClient afterShipClient;
    private final EnsurebackProperties ensurebackProperties;

    public ShippingService(ShipmentRepository shipmentRepository,
                           OrderRepository orderRepository,
                           TimerRepository timerRepository,
                           WebClient afterShipClient,
                           EnsurebackProperties ensurebackProperties) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.timerRepository = timerRepository;
        this.afterShipClient = afterShipClient;
        this.ensurebackProperties = ensurebackProperties;
    }

    public Shipment registerTracking(UUID orderId, String carrierCode, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseGet(() -> {
                    Shipment s = new Shipment();
                    s.setId(UUID.randomUUID());
                    s.setOrder(order);
                    s.setCreatedAt(OffsetDateTime.now());
                    return s;
                });

        shipment.setCarrierCode(carrierCode.toLowerCase(Locale.ROOT));
        shipment.setTrackingNumber(trackingNumber);
        shipment.setStatus(Shipment.Status.CREATED);

        callAfterShipRegister(carrierCode, trackingNumber, order.getBuyerEmail());

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Registered tracking {} for order {}", trackingNumber, orderId);
        return saved;
    }

    public void handleDeliveredEvent(String trackingNumber, OffsetDateTime deliveredAt, String rawPayload) {
        shipmentRepository.findByTrackingNumber(trackingNumber)
                .ifPresentOrElse(shipment -> {
                    shipment.setStatus(Shipment.Status.DELIVERED);
                    shipment.setDeliveredAt(deliveredAt);
                    shipment.setRaw(rawPayload);
                    shipmentRepository.save(shipment);

                    Order order = shipment.getOrder();
                    order.setDeliveredAt(deliveredAt);
                    orderRepository.save(order);

                    scheduleDisputeWindow(order, deliveredAt);
                    log.info("Shipment {} marked delivered", trackingNumber);
                }, () -> log.warn("No shipment found for tracking {}", trackingNumber));
    }

    private void scheduleDisputeWindow(Order order, OffsetDateTime deliveredAt) {
        OffsetDateTime startsAt = deliveredAt != null ? deliveredAt : OffsetDateTime.now();
        int hours = resolveDisputeWindowHours(order.getMerchant());
        OffsetDateTime expiresAt = startsAt.plusHours(hours);

        Timer timer = timerRepository.findByOrder_IdAndType(order.getId(), Timer.Type.DISPUTE_WINDOW)
                .orElseGet(() -> {
                    Timer t = new Timer();
                    t.setId(UUID.randomUUID());
                    t.setOrder(order);
                    t.setType(Timer.Type.DISPUTE_WINDOW);
                    return t;
                });
        timer.setStartsAt(startsAt);
        timer.setExpiresAt(expiresAt);
        timer.setState(Timer.State.SCHEDULED);
        timerRepository.save(timer);
        log.info("Scheduled dispute window for order {} expiring at {}", order.getId(), expiresAt);
    }

    private int resolveDisputeWindowHours(Merchant merchant) {
        if (merchant != null && merchant.getDisputeWindowHours() != null && merchant.getDisputeWindowHours() > 0) {
            return merchant.getDisputeWindowHours();
        }
        return ensurebackProperties.getDisputeWindowDefaultHours();
    }

    private void callAfterShipRegister(String carrierCode, String trackingNumber, String buyerEmail) {
        try {
            afterShipClient.post()
                    .uri("/trackings")
                    .bodyValue(new TrackingRequest(new TrackingData(carrierCode, trackingNumber, new String[]{buyerEmail})))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (RuntimeException ex) {
            log.warn("Failed to register tracking with AfterShip: {}", ex.getMessage());
        }
    }

    private record TrackingRequest(TrackingData tracking) {
    }

    private record TrackingData(String slug, String trackingNumber, String[] emails) {
    }
}