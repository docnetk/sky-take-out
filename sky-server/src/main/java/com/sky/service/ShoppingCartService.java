package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void addItem(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> getShoppingCart();

    void deleteShoppingCart();

    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
