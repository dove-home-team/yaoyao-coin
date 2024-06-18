package com.warmthdawn.mods.yaoyaocoin.gui;

import com.warmthdawn.mods.yaoyaocoin.capability.CoinCapability;
import com.warmthdawn.mods.yaoyaocoin.capability.CoinInventoryCapability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;

public class CoinSlot {


    private int slotId;
    private int count;
    private ItemStack stack;

    public CoinSlot(int slotId, ItemStack stack) {
        this.slotId = slotId;
        this.stack = stack;
        this.count = 0;
    }

    public int getId() {
        return slotId;
    }


    public int getCount() {
        return count;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
