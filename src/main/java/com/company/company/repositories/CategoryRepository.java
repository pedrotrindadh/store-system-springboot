package com.company.company.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.company.company.entities.Category;

public interface CategoryRepository extends JpaRepository<Category, Long>{

}
