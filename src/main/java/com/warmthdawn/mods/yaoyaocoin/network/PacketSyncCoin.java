package com.warmthdawn.mods.yaoyaocoin.network;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.gui.ClientCoinStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record PacketSyncCoin(
        HashMap<String, Integer> coinData
) {

    public static PacketSyncCoin fromPlayer(Player player) {
        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();

        return new PacketSyncCoin(inventory.map(it -> {
            HashMap<String, Integer> map = new HashMap<>();
            Map<CoinType, Integer> coins = it.getCoinMap();
            for (Map.Entry<CoinType, Integer> entry : coins.entrySet()) {
                map.put(entry.getKey().name(), entry.getValue());
            }

            return map;

        }).orElse(null));
    }

    public void updateClient() {
        ClientCoinStorage storage = ClientCoinStorage.INSTANCE;
        storage.resetSlotCount();
        if (coinData == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : coinData.entrySet()) {
            storage.setSlotCount(entry.getKey(), entry.getValue());
        }


    }

    public void encoder(FriendlyByteBuf buffer) {
        if (coinData == null || coinData.isEmpty()) {
            buffer.writeVarInt(0);
        } else {
            buffer.writeVarInt(coinData.size());

            for (Map.Entry<String, Integer> entry : coinData.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeVarInt(entry.getValue());
            }
        }
    }

    public static PacketSyncCoin decoder(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buffer.readUtf(), buffer.readVarInt());
        }
        return new PacketSyncCoin(map);
    }


    public void messageConsumer(Supplier<NetworkEvent.Context> ctx) {
        updateClient();
    }


}
