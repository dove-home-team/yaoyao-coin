package com.warmthdawn.mods.yaoyaocoin.misc;

import com.google.common.collect.ImmutableMap;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.gui.ClientCoinStorage;
import com.warmthdawn.mods.yaoyaocoin.gui.CoinSlot;
import com.warmthdawn.mods.yaoyaocoin.network.PacketCoinSlotClicked;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class CoinUtils {


    public enum ClickType {
        //        PickOne,
        PickStack,
        PickHalf,

        QuickMoveStack,
        QuickMoveOne,

        StoreOne,
        StoreStack,
    }

    private static final Lazy<Map<Item, int[]>> playerCoinSet = Lazy.of(() -> {
        HashMap<Item, IntSet> map = new HashMap<>();

        for (int i = 0; i < CoinManager.getInstance().getCoinTypeCount(); i++) {
            CoinType coinType = CoinManager.getInstance().getCoinType(i);
            map.computeIfAbsent(coinType.itemStack().getItem(), it -> new IntArraySet(10)).add(i);
        }

        ImmutableMap.Builder<Item, int[]> builder = ImmutableMap.builder();
        for (Map.Entry<Item, IntSet> entry : map.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().toArray(new int[0]));
        }
        return builder.build();
    });

    public static boolean mayCoinItem(ItemStack stack) {
        return playerCoinSet.get().containsKey(stack.getItem());
    }


    public static CoinType findType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CoinManager manager = CoinManager.getInstance();
        int[] possible = playerCoinSet.get().get(stack.getItem());
        if (possible == null) {
            return null;
        }
        for (int id : possible) {
            CoinType type = manager.getCoinType(id);
            if (type.matches(stack)) {
                return type;
            }
        }

        return null;

    }

    public static ItemStack insertCoin(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!CoinUtils.mayCoinItem(stack)) {
            return stack;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ClientCoinStorage.INSTANCE.insertCoin(stack);
        }
        AtomicReference<ItemStack> restStackSimulated = new AtomicReference<>(stack.copy());

        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();

        inventory.ifPresent(inv -> {
            ItemStack rest = ItemHandlerHelper.insertItem(inv, restStackSimulated.get(), true);
            restStackSimulated.set(rest);
        });

        if (restStackSimulated.get().getCount() != stack.getCount()) {
            AtomicReference<ItemStack> remainingStack = new AtomicReference<>(stack.copy());
            inventory.ifPresent(inv -> {
                ItemStack rest = ItemHandlerHelper.insertItem(inv, remainingStack.get(), false);

                remainingStack.set(rest);
            });

            return remainingStack.get();
        }

        return stack;

    }

    public static ItemStack extractCoin(Player player, String coinName, int count, boolean autoTransform) {


        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.findCoinType(coinName);
        if (type == null) {
            return ItemStack.EMPTY;
        }
        return extractCoin(player, type, count, autoTransform);
    }

    public static ItemStack extractCoin(Player player, CoinType type, int count, boolean autoTransform) {

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ClientCoinStorage.INSTANCE.extractCoin(type, count, autoTransform);
        }
        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();

        return inventory.map(inv -> {
            int slotId = type.id();
            if (!inv.getVisibility(slotId)) {
                return ItemStack.EMPTY;
            }
            // find slot id
            ItemStack pickedStack = ItemStack.EMPTY;
            if (autoTransform) {
                pickedStack = CoinUtils.extractCoinAutoTransform(inv, slotId, count, player);
            } else {
                pickedStack = inv.extractItem(slotId, count, false);
            }

            return pickedStack;
        }).orElse(ItemStack.EMPTY);


    }

    public static ItemStack extractCoin(Player player, ItemStack stack, int count, boolean autoTransform) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!CoinUtils.mayCoinItem(stack)) {
            return ItemStack.EMPTY;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ClientCoinStorage.INSTANCE.extractCoin(stack, count, autoTransform);
        }

        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();

        return inventory.map(inv -> {

            // find slot id
            for (int slotId = 0; slotId < inv.getSlots(); slotId++) {
                if (!inv.getVisibility(slotId)) {
                    continue;
                }
                ItemStack sampleStack = inv.getSampleStack(slotId);
                if (!ItemStack.isSameItemSameTags(stack, sampleStack)) {
                    continue;
                }
                ItemStack pickedStack = ItemStack.EMPTY;
                if (autoTransform) {
                    pickedStack = CoinUtils.extractCoinAutoTransform(inv, slotId, count, player);
                } else {
                    pickedStack = inv.extractItem(slotId, count, false);
                }

                return pickedStack;
            }

            return ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY);


    }


    public static ItemStack extractCoinAutoTransform(CoinInventoryCapability.CoinInventory inv, int slotId, int count, Player player) {

        CoinManager manager = CoinManager.getInstance();

        int currentCount = inv.getCoinCount(slotId);
        if (currentCount >= count) {
            return inv.extractItem(slotId, count, false);
        }

        int[] coins = new int[manager.getCoinTypeCount()];
        for (int i = 0; i < coins.length; i++) {
            coins[i] = inv.getCoinCount(i);
        }

        int convertGroup = manager.getCoinType(slotId).convertGroup();

        long totalMoney = manager.getTotalMoneyInGroup(coins, convertGroup);
        int availableCount = (int) (totalMoney / manager.getCoinType(slotId).money());
        count = Math.min(availableCount, count);

        manager.computeCoins(totalMoney, coins, slotId, count);

        ItemStack extractedStack = inv.getSampleStack(slotId);

        extractedStack = ItemHandlerHelper.copyStackWithSize(extractedStack, count);

        for (int i = 0; i < coins.length; i++) {
            int newCount = coins[i];
            CoinType coinType = manager.getCoinType(i);
            ItemStack sampleStack = inv.getSampleStack(i);

            if (newCount > coinType.maxStackSize()) {
                ItemStack stackToGive = ItemHandlerHelper.copyStackWithSize(sampleStack, newCount - coinType.maxStackSize());
                ItemHandlerHelper.giveItemToPlayer(player, stackToGive);
                newCount = coinType.maxStackSize();
            }
            inv.setCoinCount(i, newCount);
        }

        if (extractedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return extractedStack;


    }

    private static int[] quickMoveRange(AbstractContainerMenu container) {
        if (container instanceof InventoryMenu) {
            return new int[]{9, 45};
        }

        return new int[]{0, container.slots.size()};
    }

    private static int handleQuickMove(ItemStack sample, int count, AbstractContainerMenu container) {
        int[] range = quickMoveRange(container);

        for (int i = range[0]; i < range[1]; i++) {
            Slot slot = container.getSlot(i);
            ItemStack itemstack = slot.getItem();
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(sample, itemstack)) {
                int j = itemstack.getCount() + count;
                int maxSize = Math.min(slot.getMaxStackSize(), sample.getMaxStackSize());
                if (j <= maxSize) {
                    count = 0;
                    itemstack.setCount(j);
                    slot.setChanged();
                } else if (itemstack.getCount() < maxSize) {
                    count -= maxSize - itemstack.getCount();
                    itemstack.setCount(maxSize);
                    slot.setChanged();
                }
            }
        }

        if (count == 0) {
            return 0;
        }

        for (int i = range[0]; i < range[1]; i++) {
            Slot slot1 = container.getSlot(i);
            ItemStack itemstack1 = slot1.getItem();
            if (itemstack1.isEmpty() && slot1.mayPlace(sample)) {
                if (count > slot1.getMaxStackSize()) {
                    ItemStack stack = ItemHandlerHelper.copyStackWithSize(sample, slot1.getMaxStackSize());
                    count -= slot1.getMaxStackSize();
                    slot1.set(stack);
                } else {
                    ItemStack stack = ItemHandlerHelper.copyStackWithSize(sample, count);
                    count = 0;
                    slot1.set(stack);
                }

                slot1.setChanged();
                break;
            }
        }

        return count;
    }

    public static void handleSlotClickServer(int slotId, ClickType clickType, ItemStack stack, boolean autoTransform, Player player) {

        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.getCoinType(slotId);

        if (type == null) {
            return;
        }

        LazyOptional<CoinInventoryCapability.CoinInventory> inventory = player.getCapability(CoinCapability.COIN_INVENTORY).cast();
        ItemStack finalStack = stack;
        inventory.ifPresent(it -> {
            if (!it.getVisibility(slotId)) {
                return;
            }
            ItemStack slotStack = it.getStackInSlot(slotId);

            switch (clickType) {
                case QuickMoveOne:
                case QuickMoveStack:
                case PickStack:
                case PickHalf: {
                    if (!finalStack.isEmpty()) {
                        return;
                    }
                    int pickCount = switch (clickType) {
                        case QuickMoveOne -> 1;
                        case PickStack, QuickMoveStack -> slotStack.getMaxStackSize();
                        case PickHalf -> slotStack.getMaxStackSize() / 2;
                        default -> 0;
                    };

                    boolean isQuickMove = clickType == ClickType.QuickMoveOne || clickType == ClickType.QuickMoveStack;

                    ItemStack pickedStack = ItemStack.EMPTY;
                    if (autoTransform) {

                        pickedStack = CoinUtils.extractCoinAutoTransform(it, slotId, pickCount, player);
                    } else {
                        if (slotStack.isEmpty()) {
                            return;
                        }

                        pickedStack = it.extractItem(slotId, pickCount, false);
                    }

                    if (isQuickMove) {
                        int rest = handleQuickMove(pickedStack, pickedStack.getCount(), player.containerMenu);
                        if (rest > 0) {
                            pickedStack.setCount(rest);
                            player.containerMenu.setCarried(pickedStack);
                        } else {
                            player.containerMenu.setCarried(ItemStack.EMPTY);
                        }
                    } else {
                        player.containerMenu.setCarried(pickedStack);
                    }

                    player.containerMenu.broadcastChanges();

                }
                break;
                case StoreOne:
                case StoreStack: {
                    if (finalStack.isEmpty()) {
                        return;
                    }

                    int insertCount = switch (clickType) {
                        case StoreOne -> 1;
                        case StoreStack -> finalStack.getCount();
                        default -> 0;
                    };

                    ItemStack stackToInsert = ItemHandlerHelper.copyStackWithSize(finalStack, insertCount);

                    ItemStack rest = it.insertItem(slotId, stackToInsert, false);

                    int restCount = finalStack.getCount() - insertCount + rest.getCount();

                    ItemStack restStack = ItemHandlerHelper.copyStackWithSize(finalStack, restCount);

                    player.containerMenu.setCarried(restStack);
                    player.containerMenu.broadcastChanges();
                }
                break;
            }
        });

    }


    public static void sendSlotClickPacket(int slotId, ClickType clickType, ItemStack stack, boolean autoTransform) {
        YaoYaoCoinNetwork.INSTANCE.sendToServer(new PacketCoinSlotClicked(slotId, autoTransform, clickType, stack));
    }

    public static ItemStack handleSlotClick(CoinSlot slot, ItemStack stack, boolean isRightClick,
                                            boolean isShiftHolding) {

        int slotId = slot.getId();


        CoinManager manager = CoinManager.getInstance();
        ClientCoinStorage storage = ClientCoinStorage.INSTANCE;
        CoinType type = manager.getCoinType(slotId);

        if (type == null) {
            return stack;
        }

        if (!storage.getSlots().get(type.id()).isVisible()) {
            return stack;
        }

        if (stack.isEmpty()) {

            int maxStackSize = slot.getStack().getMaxStackSize();
            boolean quickMove = false;

            int pickCount;
            ClickType clickType;
            if (isShiftHolding && isRightClick) {
                clickType = ClickType.QuickMoveOne;
                pickCount = 1;
                quickMove = true;
            } else if (isShiftHolding) {
                clickType = ClickType.QuickMoveStack;
                pickCount = maxStackSize;
                quickMove = true;
            } else if (isRightClick) {
                clickType = ClickType.PickHalf;
                pickCount = maxStackSize / 2;
            } else {
                clickType = ClickType.PickStack;
                pickCount = maxStackSize;
            }

            boolean autoTransform = false;

            long totalMoney = 0;

            if (slot.getCount() < pickCount) {

                if (isShiftHolding) {
                    for (int i = 0; i < manager.getCoinTypeCount(); i++) {
                        CoinType coinType = manager.getCoinType(i);
                        if (coinType.convertGroup() != type.convertGroup()) {
                            continue;
                        }
                        CoinSlot slotIt = storage.getSlots().get(i);
                        totalMoney += (long) coinType.money() * slotIt.getCount();
                    }

                    int availableCount = (int) (totalMoney / type.money());

                    pickCount = Math.min(availableCount, pickCount);
                    autoTransform = true;
                } else {
                    pickCount = slot.getCount();
                }
            }

            if (pickCount == 0) {
                return ItemStack.EMPTY;
            }
            ItemStack pickedStack = slot.getStack().copy();
            pickedStack.setCount(pickCount);
            if (autoTransform) {

                int[] coins = new int[manager.getCoinTypeCount()];
                for (int i = 0; i < coins.length; i++) {
                    CoinSlot slotIt = storage.getSlots().get(i);
                    coins[i] = slotIt.getCount();
                }

                manager.computeCoins(totalMoney, coins, slotId, pickCount);

                for (int i = 0; i < coins.length; i++) {
                    CoinSlot slotIt = storage.getSlots().get(i);
                    CoinType coinType = manager.getCoinType(i);
                    if (coins[i] > coinType.maxStackSize()) {
                        coins[i] = coinType.maxStackSize();
                    }
                    slotIt.setCount(coins[i]);
                }
            } else {
                pickedStack.setCount(pickCount);
                slot.setCount(slot.getCount() - pickCount);
            }

            sendSlotClickPacket(slotId, clickType, ItemStack.EMPTY, autoTransform);

            if (quickMove) {
                return ItemStack.EMPTY;
            }

            return pickedStack;

        } else {

            ItemStack slotStack = slot.getStack();

            if (!ItemHandlerHelper.canItemStacksStack(stack, slotStack)) {
                return stack;
            }


            if (isRightClick) {
                // insert one to slot
                int maxSize = type.maxStackSize();

                if (slot.getCount() < maxSize) {
                    slot.setCount(slot.getCount() + 1);
                }
                ItemStack retStack = stack.copy();

                retStack.shrink(1);

                sendSlotClickPacket(slotId, ClickType.StoreOne, stack, false);
                if (retStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                return retStack;
            } else {
                // insert all to slot

                int maxSize = type.maxStackSize();

                int insertCount = Math.min(maxSize - slot.getCount(), stack.getCount());

                slot.setCount(slot.getCount() + insertCount);

                ItemStack retStack = stack.copy();

                retStack.shrink(insertCount);

                sendSlotClickPacket(slotId, ClickType.StoreStack, stack, false);
                if (retStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                return retStack;

            }

        }
    }
}
