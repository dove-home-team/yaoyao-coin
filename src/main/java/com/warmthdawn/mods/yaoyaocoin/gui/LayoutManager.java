package com.warmthdawn.mods.yaoyaocoin.gui;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class LayoutManager {


    private final ArrayList<CoinSlotGroup> groups = new ArrayList<>();
    private final Int2IntOpenHashMap slotIdToGroupIndex = new Int2IntOpenHashMap();

    private static final int SLOT_SIZE = 20;


    public LayoutManager() {

        CoinSlotGroup defaultGroup = new CoinSlotGroup();
        groups.add(defaultGroup);



        defaultGroup.addSlot(0, 0, new CoinSlot(0), false);
        defaultGroup.addSlot(1, 0, new CoinSlot(1), false);
        defaultGroup.addSlot(2, 0, new CoinSlot(2), false);
        defaultGroup.addSlot(0, 1, new CoinSlot(3), false);
        defaultGroup.addSlot(1, 1, new CoinSlot(4), false);
        defaultGroup.addSlot(1, 2, new CoinSlot(5), false);

        defaultGroup.setGroupX(50);
        defaultGroup.setGroupY(50);

        for(int i = 0; i < 5; i++) {
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


    private CoinSlotGroup draggingGroup = null;
    private int draggingOffsetX = 0;
    private int draggingOffsetY = 0;


}
