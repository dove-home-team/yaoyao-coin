package com.warmthdawn.mods.yaoyaocoin.mixin;

import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class MixinInventory {

    // Lnet/minecraft/world/entity/player/Inventory;placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V

    @Shadow
    @Final
    public Player player;

    @Inject(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V", at = @At(value = "HEAD"))
    private void inject_placeItemBackInInventory(ItemStack stack, boolean sendPacket, CallbackInfo ci) {
        if (stack.isEmpty()) {
            return;
        }

        CoinUtils.insertCoin(player, stack).ifPresent(remaining -> {
            stack.setCount(remaining.getCount());
        });
    }

}
