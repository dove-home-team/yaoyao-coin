package com.warmthdawn.mods.yaoyaocoin.mixin;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import com.warmthdawn.mods.yaoyaocoin.network.PacketSyncCoin;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public abstract class MixinAbstractContainerMenu {

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Inject(method = "doClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
            ordinal = 0

    ))
    private void inject_quickMoveStack(int pSlotId, int pButton, ClickType pClickType, Player pPlayer, CallbackInfo ci) {
        ItemStack stack = this.slots.get(pSlotId).getItem();
        if(!(pPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        if(!CoinUtils.mayCoinItem(stack)) {
            return;
        }

        AtomicReference<ItemStack> restStackSimulated = new AtomicReference<>(stack.copy());
        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = pPlayer.getCapability(CoinCapability.COIN_INVENTORY).cast();
        inventory.ifPresent(inv -> {
            ItemStack rest = ItemHandlerHelper.insertItem(inv, restStackSimulated.get(), true);
            restStackSimulated.set(rest);
        });

        if(restStackSimulated.get().getCount() != stack.getCount()) {
            AtomicReference<ItemStack> remainingStack = new AtomicReference<>(stack.copy());
            inventory.ifPresent(inv -> {
                ItemStack rest = ItemHandlerHelper.insertItem(inv, remainingStack.get(), false);

                remainingStack.set(rest);
            });

            this.slots.get(pSlotId).set(remainingStack.get());
            this.slots.get(pSlotId).setChanged();
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(pPlayer));
        }

    }
}
