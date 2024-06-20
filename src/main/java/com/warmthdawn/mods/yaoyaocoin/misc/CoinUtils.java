package com.warmthdawn.mods.yaoyaocoin.misc;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.gui.CoinSlot;
import com.warmthdawn.mods.yaoyaocoin.network.PacketCoinSlotClicked;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

public class CoinUtils {

    public enum ClickType {
        PickOne,
        PickStack,
        PickHalf,

        StoreOne,
        StoreStack,
    }

    public static ItemStack extractCoinAutoTransform(IItemHandlerModifiable inv, int slotId, int count) {
        return inv.extractItem(slotId, count, false);
    }

    public static void handleSlotClickServer(int slotId, ClickType clickType, ItemStack stack, boolean autoTransform, Player player) {

        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.getCoinType(slotId);

        if (type == null) {
            return;
        }

        LazyOptional<IItemHandlerModifiable> inventory = player.getCapability(CoinCapability.COIN_INVENTORY);
        ItemStack finalStack = stack;
        inventory.ifPresent(it -> {
            ItemStack slotStack = it.getStackInSlot(slotId);

            switch (clickType) {
                case PickOne:
                case PickStack:
                case PickHalf: {

                    if (slotStack.isEmpty() || !finalStack.isEmpty()) {
                        return;
                    }

                    int pickCount = switch (clickType) {
                        case PickOne -> 1;
                        case PickStack -> slotStack.getMaxStackSize();
                        case PickHalf -> slotStack.getMaxStackSize() / 2;
                        default -> 0;
                    };

                    ItemStack pickedStack = ItemStack.EMPTY;

                    if (autoTransform) {
                        pickedStack = CoinUtils.extractCoinAutoTransform(it, slotId, pickCount);
                    } else {
                        pickedStack = it.extractItem(slotId, pickCount, false);
                    }

                    player.containerMenu.setCarried(pickedStack);
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

                    ItemStack stackToInsert = finalStack.copy();
                    stackToInsert.setCount(insertCount);

                    ItemStack rest = it.insertItem(slotId, stackToInsert, false);

                    ItemStack restStack = finalStack.copy();
                    restStack.shrink(insertCount - rest.getCount());

                    player.containerMenu.setCarried(restStack);
                    player.containerMenu.broadcastChanges();
                }
                break;
            }
        });

    }


    public static void sendSlotClickPacket(int slotId, ClickType clickType, ItemStack stack, boolean autoTransform) {
        YaoYaoCoinNetwork.INSTANCE.sendToServer(new PacketCoinSlotClicked(slotId,autoTransform, clickType, stack));
    }

    public static ItemStack handleSlotClick(CoinSlot slot, ItemStack stack, boolean isRightClick,
                                            boolean isShiftHolding) {

        int slotId = slot.getId();


        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.getCoinType(slotId);

        if (type == null) {
            return stack;
        }


        if (stack.isEmpty()) {

            int pickCount = 0;

            int maxStackSize = slot.getStack().getMaxStackSize();
            ClickType clickType = ClickType.PickOne;
            if (isRightClick) {
                pickCount = 1;
            } else if (isShiftHolding) {
                clickType = ClickType.PickStack;
                pickCount = maxStackSize;
            } else {
                clickType = ClickType.PickHalf;
                pickCount = maxStackSize / 2;
            }

            boolean autoTransform = false;

            if (slot.getCount() < pickCount) {

                if (isShiftHolding) {
                    // TODO: compute auto transform
                    autoTransform = true;
                }

                pickCount = slot.getCount();
            }

            if (pickCount == 0) {
                return ItemStack.EMPTY;
            }

            ItemStack pickedStack = slot.getStack().copy();

            pickedStack.setCount(pickCount);
            slot.setCount(slot.getCount() - pickCount);

            sendSlotClickPacket(slotId, clickType, ItemStack.EMPTY, autoTransform);

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
