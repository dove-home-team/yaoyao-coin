package com.warmthdawn.mods.yaoyaocoin.misc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface CoinPlayerExt {

    default ItemStack extractCoinFromStorage(ItemStack stack, int amount) {
        return extractCoinFromStorage(stack, amount, true);
    }

    default ItemStack extractCoinFromStorage(ItemStack stack, int amount, boolean autoTransform) {
        return CoinUtils.extractCoin((ServerPlayer) this, stack, amount, autoTransform);
    }

    default ItemStack extractCoinFromStorage(String name, int amount, boolean autoTransform) {
        return CoinUtils.extractCoin((ServerPlayer) this, name, amount, autoTransform);
    }

    default ItemStack insertCoinToStorage(ItemStack stack) {
        return CoinUtils.insertCoin((ServerPlayer) this, stack).orElse(stack);
    }


}
