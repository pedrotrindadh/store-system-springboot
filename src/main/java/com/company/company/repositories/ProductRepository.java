package com.company.company.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.company.company.entities.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{

}
