package com.warmthdawn.mods.yaoyaocoin.data;

import com.warmthdawn.mods.yaoyaocoin.config.CoinLayoutArea;
import net.minecraft.world.item.ItemStack;

public record CoinType(
        int id,
        String name,
        int money,
        int convertGroup,
        int maxStackSize,
        boolean hiddenDefault,
        ItemStack itemStack,
        CoinLayoutArea defaultArea
) {

    public boolean matches(ItemStack stack) {
        return ItemStack.isSameItemSameTags(stack, itemStack);
    }

}
