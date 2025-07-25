package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.SlotKind;
import com.warmthdawn.mods.yaoyaocoin.misc.Block;
import com.warmthdawn.mods.yaoyaocoin.misc.GroupCollision;
import com.warmthdawn.mods.yaoyaocoin.misc.Rectangle2i;
import com.warmthdawn.mods.yaoyaocoin.misc.UnionFind;
import com.warmthdawn.mods.yaoyaocoin.misc.Vector2i;
import it.unimi.dsi.fastutil.ints.*;
import org.slf4j.Logger;

import java.util.*;

public class CoinSlotGroup {
    private static final Logger logger = LogUtils.getLogger();
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_BORDER_SIZE = 4;

    public boolean isDiscard() {
        return discard;
    }

    public boolean empty() {
        return slots.isEmpty();
    }

    public int slotCount() {
        return slots.size();
    }

    public enum Neighbour {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0),

        UP_LEFT(-1, -1),
        UP_RIGHT(1, -1),
        DOWN_LEFT(-1, 1),
        DOWN_RIGHT(1, 1);

        public final int offsetX;
        public final int offsetY;

        Neighbour(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }


    public interface SlotConsumer {
        void accept(int x, int y, CoinSlot slot, boolean borrowed);
    }

    public interface SlotPredicate {
        boolean test(int x, int y, CoinSlot slot, boolean borrowed);
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
    private final List<Rectangle2i> collisionRects = new ArrayList<>();
    private GroupCollision groupCollision = null;
    private boolean updating = false;
    private boolean discard = false;

    public List<Rectangle2i> getCollisionRects() {
        return collisionRects;
    }

    public GroupCollision getCollision() {
        if (groupCollision == null) {
            groupCollision = GroupCollision.compute(this, SLOT_SIZE);
        }
        return groupCollision;
    }

    private boolean needsRebuild = false;

    private void setSlotGrid(int gridX, int gridY, int index) {
        if (gridX < -1 || gridX > gridWidth || gridY < -1 || gridY > gridHeight) {
            logger.error("Setting slot out of bounds! this is a bug!");
            return;
        }

        if (index < 0 || index >= slots.size()) {
            logger.error("slot index out of bounds! this is a bug!");
            return;
        }

        Entry entry = slots.get(index);
        if (entry == null) {
            logger.error("Setting null slot! this is a bug!");
            return;
        }
        if (!entry.isBorrowed) {
            if (gridX < 0 || gridY < 0 || gridX >= gridWidth || gridY >= gridHeight) {
                logger.error("Setting non-borrowed slot out of bounds! this is a bug!");
                return;
            }
        }

        if (entry.girdX != gridX || entry.gridY != gridY) {
            logger.error("Setting slot to the wrong position! this is a bug!");
            return;
        }


        slotGrid[(gridX + 1) + (gridY + 1) * (gridWidth + 2)] = index;
    }

    private int getSlotGrid(int gridX, int gridY) {
        int index = (gridX + 1) + (gridY + 1) * (gridWidth + 2);
        if (index < 0 || index >= slotGrid.length) {
            return -1;
        }
        return slotGrid[index];
    }


    private void rebuildSlotGrid() {
        if (slots.isEmpty() || slots.stream().allMatch(Entry::isBorrowed)) {
            this.discardGroup();
            return;
        }
        int maxGridX = Integer.MIN_VALUE;
        int maxGridY = Integer.MIN_VALUE;
        int minGridX = Integer.MAX_VALUE;
        int minGridY = Integer.MAX_VALUE;

        for (Entry entry : slots) {
            if (entry.isBorrowed) {
                continue;
            }
            maxGridX = Math.max(maxGridX, entry.girdX);
            maxGridY = Math.max(maxGridY, entry.gridY);
            minGridX = Math.min(minGridX, entry.girdX);
            minGridY = Math.min(minGridY, entry.gridY);
        }

        gridWidth = maxGridX - minGridX + 1;
        gridHeight = maxGridY - minGridY + 1;

        slotGrid = new int[(gridHeight + 2) * (gridWidth + 2)];

        Arrays.fill(slotGrid, -1);

        List<Entry> invalidBorrowed = new ArrayList<>();

        for (int i = 0; i < slots.size(); i++) {
            Entry entry = slots.get(i);

            if (minGridX != 0 || minGridY != 0) {
                entry = new Entry(entry.girdX - minGridX, entry.gridY - minGridY, entry.slotId, entry.isBorrowed);
                slots.set(i, entry);
            }

            if (entry.isBorrowed) {
                if (entry.girdX < -1 || entry.girdX > gridWidth || entry.gridY < -1 || entry.gridY > gridHeight) {
                    invalidBorrowed.add(entry);
                    logger.info("removing invalid borrowed slot since it is out of bounds");
                    continue;
                }

            } else if (entry.girdX < 0 || entry.girdX >= gridWidth || entry.gridY < 0 || entry.gridY >= gridHeight) {
                logger.error("Rebuilding slot group finds out of bounds slot! this is a bug!");
                continue;
            }

            setSlotGrid(entry.girdX, entry.gridY, i);
        }

        if (minGridX != 0 || minGridY != 0) {
            groupX += minGridX * 20;
            groupY += minGridY * 20;
        }

        for (Entry entry : invalidBorrowed) {
            slots.remove(entry);
        }

        rebuildSlotIndex();
        needsRebuild = false;
        groupCollision = null;
    }

    private void rebuildSlotIndex() {
        slotIdToIndex.clear();
        for (int i = 0; i < slots.size(); i++) {
            slotIdToIndex.put(slots.get(i).slotId, i);
        }
    }

    public Block createAdsorptionBlock(int gridSize, Vector2i offset) {
        boolean[][] matrix = new boolean[gridWidth][gridHeight];
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                matrix[j][i] = hasSlot(j, i);
            }
        }
        return new Block(matrix, (int) Math.floor(1.0 * offset.getX() / gridSize), (int) Math.floor(1.0 * offset.getY() / gridSize));
    }

    public Vector2i getOffset() {
        return new Vector2i(groupX, groupY);
    }


    public void computeCollisionRects() {
        if (isDiscard()) {
            return;
        }
        collisionRects.clear();
        // R === gridHeight === Y
        // C === gridWidth === X
        boolean[][] used = new boolean[gridHeight][gridWidth];

        for (int y = 0; y < gridHeight; ++y) {
            for (int x = 0; x < gridWidth; ) {
                if (!hasSlot(x, y) || used[y][x]) {
                    x++;
                    continue;
                }

                // 找出当前行从 c 开始的连续 true
                int x1 = x;
                while (x1 < gridWidth && hasSlot(x1, y)) x1++;

                // 向下扩展最大高度
                int maxHeight = 1;
                outer:
                for (int h = 1; y + h < gridHeight; ++h) {
                    for (int cc = x; cc < x1; ++cc) {
                        if (!hasSlot(cc, y + h)) {
                            break outer;
                        }
                    }
                    maxHeight++;
                }

                // 添加矩形并标记
                Rectangle2i rect = new Rectangle2i(x, y, x1 - x, maxHeight);
                collisionRects.add(rect);
                for (int rr = y; rr < y + maxHeight; ++rr)
                    for (int cc = x; cc < x1; ++cc)
                        used[rr][cc] = true;

                x = x1;
            }
        }

        groupCollision = null;

    }

    public void addSlot(int gridX, int gridY, CoinSlot slot, boolean borrowed) {
        slots.add(new Entry(gridX, gridY, slot.getId(), borrowed));
        if (!updating) {
            rebuildSlotGrid();
            computeCollisionRects();
        } else {
            needsRebuild = true;
        }
    }

    private void addSlotInternalUnsafe(int gridX, int gridY, CoinSlot slot, boolean borrowed) {
        if (gridX < -1 || gridX > gridWidth || gridY < -1 || gridY > gridHeight) {
            logger.error("Adding slot out of bounds! this is a bug!");
            return;
        }
        int newIndex = slots.size();
        slots.add(new Entry(gridX, gridY, slot.getId(), borrowed));
        setSlotGrid(gridX, gridY, newIndex);
        slotIdToIndex.put(slot.getId(), newIndex);
    }

    public void discardGroup() {
        discard = true;
    }

    public void removeSlot(int slotId) {
        if (discard) {
            return;
        }
        int index = slotIdToIndex.getOrDefault(slotId, -1);
        if (index < 0) {
            return;
        }
        slots.remove(index);
        if (!updating) {
            rebuildSlotGrid();
            computeCollisionRects();
        } else {
            rebuildSlotIndex();
            needsRebuild = true;
        }
    }

    public boolean isSingle() {
        return slots.size() == 1;
    }

    public void clearBorrowedSlots() {
        slots.removeIf(Entry::isBorrowed);
        if (!updating) {
            rebuildSlotGrid();
            computeCollisionRects();
        } else {
            needsRebuild = true;
        }
    }

    public boolean borrowSlots(CoinSlotGroup groupB, int slotSize) {
        int xOff = groupB.groupX - groupX;
        int yOff = groupB.groupY - groupY;
        xOff = (int) Math.floor(1.0 * xOff / slotSize);
        yOff = (int) Math.floor(1.0 * yOff / slotSize);

        boolean modified = false;
        for (Entry entry : groupB.slots) {
            if (entry.isBorrowed) {
                continue;
            }
            int slotX = entry.girdX + xOff;
            int slotY = entry.gridY + yOff;

            if (slotX < -1 || slotX > gridWidth || slotY < -1 || slotY > gridHeight) {
                continue;
            }

            if (hasSlot(slotX, slotY)) {
                continue;
            }

            if (!isAdsorptionSlot(slotX, slotY)) {
                continue;
            }

            this.addSlotInternalUnsafe(slotX, slotY, ClientCoinStorage.INSTANCE.getSlots().get(entry.slotId), true);
            modified = true;
        }

        return modified;

    }

    public void takeSlot(int slotId, CoinSlotGroup other) {
        int index = other.slotIdToIndex.getOrDefault(slotId, -1);
        if (index < 0) {
            return;
        }
        Entry entry = other.slots.get(index);
        this.slots.add(entry);

        other.removeSlot(slotId);

        if (!updating) {
            rebuildSlotGrid();
            computeCollisionRects();
        } else {
            needsRebuild = true;
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
        int index = getSlotGrid(gridX, gridY);
        if (index == -1) {
            return -1;
        }
        return slots.get(index).slotId;
    }

    public boolean hasSlot(int gridX, int gridY) {
        return hasSlot(gridX, gridY, true);
    }

    public boolean hasSlot(int gridX, int gridY, boolean ignoreBorrowed) {
        Entry entry = getSlotAt(gridX, gridY);

        if (entry == null) {
            return false;
        }
        if (ignoreBorrowed) {
            return !entry.isBorrowed;
        }

        return true;
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
        this.groupCollision = null;
    }

    public void setGroupY(int groupY) {
        this.groupY = groupY;
        this.groupCollision = null;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public int findSlot(SlotPredicate predicate) {
        for (Entry entry : slots) {
            CoinSlot slot = ClientCoinStorage.INSTANCE.getSlots().get(entry.slotId);
            if (predicate.test(entry.girdX, entry.gridY, slot, entry.isBorrowed)) {
                return entry.slotId;
            }
        }
        return -1;
    }

    private Entry getSlotAt(int gridX, int gridY) {
        if (needsRebuild) {
            logger.error("Accessing slot group without rebuilding slot grid! this is a bug!");
            rebuildSlotGrid();
        }
        if (gridX < -1 || gridX > gridWidth || gridY < -1 || gridY > gridHeight) {
            return null;
        }
        int index = getSlotGrid(gridX, gridY);
        if (index == -1 || index >= slots.size()) {
            return null;
        }
        Entry entry = slots.get(index);

        if (gridX < 0 || gridY < 0 || gridX >= gridWidth || gridY >= gridHeight) {
            if (entry == null || entry.isBorrowed) {
                return entry;
            }

            logger.error("Accessing slot group out of bounds! this is a bug!");
            return null;
        }

        return entry;

    }

    public static boolean combineGroups(List<CoinSlotGroup> groups, List<CoinSlotGroup> additional, int slotSize) {

        HashMap<Vector2i, Integer> slotMap = new HashMap<>();
        CoinManager manager = CoinManager.getInstance();
        int[] slotGroupMap = new int[manager.getCoinTypeCount()];
        Arrays.fill(slotGroupMap, -1);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        for (int i = 0; i < groups.size(); i++) {
            CoinSlotGroup group = groups.get(i);
            if (group.isDiscard() || !group.isVisible()) {
                continue;
            }
            minX = Math.min(minX, group.groupX);
            minY = Math.min(minY, group.groupY);

            for (Entry entry : group.slots) {
                if (entry.isBorrowed) {
                    continue;
                }
                slotGroupMap[entry.slotId] = i;
            }
        }
        UnionFind unionFind = new UnionFind(groups.size());

        int xStart = Integer.MAX_VALUE;
        int yStart = Integer.MAX_VALUE;
        int xEnd = Integer.MIN_VALUE;
        int yEnd = Integer.MIN_VALUE;

        Int2ObjectOpenHashMap<IntList> invalidSlots = new Int2ObjectOpenHashMap<>();

//        for (CoinSlotGroup group : groups) {
        for (int i = 0; i < groups.size(); i++) {
            CoinSlotGroup group = groups.get(i);
            if (group.isDiscard() || !group.isVisible()) {
                continue;
            }
            int xOff = (group.groupX - minX) / slotSize;
            int yOff = (group.groupY - minY) / slotSize;

            assert ((group.groupX - minX) % slotSize == 0);
            assert ((group.groupY - minY) % slotSize == 0);


            for (Entry entry : group.slots) {
                if (entry.isBorrowed) {
                    continue;
                }

                Vector2i pos = new Vector2i(entry.girdX + xOff, entry.gridY + yOff);
                xStart = Math.min(xStart, pos.getX());
                yStart = Math.min(yStart, pos.getY());
                xEnd = Math.max(xEnd, pos.getX());
                yEnd = Math.max(yEnd, pos.getY());

                if (slotMap.containsKey(pos)) {
                    int slotId = slotMap.get(pos);
                    int groupId = slotGroupMap[slotId];
                    if (groupId == -1) {
                        continue;
                    }

                    invalidSlots.computeIfAbsent(slotGroupMap[entry.slotId], it -> new IntArrayList()).add(entry.slotId);


                } else {
                    slotMap.put(pos, entry.slotId);
                }
            }
        }

        boolean modified = false;
        if (!invalidSlots.isEmpty()) {
            modified = true;
            CoinSlotGroup invalidGroup = new CoinSlotGroup();
            invalidGroup.setGroupX(minX - slotSize - 10);
            invalidGroup.setGroupY(minY);

            invalidGroup.beginUpdate();

            for (Int2ObjectMap.Entry<IntList> entry : invalidSlots.int2ObjectEntrySet()) {
                int groupId = entry.getIntKey();
                CoinSlotGroup group = groups.get(groupId);
                group.beginUpdate();
                for (int i = 0; i < entry.getValue().size(); i++) {
                    int slotId = entry.getValue().getInt(i);
                    invalidGroup.takeSlot(slotId, group);
                }
                group.endUpdate();
            }
            invalidGroup.endUpdate();
            additional.add(invalidGroup);
        }


        // 在 2x2 的窗口内进行卷积，合并相邻的 slot

        int windowSize = 2;
        for (int i = yStart; i <= yEnd + 1; i++) {
            for (int j = xStart; j <= xEnd + 1; j++) {
                HashSet<Integer> groupSet = new HashSet<>();
                for (int k = 0; k < windowSize; k++) {
                    for (int l = 0; l < windowSize; l++) {
                        Vector2i pos = new Vector2i(j + l, i + k);
                        if (slotMap.containsKey(pos)) {
                            int slotId = slotMap.get(pos);
                            int groupId = slotGroupMap[slotId];
                            if (groupId != -1) {
                                groupSet.add(groupId);
                            }
                        }
                    }
                }

                int[] groupArray = groupSet.stream().mapToInt(Integer::intValue).toArray();

                for (int k = 0; k < groupArray.length; k++) {
                    for (int l = k + 1; l < groupArray.length; l++) {
                        unionFind.union(groupArray[k], groupArray[l]);
                    }
                }
            }
        }


        Int2ObjectOpenHashMap<List<CoinSlotGroup>> groupMap = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).isDiscard()) {
                continue;
            }
            int g = unionFind.find(i);
            groupMap.computeIfAbsent(g, it -> new ArrayList<>()).add(groups.get(i));
        }


        for (Int2ObjectMap.Entry<List<CoinSlotGroup>> entry : groupMap.int2ObjectEntrySet()) {
            List<CoinSlotGroup> list = entry.getValue();
            if (list.size() <= 1) {
                continue;
            }

            CoinSlotGroup first = list.get(0);

            if (first == null) {
                logger.error("slot group not found! this is a bug!");
                continue;
            }
            modified = true;

            first.beginUpdate();
            for (int i = 1; i < list.size(); i++) {
                CoinSlotGroup other = list.get(i);
                if (other == null || other == first) {
                    continue;
                }
                int xOff = (other.groupX - first.groupX) / slotSize;
                int yOff = (other.groupY - first.groupY) / slotSize;
                for (Entry otherEntry : other.slots) {
                    if (otherEntry.isBorrowed) {
                        continue;
                    }

                    int slotX = otherEntry.girdX + xOff;
                    int slotY = otherEntry.gridY + yOff;

                    Entry newEntry = new Entry(slotX, slotY, otherEntry.slotId, false);
                    int slotIndex = first.getSlotGrid(slotX, slotY);
                    if (slotIndex != -1) {
                        first.slots.set(slotIndex, newEntry);
                        continue;
                    }

                    first.slots.add(newEntry);
                }
                other.discardGroup();
            }
            first.endUpdate();

        }

        return modified;


    }

    private boolean visible = true;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean splitUnConnected(List<CoinSlotGroup> splitted) {
        UnionFind unionFind = new UnionFind(slots.size());
        final int windowSize = 2;
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                int[] slotsInRegion = collectSlots(j, windowSize, i);
                for (int k = 0; k < slotsInRegion.length; k++) {
                    for (int l = k + 1; l < slotsInRegion.length; l++) {
                        unionFind.union(slotsInRegion[k], slotsInRegion[l]);
                    }
                }
            }
        }

        Int2ObjectOpenHashMap<IntList> groupMap = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            int g = unionFind.find(i);
            groupMap.computeIfAbsent(g, it -> new IntArrayList()).add(i);
        }

        if (groupMap.size() <= 1) {
            return false;
        }
        this.discardGroup();
        for (IntList list : groupMap.values()) {
            CoinSlotGroup newGroup = new CoinSlotGroup();
            newGroup.groupX = this.groupX;
            newGroup.groupY = this.groupY;
            newGroup.beginUpdate();
            for (int i = 0; i < list.size(); i++) {
                int slotIndex = list.getInt(i);
                int slotId = slots.get(slotIndex).slotId;
                newGroup.takeSlot(slotId, this);
            }
            newGroup.endUpdate();

            splitted.add(newGroup);
        }

        return true;

    }

    private int[] collectSlots(int j, int windowSize, int i) {
        IntSet region = new IntOpenHashSet();
        // 3x3 的窗口内进行卷积，合并相邻的 slot
        int maxX = Math.min(gridWidth - j, windowSize);
        int maxY = Math.min(gridHeight - i, windowSize);
        for (int k = 0; k < maxY; k++) {
            for (int l = 0; l < maxX; l++) {
                int slotIndex = getSlotGrid(j + l, i + k);
                if (slotIndex == -1) {
                    continue;
                }

                Entry entry = slots.get(slotIndex);
                if (entry.isBorrowed) {
                    continue;
                }
                region.add(slotIndex);
            }
        }

        int[] regionArray = region.toIntArray();
        return regionArray;
    }


    private boolean isAdsorptionSlot(int slotX, int slotY) {
        Entry entry = getSlotAt(slotX, slotY);
        if (entry != null && !entry.isBorrowed) {
            return true;
        }

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }


                entry = getSlotAt(slotX + i, slotY + j);
                if (entry != null && !entry.isBorrowed) {
                    return true;
                }
            }
        }

        return false;


    }

    public SlotKind getSlotKind(int slotX, int slotY) {
        Entry entry = getSlotAt(slotX, slotY);
        if (entry == null) {
            return SlotKind.Empty;
        }
        if (entry.isBorrowed) {
            return SlotKind.Virtual;
        }
        return SlotKind.Real;
    }


}
