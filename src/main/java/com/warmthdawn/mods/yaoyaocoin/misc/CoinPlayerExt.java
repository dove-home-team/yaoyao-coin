package com.warmthdawn.mods.yaoyaocoin.misc;

import com.warmthdawn.mods.yaoyaocoin.network.PacketSyncCoin;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

public interface CoinPlayerExt {

    default ItemStack extractCoinFromStorage(ItemStack stack, int amount) {
        ItemStack ret =  extractCoinFromStorage(stack, amount, true);

        if (this instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(serverPlayer));
        }

        return ret;
    }

    default ItemStack extractCoinFromStorage(ItemStack stack, int amount, boolean autoTransform) {
        ItemStack ret =  CoinUtils.extractCoin((ServerPlayer) this, stack, amount, autoTransform);

        if (this instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(serverPlayer));
        }

        return ret;
    }

    default ItemStack extractCoinFromStorage(String name, int amount, boolean autoTransform) {
        ItemStack ret =  CoinUtils.extractCoin((ServerPlayer) this, name, amount, autoTransform);

        if (this instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(serverPlayer));
        }

        return ret;
    }

    default ItemStack insertCoinToStorage(ItemStack stack) {
        ItemStack ret = CoinUtils.insertCoin((ServerPlayer) this, stack);

        if (this instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(serverPlayer));
        }

        return ret;
    }


}
