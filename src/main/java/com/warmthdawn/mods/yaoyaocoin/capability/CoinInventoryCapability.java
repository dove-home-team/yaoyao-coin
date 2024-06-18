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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        private final NonNullList<ItemStack> stacks;
        private final ArrayList<ItemStack> invalidStacks;

        public CoinInventory() {
            CoinManager manager = CoinManager.getInstance();

            List<ItemStack> initial = manager.createInventoryStacks();

            stacks = NonNullList.withSize(initial.size(), ItemStack.EMPTY);

            for (int i = 0; i < initial.size(); i++) {
                stacks.set(i, initial.get(i));
            }

            invalidStacks = new ArrayList<>();
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            stacks.set(slot, stack);
        }

        @Override
        public int getSlots() {
            return stacks.size();
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if(slot < 0 || slot >= stacks.size()) {
                return ItemStack.EMPTY;
            }
            return stacks.get(slot);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack existing = stacks.get(slot);
            int limit = getSlotLimit(slot);

            if (!existing.isEmpty()) {
                if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                    return stack;

                limit -= existing.getCount();
            }

            if (limit <= 0)
                return stack;


            boolean reachedLimit = stack.getCount() > limit;

            if (!simulate) {
                if (existing.isEmpty()) {
                    stacks.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
                } else {
                    existing.grow(reachedLimit ? limit : stack.getCount());
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

            ItemStack existing = stacks.get(slot);

            if (existing.isEmpty())
                return ItemStack.EMPTY;

            int toExtract = Math.min(amount, existing.getMaxStackSize());

            if (existing.getCount() <= toExtract) {
                if (!simulate) {
                    stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, 0));
                }
                return existing;
            } else {
                if (!simulate) {
                    stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
                }

                return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
            }
        }

        public void clear() {
            stacks.replaceAll(itemStack -> ItemHandlerHelper.copyStackWithSize(itemStack, 0));
        }

        public List<ItemStack> getInvalidStacks() {
            return invalidStacks;
        }


        @Override
        public int getSlotLimit(int slot) {
            CoinManager manager = CoinManager.getInstance();
            CoinType type = manager.getCoinType(slot);
            if(type == null) {
                return 0;
            }

            return type.maxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        public Map<CoinType, Integer> getCoinMap() {
            Map<CoinType, Integer> map = new HashMap<>();
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                CoinType type = CoinManager.getInstance().getCoinType(i);
                map.put(type, stack.getCount());
            }
            return map;
        }

        @Override
        public Tag serializeNBT() {
            CompoundTag nbt = new CompoundTag();

            CompoundTag stacksTag = new CompoundTag();
            nbt.put("stacks", stacksTag);

            CoinManager manager = CoinManager.getInstance();
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty()) {
                    continue;
                }

                String name = manager.getCoinType(i).name();
                stacksTag.put(name, stack.serializeNBT());
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

        @Override
        public void deserializeNBT(Tag nbt) {
            if (!(nbt instanceof CompoundTag tag)) {
                return;
            }
            CoinManager manager = CoinManager.getInstance();
            CompoundTag stacksTag = tag.getCompound("stacks");

            for (String key : stacksTag.getAllKeys()) {
                ItemStack stack = ItemStack.of(stacksTag.getCompound(key));
                if (stack.isEmpty()) {
                    continue;
                }

                CoinType type = manager.findCoinType(key);
                if (type == null) {
                    YaoYaoCoin.LOGGER.warn("Invalid coin type: {}", key);
                    invalidStacks.add(stack);
                    continue;
                }

                if (!type.matches(stack)) {
                    YaoYaoCoin.LOGGER.warn("Invalid coin stack: {}", stack);
                    invalidStacks.add(stack);
                    continue;
                }
                stacks.set(type.id(), stack);
            }

            ListTag invalidStacksTag = tag.getList("invalidStacks", 10);

            for (Tag stackTag : invalidStacksTag) {
                invalidStacks.add(ItemStack.of((CompoundTag) stackTag));
            }
        }
    }
}
