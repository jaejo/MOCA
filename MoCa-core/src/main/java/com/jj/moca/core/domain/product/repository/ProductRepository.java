package com.jj.moca.core.domain.product.repository;

import com.jj.moca.core.domain.product.entity.Product;
import com.jj.moca.core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

}
