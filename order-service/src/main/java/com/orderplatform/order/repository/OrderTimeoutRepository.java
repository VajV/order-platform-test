package com.orderplatform.order.repository;

import com.orderplatform.order.domain.entity.OrderTimeout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderTimeoutRepository extends JpaRepository<OrderTimeout, Long> {
    List<OrderTimeout> findByExpiresAtBefore(LocalDateTime expiresAt);
}
