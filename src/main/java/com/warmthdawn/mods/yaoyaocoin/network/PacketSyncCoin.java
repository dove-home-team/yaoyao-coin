package com.warmthdawn.mods.yaoyaocoin.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.event.CoinRefreshedEvent;
import com.warmthdawn.mods.yaoyaocoin.gui.ClientCoinStorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent;

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
            return;
        }
        
        // Compress data using NBT
        CompoundTag nbt = new CompoundTag();
        coinData.forEach((key, entry) -> {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("count", entry.count);
            entryTag.putBoolean("visible", entry.visibility);
            nbt.put(key, entryTag);
        });
        
        buffer.writeNbt(nbt);  // Automatic compression
    }

    public static PacketSyncCoin decoder(FriendlyByteBuf buffer) {
        CompoundTag nbt = buffer.readNbt();
        if (nbt == null) {
            return new PacketSyncCoin(null);
        }
        
        HashMap<String, Entry> map = new HashMap<>();
        for (String key : nbt.getAllKeys()) {
            CompoundTag entryTag = nbt.getCompound(key);
            map.put(key, new Entry(
                entryTag.getInt("count"),
                entryTag.getBoolean("visible")
            ));
        }
        return new PacketSyncCoin(map);
    }


    public void messageConsumer(Supplier<NetworkEvent.Context> ctx) {
        updateClient();
    }


}
