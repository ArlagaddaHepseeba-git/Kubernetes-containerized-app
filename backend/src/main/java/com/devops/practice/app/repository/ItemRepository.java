package com.devops.practice.app.repository;

import com.devops.practice.app.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}