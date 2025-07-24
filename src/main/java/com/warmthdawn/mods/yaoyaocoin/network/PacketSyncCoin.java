package com.warmthdawn.mods.yaoyaocoin.network;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.event.CoinRefreshedEvent;
import com.warmthdawn.mods.yaoyaocoin.gui.ClientCoinStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record PacketSyncCoin(
        HashMap<String, Entry> coinData
) {

    private record Entry(int count, boolean visibility) {
    }

    public static PacketSyncCoin fromPlayer(Player player) {
        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();

        return new PacketSyncCoin(inventory.map(it -> {
            HashMap<String, Entry> map = new HashMap<>();
            CoinManager manager = CoinManager.getInstance();
            for(int i=0;i<it.getSlots();i++) {
                int count = it.getCoinCount(i);
                boolean visibility = it.getVisibility(i);

                CoinType type = manager.getCoinType(i);

                map.put(type.name(), new Entry(count, visibility));
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

        for (Map.Entry<String, Entry> entry : coinData.entrySet()) {
            storage.setSlotCount(entry.getKey(), entry.getValue().count);
            storage.setSlotVisibility(entry.getKey(), entry.getValue().visibility);
        }

        MinecraftForge.EVENT_BUS.post(new CoinRefreshedEvent());


    }

    public void encoder(FriendlyByteBuf buffer) {
        if (coinData == null || coinData.isEmpty()) {
            buffer.writeVarInt(0);
        } else {
            buffer.writeVarInt(coinData.size());

            for (Map.Entry<String, Entry> entry : coinData.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeVarInt(entry.getValue().count);
                buffer.writeBoolean(entry.getValue().visibility);
            }
        }
    }

    public static PacketSyncCoin decoder(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        HashMap<String, Entry> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buffer.readUtf();
            Entry entry = new Entry(
                    buffer.readVarInt(),
                    buffer.readBoolean()
            );
            map.put(key, entry);
        }
        return new PacketSyncCoin(map);
    }


    public void messageConsumer(Supplier<NetworkEvent.Context> ctx) {
        updateClient();
    }


}
