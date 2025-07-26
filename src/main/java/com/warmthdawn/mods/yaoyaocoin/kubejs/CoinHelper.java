package com.warmthdawn.mods.yaoyaocoin.kubejs;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import com.warmthdawn.mods.yaoyaocoin.network.PacketSyncCoin;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

public class CoinHelper {
    private final ServerPlayer player;

    private CoinHelper(ServerPlayer player) {
        this.player = player;
    }

    public static CoinHelper of(ServerPlayer player) {
        return new CoinHelper(player);
    }

    @Info(value = "Insert a coin into the player's inventory, return the remaining coin if full or cannot insert", params = {
            @Param(name = "stack", value = "The coin item to insert")
    })
    public ItemStack insert(ItemStack stack) {
        if (this.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack ret = CoinUtils.insertCoin(player, stack);
        sendCoinUpdatePacket();
        return ret;
    }

    @Info(value = "Give a coin to the player", params = {
            @Param(name = "type", value = "The coin type"),
            @Param(name = "amount", value = "The amount of coins to give")
    })
    public void give(CoinType type, int amount) {
        if (this.player == null) {
            return;
        }
        if (type == null) {
            return;
        }
        LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                .getCapability(CoinCapability.COIN_INVENTORY).cast();
        inv.ifPresent(coin -> {
            int count = coin.getCoinCount(type.id());
            count = Math.min(type.maxStackSize(), count + amount);
            coin.setCoinCount(type.id(), count);
            sendCoinUpdatePacket();
        });
        sendCoinUpdatePacket();
    }

    @Info(value = "Extract a coin from the player's inventory", params = {
            @Param(name = "stack", value = "The coin item stack to extract"),
            @Param(name = "amount", value = "The amount of coins to extract"),
            @Param(name = "autoTransform", value = "Whether to auto transform the coin (default: true)")
    })
    public ItemStack extract(ItemStack stack, int amount, boolean autoTransform) {
        if (this.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack ret = CoinUtils.extractCoin(player, stack, amount, autoTransform);
        sendCoinUpdatePacket();
        return ret;
    }


    @Info(value = "Consume a coin from the player's inventory by coin type", params = {
            @Param(name = "type", value = "The coin type"),
            @Param(name = "amount", value = "The amount of coins to consume")
    })
    public ItemStack consume(CoinType type, int amount) {
        if (this.player == null) {
            return ItemStack.EMPTY;
        }
        return consume(type, amount, true);
    }

    @Info(value = "Consume a coin from the player's inventory by coin type", params = {
            @Param(name = "type", value = "The coin type"),
            @Param(name = "amount", value = "The amount of coins to consume"),
            @Param(name = "autoTransform", value = "Whether to auto transform the coin (default: true)")
    })
    public ItemStack consume(CoinType type, int amount, boolean autoTransform) {
        if (this.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack ret = CoinUtils.extractCoin(player, type, amount, autoTransform);
        sendCoinUpdatePacket();
        return ret;
    }

    @Info(value = "Set the enable state of the coin type", params = {
            @Param(name = "type", value = "The coin type"),
            @Param(name = "enable", value = "The enable state")
    })
    public void setEnable(CoinType type, boolean enable) {
        if (this.player == null) {
            return;
        }
        LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                .getCapability(CoinCapability.COIN_INVENTORY).cast();
    }

    @Info(value = "Clear all coins of the player's inventory by coin type", params = {
            @Param(name = "type", value = "The coin type")
    })
    public void clear(CoinType type) {
        if (this.player == null) {
            return;
        }
        LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                .getCapability(CoinCapability.COIN_INVENTORY).cast();
        inv.ifPresent(coin -> {
            coin.setCoinCount(type.id(), 0);
            sendCoinUpdatePacket();
        });
    }

    @HideFromJS
    private void sendCoinUpdatePacket() {
        YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), PacketSyncCoin.fromPlayer(player));
    }


}
