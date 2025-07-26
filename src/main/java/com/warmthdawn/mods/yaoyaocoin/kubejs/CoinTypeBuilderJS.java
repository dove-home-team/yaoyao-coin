package com.warmthdawn.mods.yaoyaocoin.kubejs;

import com.warmthdawn.mods.yaoyaocoin.config.LayoutArea;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.world.item.ItemStack;

public class CoinTypeBuilderJS {
    private final String name;
    private int money = -1;
    private int convertGroup = -1;
    private int maxStackSize = -1;
    private boolean hideDefault = false;
    private final ItemStack itemStack;
    private LayoutArea defaultLayoutArea = LayoutArea.TOP_LEFT;

    public CoinTypeBuilderJS(String name, ItemStack itemStack) {
        this.name = name;
        this.itemStack = itemStack;
    }

    @Info("Set the money value of the coin slot (default: 1)")
    public CoinTypeBuilderJS money(int money) {
        this.money = money;
        return this;
    }

    @Info("Set the convert group of the coin slot (default: 0)")
    public CoinTypeBuilderJS group(int convertGroup) {
        this.convertGroup = convertGroup;
        return this;
    }

    @Info("Set the max stack size of the coin slot (default: item stack max stack size)")
    public CoinTypeBuilderJS maxSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    @Info("Hide the coin slot by default")
    public CoinTypeBuilderJS hideDefault() {
        this.hideDefault = true;
        return this;
    }

    @Info("Set the coin default layout area")
    public CoinTypeBuilderJS defaultArea(LayoutArea area) {
        if (area == null) {
            return this;
        }
        this.defaultLayoutArea = area;
        return this;
    }

    @HideFromJS
    public CoinType build(int id) {
        if (money < 0) {
            money = 1;
        }
        if (convertGroup < 0) {
            convertGroup = 0;
        }
        if (maxStackSize < 0) {
            maxStackSize = itemStack.getMaxStackSize();
        }

        return new CoinType(id, name, money, convertGroup, maxStackSize, hideDefault, itemStack, defaultLayoutArea);
    }

}
