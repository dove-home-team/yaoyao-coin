package com.warmthdawn.mods.yaoyaocoin.network;

import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PacketCoinSlotClicked(
        int slotId,
        boolean autoTransform,
        CoinUtils.ClickType clickType,
        ItemStack stack
) {

    public static PacketCoinSlotClicked decoder(FriendlyByteBuf buffer) {
        int slotId = buffer.readVarInt();
        boolean autoTransform = buffer.readBoolean();
        CoinUtils.ClickType clickType = CoinUtils.ClickType.values()[buffer.readVarInt()];
        ItemStack stack = buffer.readItem();
        return new PacketCoinSlotClicked(slotId, autoTransform, clickType, stack);
    }

    public void encoder(FriendlyByteBuf buffer) {
        buffer.writeVarInt(slotId);
        buffer.writeBoolean(autoTransform);
        buffer.writeVarInt(clickType.ordinal());
        buffer.writeItem(stack);
    }


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        Player player = ctx.get().getSender();
        if (player == null) {
            return;
        }

        ItemStack actualMouseItem = player.containerMenu.getCarried();
        if (!ItemStack.matches(actualMouseItem, this.stack)) {
            return;
        }

        CoinUtils.handleSlotClickServer(this.slotId, this.clickType, actualMouseItem, this.autoTransform, player);

    }
}
