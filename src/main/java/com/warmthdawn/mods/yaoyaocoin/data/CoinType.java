package com.warmthdawn.mods.yaoyaocoin.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public record CoinType(
        int id,
        String name,
        int money,
        int convertGroup,
        int maxStackSize,
        ResourceLocation itemName,
        CompoundTag itemTag
) {

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!Objects.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()), itemName)) {
            return false;
        }

        if (!stack.hasTag() && itemTag == null) {
            return true;
        }

        return stack.hasTag() && Objects.equals(stack.getTag(), itemTag);
    }


    public ItemStack createItemStack() {
        Item item = ForgeRegistries.ITEMS.getValue(itemName);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        stack.setCount(1);
        if(itemTag != null) {
            stack.setTag(itemTag);
        }
        return stack;
    }

}
