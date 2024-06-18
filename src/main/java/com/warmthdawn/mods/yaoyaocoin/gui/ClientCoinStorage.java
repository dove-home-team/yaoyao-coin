package com.warmthdawn.mods.yaoyaocoin.gui;

import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClientCoinStorage {

    public static final ClientCoinStorage INSTANCE = new ClientCoinStorage();


    private NonNullList<CoinSlot> slots = NonNullList.create();

    public List<CoinSlot> getSlots() {
        return slots;
    }


    public void init() {
        CoinManager manager = CoinManager.getInstance();

        slots = NonNullList.createWithCapacity(manager.getCoinTypeCount());

        for (int i = 0; i < manager.getCoinTypeCount(); i++) {
            CoinType type = manager.getCoinType(i);
            ItemStack stack = type.createItemStack();
            slots.add(new CoinSlot(i, stack));
        }

    }

    public void setSlotCount(String name, int count) {
        CoinManager manager = CoinManager.getInstance();
        CoinType type = manager.findCoinType(name);
        if (type == null) {
            return;
        }
        slots.get(type.id()).setCount(count);
    }

    public void resetSlotCount() {
        for (CoinSlot slot : slots) {
            slot.setCount(0);
        }
    }


}
