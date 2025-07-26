package com.warmthdawn.mods.yaoyaocoin.network;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.event.CoinRefreshedEvent;
import com.warmthdawn.mods.yaoyaocoin.gui.ClientCoinStorage;
import com.warmthdawn.mods.yaoyaocoin.gui.CoinSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PacketSyncCoinSingle(
        int id,
        int count,
        boolean visibility
) {

    public static PacketSyncCoinSingle fromPlayer(Player player, CoinType coinType) {
        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();

        return inventory.map(it ->
                        new PacketSyncCoinSingle(coinType.id(), it.getCoinCount(coinType.id()), it.getVisibility(coinType.id())))
                .orElse(null);
    }

    public void updateClient() {
        ClientCoinStorage storage = ClientCoinStorage.INSTANCE;
        CoinSlot slot = storage.getSlots().get(id);
        slot.setCount(count);
        if (slot.isVisible() != visibility) {
            slot.setVisible(visibility);
            MinecraftForge.EVENT_BUS.post(new CoinRefreshedEvent());
        }
    }

    public void encoder(FriendlyByteBuf buffer) {
        buffer.writeVarInt(id());
        buffer.writeVarInt(count());
        buffer.writeBoolean(visibility());
    }

    public static PacketSyncCoinSingle decoder(FriendlyByteBuf buffer) {
        return new PacketSyncCoinSingle(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
    }


    public void messageConsumer(Supplier<NetworkEvent.Context> ctx) {
        updateClient();
    }


}
