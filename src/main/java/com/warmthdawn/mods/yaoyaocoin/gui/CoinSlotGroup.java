package com.warmthdawn.mods.yaoyaocoin.gui;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;

public class CoinSlotGroup {


    public enum Neighbour {
        UP,
        DOWN,
        LEFT,
        RIGHT,

        UP_LEFT,
        UP_RIGHT,
        DOWN_LEFT,
        DOWN_RIGHT,
    }


    public enum NeighborKind {
        Slot,
        Empty
    }

    private final ArrayList<Entry> slots = new ArrayList<>();

    private final Int2IntOpenHashMap slotIdToIndex = new Int2IntOpenHashMap();


    private int[] slotGrid;
    private int gridWidth = 0;
    private int gridHeight = 0;

    private record Entry(int girdX, int gridY, int slotId, boolean isBorrowed) {
    }


    private int groupX = 0;
    private int groupY = 0;



    private void rebuildSlotGrid() {
        int maxGridX = 0;
        int maxGridY = 0;

        for (Entry entry : slots) {
            if(entry.isBorrowed) {
                continue;
            }
            maxGridX = Math.max(maxGridX, entry.girdX);
            maxGridY = Math.max(maxGridY, entry.gridY);
        }

        gridWidth = maxGridX + 1;
        gridHeight = maxGridY + 1;

        slotGrid = new int[gridHeight * gridWidth];

        Arrays.fill(slotGrid, -1);

        for (int i = 0; i < slots.size(); i++) {
            Entry entry = slots.get(i);
            slotGrid[entry.girdX + entry.gridY * gridWidth] = i;
        }

        slotIdToIndex.clear();
        for (int i = 0; i < slots.size(); i++) {
            slotIdToIndex.put(slots.get(i).slotId, i);
        }

    }

    public void addSlot(int gridX, int gridY, CoinSlot slot, boolean borrowed) {
        slots.add(new Entry(gridX, gridY, slot.getId(), borrowed));
        rebuildSlotGrid();
    }


    public int getSlotId(int gridX, int gridY) {
        if (gridX < 0 || gridX >= gridWidth || gridY < 0 || gridY >= gridHeight) {
            return -1;
        }
        int index = slotGrid[gridX + gridY * gridWidth];
        if (index == -1) {
            return -1;
        }
        return slots.get(index).slotId;
    }

    public boolean hasSlot(int gridX, int gridY) {
        Entry entry = getSlotAt(gridX, gridY);

        if (entry == null) {
            return false;
        }

        return !entry.isBorrowed;
    }


    public int getSlotX(int slotId) {
        int slotIndex = slotIdToIndex.getOrDefault(slotId, -1);
        if (slotIndex < 0) {
            return -1;
        }
        return slots.get(slotIndex).girdX;
    }

    public int getSlotY(int slotId) {
        int slotIndex = slotIdToIndex.getOrDefault(slotId, -1);
        if (slotIndex < 0) {
            return -1;
        }
        return slots.get(slotIndex).gridY;
    }

    public int getGroupX() {
        return groupX;
    }

    public int getGroupY() {
        return groupY;
    }

    public void setGroupX(int groupX) {
        this.groupX = groupX;
    }

    public void setGroupY(int groupY) {
        this.groupY = groupY;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    private Entry getSlotAt(int gridX, int gridY) {
        if (gridX < 0 || gridX >= gridWidth || gridY < 0 || gridY >= gridHeight) {
            return null;
        }
        int index = slotGrid[gridX + gridY * gridWidth];
        if (index == -1) {
            return null;
        }
        return slots.get(index);
    }

    public NeighborKind getNeighbourKind(int slotX, int slotY, Neighbour neighbour) {
        Entry entry = switch (neighbour) {
            case UP -> this.getSlotAt(slotX, slotY - 1);
            case DOWN -> this.getSlotAt(slotX, slotY + 1);
            case LEFT -> this.getSlotAt(slotX - 1, slotY);
            case RIGHT -> this.getSlotAt(slotX + 1, slotY);
            case UP_LEFT -> this.getSlotAt(slotX - 1, slotY - 1);
            case UP_RIGHT -> this.getSlotAt(slotX + 1, slotY - 1);
            case DOWN_LEFT -> this.getSlotAt(slotX - 1, slotY + 1);
            case DOWN_RIGHT -> this.getSlotAt(slotX + 1, slotY + 1);
        };

        if (entry == null) {
            return NeighborKind.Empty;
        }

        return NeighborKind.Slot;
    }


}
