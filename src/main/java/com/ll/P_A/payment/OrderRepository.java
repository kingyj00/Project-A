package com.ll.P_A.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerIdOrderByIdDesc(Long buyerId);
    List<Order> findByBuyerIdAndStatusOrderByIdDesc(Long buyerId, OrderStatus status);
}