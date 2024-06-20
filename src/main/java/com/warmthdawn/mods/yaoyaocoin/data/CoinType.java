package com.warmthdawn.mods.yaoyaocoin.data;

import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public record CoinType(
        int id,
        String name,
        int money,
        int maxStackSize,
        ResourceLocation itemName,
        Tag itemTag
) {

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem().getRegistryName().equals(itemName)) {
            return true;
        }

        if (!stack.hasTag() && itemTag == null) {
            return true;
        }

        return stack.hasTag() && stack.getTag().equals(itemTag);
    }


    public ItemStack createItemStack() {
        Item item = ForgeRegistries.ITEMS.getValue(itemName);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        stack.setCount(1);
        return stack;
    }

}
