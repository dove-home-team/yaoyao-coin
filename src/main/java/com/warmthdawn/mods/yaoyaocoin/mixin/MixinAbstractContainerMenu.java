package com.warmthdawn.mods.yaoyaocoin.mixin;

import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public abstract class MixinAbstractContainerMenu {

    @Shadow
    @Final
    public NonNullList<Slot> slots;


    @Unique
    private final ThreadLocal<Boolean> flag_moveItemStackTo = ThreadLocal.withInitial(() -> false);
    @Unique
    private final ThreadLocal<Player> currentPlayer = new ThreadLocal<>();


    @Inject(method = "moveItemStackTo", at = @At("HEAD"))
    private void inject_moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        flag_moveItemStackTo.set(false);
        if (stack.isEmpty()) {
            return;
        }
        if (currentPlayer.get() == null) {
            return;
        }
        if (!CoinUtils.mayCoinItem(stack)) {
            return;
        }

        CoinUtils.insertCoin(currentPlayer.get(), stack).ifPresent(remaining -> {
            stack.setCount(remaining.getCount());
            flag_moveItemStackTo.set(true);
        });
    }


    @Inject(method = "moveItemStackTo", at = @At("RETURN"), cancellable = true)
    private void inject_moveItemStackTo_ret(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        if (flag_moveItemStackTo.get()) {
            cir.setReturnValue(true);
            flag_moveItemStackTo.set(false);
        }
    }

    @Redirect(method = "doClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;"
    ))
    private ItemStack inject_quickMoveStack(AbstractContainerMenu instance, Player player, int i) {
        currentPlayer.set(player);
        ItemStack ret = instance.quickMoveStack(player, i);
        currentPlayer.set(null);
        return ret;
    }
}
