package com.warmthdawn.mods.yaoyaocoin.event;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.network.PacketSyncCoin;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber
public class CoinEventHandler {

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getPlayer();


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
        LivingEntity livingEntity = (LivingEntity) event.getEntity();

        if (!(livingEntity instanceof Player player)) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();


        boolean keepInventory =
                livingEntity.getLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);


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
    public static void playerJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getWorld().isClientSide) {
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
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            YaoYaoCoinNetwork.INSTANCE.sendTo(PacketSyncCoin.fromPlayer(player), serverPlayer.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    @SubscribeEvent
    public static void attachEntitiesCapabilities(AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player) {
            evt.addCapability(CoinCapability.ID_COIN_INVENTORY, new CoinInventoryCapability.Provider());
        }
    }

}
