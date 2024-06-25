package com.warmthdawn.mods.yaoyaocoin.gui;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public interface SlotConsumer {
        void accept(int x, int y, CoinSlot slot, boolean borrowed);
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
    private List<Rect2i> collisionRects = new ArrayList<>();
    private boolean updating = false;

    public List<Rect2i> getCollisionRects() {
        return collisionRects;
    }


    private void rebuildSlotGrid() {
        int maxGridX = 0;
        int maxGridY = 0;

        for (Entry entry : slots) {
            if (entry.isBorrowed) {
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

    private void computeCollisionRects() {
        collisionRects.clear();
        boolean[] visited = new boolean[slots.size()];

        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                Entry entry = getSlotAt(j, i);
                if (entry == null || entry.isBorrowed || visited[slotIdToIndex.get(entry.slotId)]) {
                    continue;
                }

                int minX = j;
                int minY = i;
                int maxX = j;
                int maxY = i;

                for (int k = 0; k < gridHeight; k++) {
                    for (int l = 0; l < gridWidth; l++) {
                        Entry other = getSlotAt(l, k);
                        if (other == null || other.isBorrowed || other.slotId != entry.slotId) {
                            continue;
                        }

                        minX = Math.min(minX, l);
                        minY = Math.min(minY, k);
                        maxX = Math.max(maxX, l);
                        maxY = Math.max(maxY, k);
                        visited[slotIdToIndex.get(other.slotId)] = true;
                    }
                }

                collisionRects.add(new Rect2i(minX, minY, maxX - minX + 1, maxY - minY + 1));

            }
        }
    }

    public void addSlot(int gridX, int gridY, CoinSlot slot, boolean borrowed) {
        slots.add(new Entry(gridX, gridY, slot.getId(), borrowed));
        if (updating) {
            rebuildSlotGrid();

            if (!borrowed) {
                computeCollisionRects();
            }
        }
    }

    public void beginUpdate() {
        updating = true;
    }

    public void endUpdate() {
        updating = false;
        rebuildSlotGrid();
        computeCollisionRects();
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

    public void iterateSlots(SlotConsumer consumer) {
        ClientCoinStorage storage = ClientCoinStorage.INSTANCE;
        for (Entry entry : slots) {
            CoinSlot slot = storage.getSlots().get(entry.slotId);
            consumer.accept(entry.girdX, entry.gridY, slot, entry.isBorrowed);
        }
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
