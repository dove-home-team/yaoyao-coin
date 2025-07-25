package com.warmthdawn.mods.yaoyaocoin.event;

import com.warmthdawn.mods.yaoyaocoin.YaoYaoCoin;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import com.warmthdawn.mods.yaoyaocoin.network.PacketSyncCoin;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nonnull;


@Mod.EventBusSubscriber
public class CoinEventHandler {

    @SubscribeEvent
    public static void playerPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();

        if (stack.isEmpty()) {
            return;
        }

        if(!CoinUtils.mayCoinItem(stack)) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Item item = stack.getItem();
        ItemStack copy = stack.copy();
        ItemStack remaining = CoinUtils.insertCoin(player, stack);

        int takeAmount = copy.getCount() - remaining.getCount();
        if(takeAmount > 0) {
            copy.setCount(takeAmount);
            itemEntity.setItem(remaining);
            net.minecraftforge.event.ForgeEventFactory.firePlayerItemPickupEvent(player, itemEntity, copy);
            player.take(itemEntity, takeAmount);
            if(remaining.isEmpty()) {
                itemEntity.discard();
            }

            player.awardStat(Stats.ITEM_PICKED_UP.get(item), takeAmount);
            player.onItemPickup(itemEntity);

            event.setCanceled(true);

            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(serverPlayer));
        }


    }


    @SubscribeEvent
    public static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(player));
        }
    }

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();


        oldPlayer.revive();

        LazyOptional<CoinInventoryCapability.CoinInventory> oldInventory = oldPlayer.getCapability(CoinCapability.COIN_INVENTORY).cast();
        LazyOptional<CoinInventoryCapability.CoinInventory> newInventory = newPlayer.getCapability(CoinCapability.COIN_INVENTORY).cast();

        oldInventory.ifPresent(old -> newInventory.ifPresent(newInv -> {
            newInv.deserializeNBT(old.serializeNBT());
        }));

        oldPlayer.invalidateCaps();
    }


    @SubscribeEvent
    public static void playerDrop(LivingDropsEvent event) {
        LivingEntity livingEntity = event.getEntity();

        if (!(livingEntity instanceof Player player)) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();


        boolean keepInventory =
                livingEntity.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);


        if (!keepInventory) {
            inventory.ifPresent(inv -> {
                for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack stack = inv.getStackInSlot(i);
                    if (stack.isEmpty()) {
                        continue;
                    }

                    ItemEntity itemEntity = new ItemEntity(livingEntity.getCommandSenderWorld(),
                            livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), stack);

                    itemEntity.setPickUpDelay(40);
                    event.getDrops().add(itemEntity);

                }

            });
        }
    }

    @SubscribeEvent
    public static void playerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getLevel().isClientSide) {
            return;
        }

        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();
        inventory.ifPresent(inv -> {
            inv.getInvalidStacks().forEach(stack -> {
                if (stack.isEmpty()) {
                    return;
                }
                ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
            });


            inv.getInvalidStacks().clear();
        });

    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), PacketSyncCoin.fromPlayer(player));
        }
    }

    @SubscribeEvent
    public static void attachEntitiesCapabilities(AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player) {
            evt.addCapability(CoinCapability.ID_COIN_INVENTORY, new CoinInventoryCapability.Provider());
        }
    }

}
