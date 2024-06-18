package com.warmthdawn.mods.yaoyaocoin.gui;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class LayoutManager {


    private final ArrayList<CoinSlotGroup> groups = new ArrayList<>();
    private final Int2IntOpenHashMap slotIdToGroupIndex = new Int2IntOpenHashMap();

    private static final int SLOT_SIZE = 20;


    public LayoutManager() {


    }

    public void init() {
        groups.clear();
        slotIdToGroupIndex.clear();

        CoinSlotGroup defaultGroup = new CoinSlotGroup();
        groups.add(defaultGroup);

        ClientCoinStorage storage = ClientCoinStorage.INSTANCE;


        for (int i = 0; i < storage.getSlots().size(); i++) {
            CoinSlot slot = storage.getSlots().get(i);
            defaultGroup.addSlot(i, 0, slot, false);
            slotIdToGroupIndex.put(i, 0);
        }

        defaultGroup.setGroupX(50);
        defaultGroup.setGroupY(50);

        for (int i = 0; i < 3; i++) {
            slotIdToGroupIndex.put(i, 0);
        }
    }

    public CoinSlotGroup getGroup(int slotId) {
        int groupIndex = slotIdToGroupIndex.getOrDefault(slotId, -1);
        if (groupIndex == -1) {
            throw new IllegalArgumentException("Slot is not in any group");
        }

        return groups.get(groupIndex);
    }

    public List<CoinSlotGroup> getGroups() {
        return this.groups;
    }


}
