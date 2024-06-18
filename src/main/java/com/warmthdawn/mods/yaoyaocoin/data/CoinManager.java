package com.warmthdawn.mods.yaoyaocoin.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CoinManager {

    private final List<CoinType> coinTypes = new ArrayList<>();

    private static CoinManager instance;

    public static CoinManager getInstance() {
        if (instance == null) {
            instance = new CoinManager();
        }
        return instance;
    }


    public void init() {

        this.coinTypes.add(new CoinType(0, "iron", 1, 100, 99, new ResourceLocation("minecraft", "iron_ingot"), null));
        this.coinTypes.add(new CoinType(1, "gold", 100, 100, 99, new ResourceLocation("minecraft", "gold_ingot"), null));
        this.coinTypes.add(new CoinType(1, "diamond", 1000, 100, 10, new ResourceLocation("minecraft", "diamond"), null));
    }

    public List<ItemStack> createInventoryStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (CoinType coin : coinTypes) {
            ItemStack stack = coin.createItemStack();
            stack.setCount(0);
            stacks.add(stack);
        }
        return stacks;
    }

    public CoinType findCoinType(String name) {
        for (CoinType type : coinTypes) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }


    public CoinType getCoinType(int id) {
        if (id < 0 || id >= coinTypes.size()) {
            return null;
        }
        return coinTypes.get(id);
    }

    public int getCoinTypeCount() {
        return coinTypes.size();
    }


}
