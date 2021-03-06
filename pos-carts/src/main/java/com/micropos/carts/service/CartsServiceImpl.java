package com.micropos.carts.service;

import com.micropos.carts.model.Cart;
import com.micropos.carts.model.Item;
import com.micropos.carts.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class CartsServiceImpl implements CartsService, Serializable {

    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    @Autowired
    private CircuitBreakerFactory factory;

    @Override
    public Cart add(Cart cart, String productId, int amount) {
        CircuitBreaker cb = factory.create("circuitbreaker");
        return cb.run(() -> {
            ResponseEntity<Product> productResponseEntity = restTemplate.
                    getForEntity("http://pos-products/api/products/" + productId, Product.class);
            Product product = productResponseEntity.getBody();
            if (product == null) return cart;
            cart.addItem(new Item(product, amount));
            return cart;
        }, ex -> {
            cart.addItem(new Item(new Product(productId, "故障", 0, ""), amount));
            return cart;
        });
    }
}
