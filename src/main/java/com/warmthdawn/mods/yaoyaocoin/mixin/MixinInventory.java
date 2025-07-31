package com.warmthdawn.mods.yaoyaocoin.mixin;

import com.warmthdawn.mods.yaoyaocoin.YaoYaoCoin;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import com.warmthdawn.mods.yaoyaocoin.network.PacketSyncCoinSingle;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
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
        CoinType type = CoinUtils.findType(stack);
        if (type == null) {
            return;
        }

        ItemStack remaining = CoinUtils.insertCoin(player, stack);
        stack.setCount(remaining.getCount());

        if(sendPacket && player instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoinSingle.fromPlayer(player, type));
        }

    }

}
