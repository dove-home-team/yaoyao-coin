package com.warmthdawn.mods.yaoyaocoin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.network.PacketSyncCoin;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

public class CoinCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("yaoyaocoin")
                        .requires(ctx -> ctx.hasPermission(2))
                        // enable
                        .then(Commands.literal("enable")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("slot_name", CoinNameArgument.coinName())
                                                .executes(ctx -> {
                                                    var players = EntityArgument.getPlayers(ctx, "players");
                                                    CoinType type = CoinNameArgument.getCoin(ctx, "slot_name");
                                                    return setSlotVisibility(ctx.getSource(), players, type, true);
                                                }))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> {
                                                    var players = EntityArgument.getPlayers(ctx, "players");
                                                    return setAllSlotVisibility(ctx.getSource(), players, true);
                                                }))))
                        // disable
                        .then(Commands.literal("disable")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("slot_name", CoinNameArgument.coinName())
                                                .executes(ctx -> {
                                                    var players = EntityArgument.getPlayers(ctx, "players");
                                                    CoinType type = CoinNameArgument.getCoin(ctx, "slot_name");
                                                    return setSlotVisibility(ctx.getSource(), players, type, false);
                                                }))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> {
                                                    var players = EntityArgument.getPlayers(ctx, "players");
                                                    return setAllSlotVisibility(ctx.getSource(), players, false);
                                                }))))
                        // give
                        .then(Commands.literal("give")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("slot_name", CoinNameArgument.coinName())
                                                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> {
                                                            var players = EntityArgument.getPlayers(ctx, "players");
                                                            CoinType type = CoinNameArgument.getCoin(ctx, "slot_name");
                                                            int number = IntegerArgumentType.getInteger(ctx, "number");
                                                            return giveCoin(ctx.getSource(), players, type, number);
                                                        }))
                                                .executes(ctx -> {
                                                    var players = EntityArgument.getPlayers(ctx, "players");
                                                    CoinType type = CoinNameArgument.getCoin(ctx, "slot_name");
                                                    return giveCoin(ctx.getSource(), players, type, 1);
                                                }))))
                        // consume
                        .then(Commands.literal("consume")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("slot_name", CoinNameArgument.coinName())
                                                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> {
                                                            var players = EntityArgument.getPlayers(ctx, "players");
                                                            CoinType type = CoinNameArgument.getCoin(ctx, "slot_name");
                                                            int number = IntegerArgumentType.getInteger(ctx, "number");
                                                            return consumeCoin(ctx.getSource(), players, type, number);
                                                        }))
                                                .executes(ctx -> {
                                                    var players = EntityArgument.getPlayers(ctx, "players");
                                                    CoinType type = CoinNameArgument.getCoin(ctx, "slot_name");
                                                    return consumeCoin(ctx.getSource(), players, type, 1);
                                                }))))
                        // clear
                        .then(Commands.literal("clear")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> {
                                            var players = EntityArgument.getPlayers(ctx, "players");
                                            return clearCoin(ctx.getSource(), players);
                                        })))
        );
    }

    private static int setAllSlotVisibility(CommandSourceStack source, Collection<? extends Entity> targets, boolean visible) {
        for (Entity target : targets) {
            if (target instanceof ServerPlayer player) {
                LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                        .getCapability(CoinCapability.COIN_INVENTORY).cast();
                inv.ifPresent(coin -> {
                    for (int i = 0; i < coin.getSlots(); i++) {
                        coin.setVisibility(i, visible);
                    }
                    sendCoinUpdatePacket(player);
                });
            }
        }

        return targets.size();
    }

    private static int setSlotVisibility(CommandSourceStack source, Collection<? extends Entity> targets, CoinType type,
                                         boolean visible) {
        for (Entity target : targets) {
            if (target instanceof ServerPlayer player) {
                LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                        .getCapability(CoinCapability.COIN_INVENTORY).cast();
                inv.ifPresent(coin -> {
                    coin.setVisibility(type.id(), visible);
                    sendCoinUpdatePacket(player);
                });
            }
        }

        return targets.size();
    }

    private static int giveCoin(CommandSourceStack source, Collection<? extends Entity> targets, CoinType type,
                                int number) {
        for (Entity target : targets) {
            if (target instanceof ServerPlayer player) {
                LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                        .getCapability(CoinCapability.COIN_INVENTORY).cast();
                inv.ifPresent(coin -> {
                    int count = coin.getCoinCount(type.id());
                    count = Math.min(type.maxStackSize(), count + number);
                    coin.setCoinCount(type.id(), count);
                    sendCoinUpdatePacket(player);
                });
            }
        }

        return targets.size();
    }

    private static void sendCoinUpdatePacket(ServerPlayer player) {
        YaoYaoCoinNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), PacketSyncCoin.fromPlayer(player));
    }

    private static int consumeCoin(CommandSourceStack source, Collection<? extends Entity> targets, CoinType type,
                                   int number) {
        for (Entity target : targets) {
            if (target instanceof ServerPlayer player) {
                LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                        .getCapability(CoinCapability.COIN_INVENTORY).cast();
                inv.ifPresent(coin -> {
                    int count = coin.getCoinCount(type.id());
                    count = Math.max(0, count - number);
                    coin.setCoinCount(type.id(), count);
                    sendCoinUpdatePacket(player);
                });
            }
        }

        return targets.size();
    }

    private static int clearCoin(CommandSourceStack source, Collection<? extends Entity> targets) {
        for (Entity target : targets) {
            if (target instanceof ServerPlayer player) {
                LazyOptional<CoinInventoryCapability.CoinInventory> inv = player
                        .getCapability(CoinCapability.COIN_INVENTORY).cast();
                inv.ifPresent(coin -> {
                    coin.clear();
                    sendCoinUpdatePacket(player);
                });
            }
        }

        return targets.size();
    }
}
