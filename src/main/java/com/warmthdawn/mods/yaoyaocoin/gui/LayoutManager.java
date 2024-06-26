package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.config.CoinSaveState;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.misc.Block;
import com.warmthdawn.mods.yaoyaocoin.misc.Rectangle2i;
import com.warmthdawn.mods.yaoyaocoin.misc.Vector2i;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LayoutManager {


    private final ArrayList<CoinSlotGroup> groups = new ArrayList<>();
    private final Int2IntOpenHashMap slotIdToGroupIndex = new Int2IntOpenHashMap();

    private static final int SLOT_SIZE = 20;

    private final Logger logger = LogUtils.getLogger();


    public LayoutManager() {


    }

    void clear() {
        groups.clear();
        slotIdToGroupIndex.clear();
    }

    public void finishMovement() {
        CoinSaveState saveState = CoinSaveState.instance();


    }


    public void updateGroupPosition(AbstractContainerScreen<?> screen, CoinSlotGroup group, int x, int y) {
        int newX = x;
        int newY = y;
        // collisions to center rect

//        int screenX0 = screen.getGuiLeft();
//        int screenY0 = screen.getGuiTop();
//
//        int screenX1 = screenX0 + screen.getXSize();
//        int screenY1 = screenY0 + screen.getYSize();

        Rectangle2i screenRect = new Rectangle2i(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize());


        // 计算碰撞
        for (Rectangle2i rect : group.getCollisionRects()) {
            // check if collisionRects is inside screen rect

            Rectangle2i rectActual = rect.scaled(SLOT_SIZE);

            if (!screenRect.intersects(rectActual.translated(new Vector2i(newX, newY)))) {
                continue;
            }

            int xLeft = screenRect.getX() - rectActual.getWidth() - rectActual.getX();
            int xRight = screenRect.getX() + screenRect.getWidth() - rectActual.getX();

            int yTop = screenRect.getY() - rectActual.getHeight() - rectActual.getY();
            int yBottom = screenRect.getY() + screenRect.getHeight() - rectActual.getY();

            int stoppedX;
            if (Math.abs(newX - xLeft) < Math.abs(newX - xRight)) {
                stoppedX = xLeft;
            } else {
                stoppedX = xRight;
            }

            int stoppedY;
            if (Math.abs(newY - yTop) < Math.abs(newY - yBottom)) {
                stoppedY = yTop;
            } else {
                stoppedY = yBottom;
            }


            // 停在最近的边界
            if (Math.abs(newX - stoppedX) < Math.abs(newY - stoppedY)) {
                newX = stoppedX;
            } else {
                newY = stoppedY;
            }
        }


        // 计算吸附


        // 查找最近的吸附组


        List<Rectangle2i> collisionRects = new ArrayList<>(group.getCollisionRects().size());

        Vector2i newPos = new Vector2i(newX, newY);
        for (Rectangle2i rect : group.getCollisionRects()) {
            Rectangle2i actual = rect.scaled(SLOT_SIZE).translateInPlace(newPos);
            collisionRects.add(actual);
        }


        Vector2i nearestOffset = null;
        Vector2i finalNewPos = null;
        for (CoinSlotGroup otherGroup : this.groups) {
            if (otherGroup == group) {
                continue;
            }
            boolean willAdsorb = false;
            for (Rectangle2i adRect : otherGroup.getAdsorptionRects()) {
                Rectangle2i adRectActual = adRect.scaled(SLOT_SIZE).translateInPlace(new Vector2i(otherGroup.getGroupX(), otherGroup.getGroupY()));
                for (Rectangle2i currentCollision : collisionRects) {
                    if (adRectActual.intersects(currentCollision)) {
                        willAdsorb = true;
                        break;
                    }
                }
            }

            if (!willAdsorb) {
                continue;
            }
            Vector2i otherOffset = otherGroup.getOffset();

            Block otherBlock = otherGroup.createAdsorptionBlock(SLOT_SIZE, Vector2i.ZERO);
            Block currentBlock = group.createAdsorptionBlock(SLOT_SIZE, newPos.subtract(otherOffset));

            Vector2i offset = Block.moveBlocks(currentBlock, otherBlock);

            if (offset == null) {
                continue;
            }

            if (nearestOffset == null || offset.lengthManhattan() < nearestOffset.lengthManhattan()) {
                Vector2i pos = otherOffset.add(new Vector2i(currentBlock.getX(), currentBlock.getY()).scaleInPlace(SLOT_SIZE));

                boolean valid = true;
                for (Rectangle2i rect : group.getCollisionRects()) {
                    Rectangle2i actual = rect.scaled(SLOT_SIZE).translateInPlace(pos);

                    if (!screenRect.intersects(actual)) {
                        continue;
                    }
                    valid = false;

                }
                if (valid) {
                    nearestOffset = offset;
                    finalNewPos = pos;
                }

            }
        }

        if (nearestOffset != null) {
            newX = finalNewPos.getX();
            newY = finalNewPos.getY();
        }


        group.setGroupX(newX);
        group.setGroupY(newY);

        computeGroupOverlap(true);

    }


    private void computeGroupOverlap(boolean borrow) {
        HashMap<Vector2i, ArrayList<CoinSlotGroup>> groupMap = new HashMap<>();

        for (CoinSlotGroup group : groups) {
            Vector2i offset = group.getOffset();
            offset.setX((offset.getX() % SLOT_SIZE + SLOT_SIZE) % SLOT_SIZE);
            offset.setY((offset.getY() % SLOT_SIZE + SLOT_SIZE) % SLOT_SIZE);

            ArrayList<CoinSlotGroup> list = groupMap.computeIfAbsent(offset, k -> new ArrayList<>());
            list.add(group);
        }

        for (ArrayList<CoinSlotGroup> list : groupMap.values()) {
            if (list.isEmpty()) {
                continue;
            }
            if (borrow) {
                for (int i = 0; i < list.size(); i++) {
                    CoinSlotGroup groupA = list.get(i);
                    groupA.clearBorrowedSlots();
                    groupA.beginUpdate();
                    for (int j = 0; j < list.size(); j++) {
                        if (i == j) {
                            continue;
                        }
                        CoinSlotGroup groupB = list.get(j);
                        groupA.borrowSlots(groupB, SLOT_SIZE);
                    }
                    groupA.endUpdate();
                }
            }
        }
    }


    private void rebuildGroupIndex() {
        slotIdToGroupIndex.clear();
        for (int i = 0; i < groups.size(); i++) {
            CoinSlotGroup group = groups.get(i);
            final int groupIndex = i;
            group.iterateSlots((slotX, slotY, slot, borrowed) -> {
                if (!borrowed) {
                    slotIdToGroupIndex.put(slot.getId(), groupIndex);
                }
            });
        }
    }

    public CoinSlotGroup takeSlot(int slotId, CoinSlotGroup group) {
        if (group.isSingle()) {
            return group;
        }
        CoinSlotGroup newGroup = new CoinSlotGroup();
        newGroup.setGroupX(group.getGroupX());
        newGroup.setGroupY(group.getGroupY());
        newGroup.takeSlot(slotId, group);
        groups.add(newGroup);
        splitGroup(group);
        rebuildGroupIndex();
        return newGroup;
    }

    private void splitGroup(CoinSlotGroup group) {

        ArrayList<CoinSlotGroup> splitted = new ArrayList<>();
        boolean doSplit = group.splitUnConnected(splitted);

        if (!doSplit) {
            return;
        }

        groups.remove(group);
        groups.addAll(splitted);
    }

    public void init(AbstractContainerScreen<?> screen) {
        clear();

        CoinSaveState saveState = CoinSaveState.instance();
        saveState.load();

        ClientCoinStorage storage = ClientCoinStorage.INSTANCE;
        CoinManager manager = CoinManager.getInstance();

        for (int i = 0; i < saveState.getGroups().size(); i++) {
            CoinSaveState.Group group = saveState.getGroups().get(i);
            CoinSlotGroup slotGroup = new CoinSlotGroup();
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            slotGroup.beginUpdate();
            for (CoinSaveState.Slot slot : saveState.getSlots()) {
                if (slot.groupId != group.id) {
                    continue;
                }

                int slotId = manager.findCoinType(slot.name).id();
                CoinSlot clientSlot = storage.getSlots().get(slotId);
                slotGroup.addSlot(slot.x, slot.y, clientSlot, false);

                maxX = Math.max(maxX, slot.x);
                maxY = Math.max(maxY, slot.y);
                minX = Math.min(minX, slot.x);
                minY = Math.min(minY, slot.y);
            }
            slotGroup.endUpdate();

            // x
            switch (group.area) {
                case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> {
                    int x0 = screen.getGuiLeft();
                    int groupOffset = (maxX + 1) * SLOT_SIZE;
                    slotGroup.setGroupX(x0 - groupOffset + group.horizontal);

                }

                case TOP_CENTER, BOTTOM_CENTER -> {
                    int x0 = screen.getGuiLeft() + screen.getXSize() / 2;
                    int groupOffset = (maxX + minX + 1) * SLOT_SIZE / 2;
                    slotGroup.setGroupX(x0 - groupOffset + group.horizontal);
                }

                case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> {
                    int x0 = screen.getGuiLeft() + screen.getXSize();
                    int groupOffset = (minX - 1) * SLOT_SIZE;
                    slotGroup.setGroupX(x0 + groupOffset + group.horizontal);
                }
            }

            // y
            switch (group.area) {
                case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> {
                    int y0 = screen.getGuiTop();
                    int groupOffset = (maxY + 1) * SLOT_SIZE;
                    slotGroup.setGroupY(y0 - groupOffset + group.vertical);
                }

                case CENTER_LEFT, CENTER_RIGHT -> {
                    int y0 = screen.getGuiTop() + screen.getYSize() / 2;
                    int groupOffset = (maxY + minY + 1) * SLOT_SIZE / 2;
                    slotGroup.setGroupY(y0 - groupOffset + group.vertical);
                }

                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> {
                    int y0 = screen.getGuiTop() + screen.getYSize();
                    int groupOffset = (minY - 1) * SLOT_SIZE;
                    slotGroup.setGroupY(y0 + groupOffset + group.vertical);
                }
            }

            groups.add(slotGroup);
        }

        rebuildGroupIndex();
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
