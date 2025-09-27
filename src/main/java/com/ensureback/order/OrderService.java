package com.ensureback.order;

import com.ensureback.order.dto.CreateOrderRequest;
import com.ensureback.order.dto.OrderDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderService {

    Optional<OrderDto> create(CreateOrderRequest request);

    Optional<OrderDto> findById(UUID orderId);

    List<OrderDto> listByMerchant(UUID merchantId);

    Optional<OrderDto> updateStatus(UUID orderId, String status);
}