package com.warmthdawn.mods.yaoyaocoin.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public class RegistryCoinEventJS extends EventJS {
    @HideFromJS
    public LinkedList<CoinTypeBuilderJS> coinBuilders = new LinkedList<>();

    @Info(value = "Add an item as a coin", params = {
            @Param(name = "name", value = "The coin name"),
            @Param(name = "itemStack", value = "The coin item stack")
    })
    public CoinTypeBuilderJS addCoin(String name, ItemStack itemStack) {
        CoinTypeBuilderJS builder = new CoinTypeBuilderJS(name, itemStack);
        coinBuilders.add(builder);
        return builder;
    }
}
