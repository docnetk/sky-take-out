package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /*
     * 添加购物车
     */
    @PostMapping("/add")
    public Result<Object> addItem(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        System.out.println("添加购物车：" + shoppingCartDTO);
        shoppingCartService.addItem(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<ShoppingCart>> getShoppingCart() {
        List<ShoppingCart> shoppingCart = shoppingCartService.getShoppingCart();
        return Result.success(shoppingCart);
    }

    @DeleteMapping("/clean")
    public Result<Object> cleanShoppingCart() {
        System.out.println("调用/user/shoppingCart，清空购物车");
        shoppingCartService.deleteShoppingCart();
        return Result.success();
    }

    @PostMapping("/sub")
    public Result<Object> subShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }
}
