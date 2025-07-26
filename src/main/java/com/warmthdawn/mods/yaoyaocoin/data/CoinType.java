package com.warmthdawn.mods.yaoyaocoin.data;

import com.warmthdawn.mods.yaoyaocoin.config.LayoutArea;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public record CoinType(
        int id,
        String name,
        int money,
        int convertGroup,
        int maxStackSize,
        boolean hiddenDefault,
        ItemStack itemStack,
        LayoutArea defaultArea
) {

    public boolean matches(ItemStack stack) {
        return ItemStack.isSameItemSameTags(stack, itemStack);
    }

}
