package com.warmthdawn.mods.yaoyaocoin.capability;

import com.warmthdawn.mods.yaoyaocoin.YaoYaoCoin;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoinInventoryCapability {

    public static class Provider implements ICapabilitySerializable<Tag> {
        private final CoinInventory inventory;

        public Provider() {
            inventory = new CoinInventory();
        }

        @Override
        public <T> LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
            return CoinCapability.COIN_INVENTORY.orEmpty(cap, LazyOptional.of(() -> inventory));
        }


        @Override
        public Tag serializeNBT() {
            return inventory.serializeNBT();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            inventory.deserializeNBT(nbt);
        }
    }

    public static class CoinInventory implements IItemHandlerModifiable, INBTSerializable<Tag> {
        private final NonNullList<ItemStack> sampleStacks;
        private final int[] coinsCounts;
        private final boolean[] coinVisibility;
        private final ArrayList<ItemStack> invalidStacks;

        public CoinInventory() {
            CoinManager manager = CoinManager.getInstance();


            sampleStacks = NonNullList.withSize(manager.getCoinTypeCount(), ItemStack.EMPTY);
            coinsCounts = new int[manager.getCoinTypeCount()];
            coinVisibility = new boolean[manager.getCoinTypeCount()];


            for (int i = 0; i < manager.getCoinTypeCount(); i++) {
                CoinType type = manager.getCoinType(i);
                sampleStacks.set(i, type.itemStack());
                coinsCounts[i] = 0;
                coinVisibility[i] = !type.hiddenDefault();
            }


            invalidStacks = new ArrayList<>();
        }

        public int getCoinCount(int slot) {
            return coinsCounts[slot];
        }

        public boolean getVisibility(int slot) {
            return coinVisibility[slot];
        }

        public ItemStack getSampleStack(int slot) {
            return sampleStacks.get(slot);
        }

        public void setCoinCount(int slot, int count) {
            coinsCounts[slot] = count;
        }

        public void setVisibility(int slot, boolean visibility) {
            coinVisibility[slot] = visibility;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= sampleStacks.size()) {
                return;
            }

            if (stack.isEmpty()) {
                coinsCounts[slot] = 0;
            }
            if (!ItemHandlerHelper.canItemStacksStack(stack, sampleStacks.get(slot))) {
                invalidStacks.add(stack);
                return;
            }

            coinsCounts[slot] = stack.getCount();
        }

        @Override
        public int getSlots() {
            return sampleStacks.size();
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= sampleStacks.size()) {
                return ItemStack.EMPTY;
            }
            if (coinsCounts[slot] == 0) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = sampleStacks.get(slot);

            return ItemHandlerHelper.copyStackWithSize(stack, coinsCounts[slot]);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int limit = getSlotLimit(slot);

            ItemStack sample = sampleStacks.get(slot);

            if (!ItemHandlerHelper.canItemStacksStack(stack, sample))
                return stack;

            if (!this.getVisibility(slot)) {
                return stack;
            }

            int existingCount = coinsCounts[slot];
            limit -= existingCount;

            if (limit <= 0)
                return stack;


            boolean reachedLimit = stack.getCount() > limit;

            if (!simulate) {
                if (reachedLimit) {
                    coinsCounts[slot] = existingCount + limit;
                } else {
                    coinsCounts[slot] = existingCount + stack.getCount();
                }
            }

            if (!reachedLimit)
                return ItemStack.EMPTY;

            return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit);

        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount == 0)
                return ItemStack.EMPTY;

            if(slot < 0 || slot >= sampleStacks.size()) {
                return ItemStack.EMPTY;
            }
            
            if (!this.getVisibility(slot)) {
                return ItemStack.EMPTY;
            }

            int existingCount = coinsCounts[slot];
            if (existingCount == 0)
                return ItemStack.EMPTY;


            ItemStack sample = sampleStacks.get(slot);


            int toExtract = Math.min(amount, sample.getMaxStackSize());

            if (existingCount <= toExtract) {
                if (!simulate) {
                    coinsCounts[slot] = 0;
                }
                return ItemHandlerHelper.copyStackWithSize(sample, existingCount);
            } else {
                if (!simulate) {
                    coinsCounts[slot] = existingCount - toExtract;
                }

                return ItemHandlerHelper.copyStackWithSize(sample, toExtract);
            }
        }

        public void clear() {
            Arrays.fill(coinsCounts, 0);
        }

        public List<ItemStack> getInvalidStacks() {
            return invalidStacks;
        }


        @Override
        public int getSlotLimit(int slot) {
            CoinManager manager = CoinManager.getInstance();
            CoinType type = manager.getCoinType(slot);
            if (type == null) {
                return 0;
            }

            return type.maxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        @Override
        public Tag serializeNBT() {
            CompoundTag nbt = new CompoundTag();

            CompoundTag stacksTag = new CompoundTag();
            nbt.put("coins", stacksTag);

            CoinManager manager = CoinManager.getInstance();
            for (int i = 0; i < sampleStacks.size(); i++) {
                ItemStack sampleStack = sampleStacks.get(i);
                int count = coinsCounts[i];

                CompoundTag entry = new CompoundTag();
                entry.put("item", sampleStack.serializeNBT());
                entry.putInt("count", count);
                entry.putBoolean("visibility", coinVisibility[i]);

                String name = manager.getCoinType(i).name();
                stacksTag.put(name, entry);
            }

            if (invalidStacks.isEmpty()) {
                return nbt;
            }

            ListTag invalidStacksTag = new ListTag();
            nbt.put("invalidStacks", invalidStacksTag);
            for (ItemStack stack : invalidStacks) {
                invalidStacksTag.add(stack.serializeNBT());
            }
            return nbt;
        }

        private void addToInvalidStacks(ItemStack sample, int count) {
            while (count > 0) {
                int stackSize = Math.min(count, sample.getMaxStackSize());
                ItemStack stack = ItemHandlerHelper.copyStackWithSize(sample, stackSize);
                invalidStacks.add(stack);
                count -= stackSize;
            }
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            if (!(nbt instanceof CompoundTag tag)) {
                return;
            }
            CoinManager manager = CoinManager.getInstance();
            CompoundTag stacksTag = tag.getCompound("coins");

            for (String key : stacksTag.getAllKeys()) {
                CompoundTag entry = stacksTag.getCompound(key);

                if (!entry.contains("item") || !entry.contains("count")) {
                    continue;
                }

                ItemStack sample = ItemStack.of(entry.getCompound("item"));
                int count = entry.getInt("count");


                CoinType type = manager.findCoinType(key);
                if (type == null) {
                    YaoYaoCoin.LOGGER.warn("Invalid coin type: {}", key);
                    addToInvalidStacks(sample, count);
                    continue;
                }

                if (!type.matches(sample)) {
                    YaoYaoCoin.LOGGER.warn("Invalid coin stack: {}", sample);
                    addToInvalidStacks(sample, count);
                    continue;
                }

                if (count > type.maxStackSize()) {
                    YaoYaoCoin.LOGGER.warn("Coin count exceeds max stack size: {}", count);
                    int overflow = count - type.maxStackSize();
                    addToInvalidStacks(sample, overflow);
                    count = type.maxStackSize();
                }

                int slot = type.id();
                coinsCounts[slot] = count;

                if(entry.contains("visibility")) {
                    boolean visibility = entry.getBoolean("visibility");
                    coinVisibility[slot] = visibility;
                }
            }

            ListTag invalidStacksTag = tag.getList("invalidStacks", 10);

            for (Tag stackTag : invalidStacksTag) {
                invalidStacks.add(ItemStack.of((CompoundTag) stackTag));
            }
        }
    }
}
