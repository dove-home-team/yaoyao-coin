package com.warmthdawn.mods.yaoyaocoin.gui;

import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public class ClientCoinStorage {

    public static final ClientCoinStorage INSTANCE = new ClientCoinStorage();


    private NonNullList<CoinSlot> slots = NonNullList.create();

    public List<CoinSlot> getSlots() {
        return slots;
    }


    public void init() {
        CoinManager manager = CoinManager.getInstance();

        slots = NonNullList.createWithCapacity(manager.getCoinTypeCount());

        for (int i = 0; i < manager.getCoinTypeCount(); i++) {
            CoinType type = manager.getCoinType(i);
            ItemStack stack = type.createItemStack();
            slots.add(new CoinSlot(i, stack));
        }

    }

    public void setSlotCount(String name, int count) {
        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.findCoinType(name);
        if (type == null) {
            return;
        }
        slots.get(type.id()).setCount(count);
    }

    public void setSlotVisibility(String name, boolean visibility) {
        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.findCoinType(name);
        if (type == null) {
            return;
        }
        slots.get(type.id()).setVisible(visibility);
    }

    public void resetSlotCount() {
        for (CoinSlot slot : slots) {
            slot.setCount(0);
        }
    }

    public ItemStack insertCoin(ItemStack stack) {
        CoinManager manager = CoinManager.getInstance();
        for (int i = 0; i < manager.getCoinTypeCount(); i++) {
            CoinType type = manager.getCoinType(i);
            if (type.matches(stack)) {
                if (!slots.get(i).isVisible()) {
                    continue;
                }
                int current = slots.get(i).getCount();
                int limit = type.maxStackSize() - current;
                if (limit <= 0) {
                    return stack;
                }
                if (stack.getCount() > limit) {
                    slots.get(i).setCount(current + limit);
                    return stack.copyWithCount(stack.getCount() - limit);
                }
                slots.get(i).setCount(current + stack.getCount());
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private ItemStack extractFromSlotTransforming(int index, int count) {
        if (!slots.get(index).isVisible()) {
            return ItemStack.EMPTY;
        }
        if (slots.get(index).getCount() >= count) {
            return extractFromSlot(index, count);
        }
        CoinManager manager = CoinManager.getInstance();

        int[] coins = new int[manager.getCoinTypeCount()];
        for (int i = 0; i < coins.length; i++) {
            CoinSlot slotIt = slots.get(i);
            coins[i] = slotIt.getCount();
        }
        int convGroup = manager.getCoinType(index).convertGroup();
        long totalMoney = manager.getTotalMoneyInGroup(coins, convGroup);

        int availableCount = (int) (totalMoney / manager.getCoinType(index).money());
        if (availableCount <= 0) {
            return ItemStack.EMPTY;
        }

        count = Math.min(availableCount, count);

        manager.computeCoins(totalMoney, coins, index, count);

        for (int i = 0; i < coins.length; i++) {
            slots.get(i).setCount(coins[i]);
        }
        return ItemHandlerHelper.copyStackWithSize(slots.get(index).getStack(), count);
    }

    private ItemStack extractFromSlot(int index, int count) {
        if (!slots.get(index).isVisible()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = slots.get(index).getStack();
        int current = slots.get(index).getCount();
        if (current == 0 || existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(count, existing.getMaxStackSize());

        if (current <= toExtract) {
            slots.get(index).setCount(0);
            return existing.copyWithCount(current);
        }

        slots.get(index).setCount(current - toExtract);
        return existing.copyWithCount(toExtract);
    }

    public ItemStack extractCoin(ItemStack stack, int count, boolean transform) {
        CoinManager manager = CoinManager.getInstance();
        for (int i = 0; i < manager.getCoinTypeCount(); i++) {
            CoinType type = manager.getCoinType(i);
            if (type.matches(stack)) {
                if (transform) {
                    return this.extractFromSlotTransforming(i, count);
                }
                return this.extractFromSlot(i, count);
            }
        }

        return ItemStack.EMPTY;
    }

    public ItemStack extractCoin(String name, int count, boolean transform) {
        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.findCoinType(name);
        if (type != null) {
            if (transform) {
                return this.extractFromSlotTransforming(type.id(), count);
            }
            return this.extractFromSlot(type.id(), count);
        }


        return ItemStack.EMPTY;
    }


}
