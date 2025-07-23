package com.warmthdawn.mods.yaoyaocoin.mixin;

import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MixinMerchantMenu extends AbstractContainerMenu {

    protected MixinMerchantMenu(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Shadow
    @Final
    private MerchantContainer tradeContainer;

    @Shadow
    @Final
    private Merchant trader;

    @Redirect(method = "tryMoveItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
    private boolean redirect_tryMoveItems(MerchantMenu instance, ItemStack itemStack, int startIndex, int endIndex, boolean reverseDirection) {
        if (CoinUtils.mayCoinItem(itemStack)) {
            ItemStack remaining = CoinUtils.insertCoin(this.trader.getTradingPlayer(), itemStack);
            if (remaining.getCount() != itemStack.getCount()) {
                itemStack.setCount(remaining.getCount());
                this.moveItemStackTo(itemStack, startIndex, endIndex, reverseDirection);
                return true;
            }
        }

        return this.moveItemStackTo(itemStack, startIndex, endIndex, reverseDirection);
    }

    @Inject(method = "moveFromInventoryToPaymentSlot", at = @At("RETURN"))
    private void inject_moveFromInventoryToPaymentSlot(int paymentSlotIndex, ItemStack paymentSlot, CallbackInfo ci) {
        if (paymentSlot.isEmpty()) {
            return;
        }


        if (!CoinUtils.mayCoinItem(paymentSlot)) {
            return;
        }


        ItemStack existing = this.tradeContainer.getItem(paymentSlotIndex);
        int existingCount = existing.isEmpty() ? 0 : existing.getCount();

        if (existingCount >= paymentSlot.getMaxStackSize()) {
            return;
        }

        int toExtract = paymentSlot.getMaxStackSize() - existingCount;

        ItemStack pickedStack = CoinUtils.extractCoin(this.trader.getTradingPlayer(), paymentSlot, toExtract, true);

        if (pickedStack.isEmpty()) {
            return;
        }


        ItemStack pickedCopy = pickedStack.copyWithCount(pickedStack.getCount() + existingCount);
        this.tradeContainer.setItem(paymentSlotIndex, pickedCopy);
    }

}
