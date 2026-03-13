package com.company.company.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.company.company.entities.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{

}
