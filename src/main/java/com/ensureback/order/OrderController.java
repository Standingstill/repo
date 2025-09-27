package com.ensureback.order;

import com.ensureback.order.dto.CreateOrderRequest;
import com.ensureback.order.dto.OrderDto;
import com.ensureback.order.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
        Optional<OrderDto> created = orderService.create(request);
        return created
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> listByMerchant(@RequestParam("merchantId") UUID merchantId) {
        List<OrderDto> orders = orderService.listByMerchant(merchantId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> findById(@PathVariable UUID orderId) {
        return ResponseEntity.of(orderService.findById(orderId));
    }

    @PostMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateStatus(@PathVariable UUID orderId,
                                                 @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.of(orderService.updateStatus(orderId, request.status()));
    }
}