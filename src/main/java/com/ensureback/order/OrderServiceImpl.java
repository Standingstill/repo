package com.ensureback.order;

import com.ensureback.order.dto.CreateOrderRequest;
import com.ensureback.order.dto.OrderDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<OrderDto> create(CreateOrderRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<OrderDto> findById(UUID orderId) {
        return Optional.empty();
    }

    @Override
    public List<OrderDto> listByMerchant(UUID merchantId) {
        return List.of();
    }

    @Override
    public Optional<OrderDto> updateStatus(UUID orderId, String status) {
        return Optional.empty();
    }
}