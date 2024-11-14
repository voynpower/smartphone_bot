package org.example.smarphonebot2.repository;

import org.example.smarphonebot2.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Model, Long> {
    List<Model> findByNameContainingIgnoreCase(String name);
}
