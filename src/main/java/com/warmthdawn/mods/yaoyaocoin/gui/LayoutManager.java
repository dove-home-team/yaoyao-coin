package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.config.CoinDefine;
import com.warmthdawn.mods.yaoyaocoin.config.CoinSaveState;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
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

    public void finishMovement(AbstractContainerScreen<?> screen) {
        computeGroupOverlap(false);
        CoinSaveState saveState = CoinSaveState.instance();
        CoinManager manager = CoinManager.getInstance();

        Rectangle2i screenRect = new Rectangle2i(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize());

        saveState.clear();
        int id = 0;
        for (CoinSlotGroup group : groups) {
            CoinSaveState.Group saveGroup = new CoinSaveState.Group();
            int groupId = id++;
            saveGroup.id = groupId;

            group.iterateSlots((slotX, slotY, slot, borrowed) -> {
                if (borrowed) {
                    return;
                }

                CoinSaveState.Slot saveSlot = new CoinSaveState.Slot();
                saveSlot.x = slotX;
                saveSlot.y = slotY;
                saveSlot.name = manager.getCoinType(slot.getId()).name();
                saveSlot.groupId = groupId;

                saveState.addSlot(saveSlot);

            });
            Rectangle2i groupRect = new Rectangle2i(group.getGroupX(), group.getGroupY(), group.getGridWidth() * SLOT_SIZE, group.getGridHeight() * SLOT_SIZE);

            // top left
            if (groupRect.getX1() < screenRect.getX() && groupRect.getY1() < screenRect.getY()) {
                saveGroup.area = CoinSaveState.LayoutArea.TOP_LEFT;
                // relative to top left
                saveGroup.horizontal = groupRect.getX() - screenRect.getX();
                saveGroup.vertical = groupRect.getY() - screenRect.getY();
            }
            // top center
            else if (groupRect.getX() >= screenRect.getX() && groupRect.getX1() <= screenRect.getX1() && groupRect.getY1() < screenRect.getY()) {
                // relative to top left
                saveGroup.area = CoinSaveState.LayoutArea.TOP_CENTER;
                saveGroup.horizontal = groupRect.getX() - screenRect.getX();
                saveGroup.vertical = groupRect.getY() - screenRect.getY();
            }
            // top right
            else if (groupRect.getX() > screenRect.getX1() && groupRect.getY1() < screenRect.getY()) {
                saveGroup.area = CoinSaveState.LayoutArea.TOP_RIGHT;
                // relative to top right
                saveGroup.horizontal = groupRect.getX() - screenRect.getX1();
                saveGroup.vertical = groupRect.getY() - screenRect.getY();
            }
            // center right
            else if (groupRect.getX() > screenRect.getX1() && groupRect.getY() >= screenRect.getY() && groupRect.getY1() <= screenRect.getY1()) {
                saveGroup.area = CoinSaveState.LayoutArea.CENTER_RIGHT;
                // relative to top right
                saveGroup.horizontal = groupRect.getX() - screenRect.getX1();
                saveGroup.vertical = groupRect.getY() - screenRect.getY();
            }
            // bottom right
            else if (groupRect.getX() > screenRect.getX1() && groupRect.getY() > screenRect.getY1()) {
                saveGroup.area = CoinSaveState.LayoutArea.BOTTOM_RIGHT;
                // relative to bottom right
                saveGroup.horizontal = groupRect.getX() - screenRect.getX1();
                saveGroup.vertical = groupRect.getY() - screenRect.getY1();
            }
            // bottom center
            else if (groupRect.getX() >= screenRect.getX() && groupRect.getX1() <= screenRect.getX1() && groupRect.getY() > screenRect.getY1()) {
                saveGroup.area = CoinSaveState.LayoutArea.BOTTOM_CENTER;
                // relative to bottom left
                saveGroup.horizontal = groupRect.getX() - screenRect.getX();
                saveGroup.vertical = groupRect.getY() - screenRect.getY1();
            }
            // bottom left
            else if (groupRect.getX() < screenRect.getX() && groupRect.getY() > screenRect.getY1()) {
                saveGroup.area = CoinSaveState.LayoutArea.BOTTOM_LEFT;
                // relative to bottom left
                saveGroup.horizontal = groupRect.getX() - screenRect.getX();
                saveGroup.vertical = groupRect.getY() - screenRect.getY1();
            }
            // center left
            else {
                saveGroup.area = CoinSaveState.LayoutArea.CENTER_LEFT;
                // relative to top left
                saveGroup.horizontal = groupRect.getX() - screenRect.getX();
                saveGroup.vertical = groupRect.getY() - screenRect.getY();
            }
            saveState.addGroup(saveGroup);
        }

        saveState.save();
    }


    public void updateGroupPosition(AbstractContainerScreen<?> screen, CoinSlotGroup group, int x, int y) {
        int newX = x;
        int newY = y;
        // collisions to center rect


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


        if (group.getGroupX() == newX && group.getGroupY() == newY) {
            return;
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

            ArrayList<CoinSlotGroup> set = groupMap.computeIfAbsent(offset, k -> new ArrayList<>());
            set.add(group);
        }

        boolean changed = false;
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
            } else {
                boolean modified = CoinSlotGroup.combineGroups(list, SLOT_SIZE);
                if (modified) {
                    changed = true;
                    for (CoinSlotGroup group : list) {
                        if (group.isDiscard()) {
                            groups.remove(group);
                        }
                    }
                }
            }
        }

        if (changed) {
            rebuildGroupIndex();
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

        computeGroupOverlap(true);
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

        boolean[] used = new boolean[manager.getCoinTypeCount()];

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
                CoinType type = manager.findCoinType(slot.name);
                if (type == null) {
                    logger.warn("Unknown coin type {}", slot.name);
                    continue;
                }

                int slotId = type.id();
                if (used[slotId]) {
                    logger.warn("Slot {} is already used", slotId);
                    continue;
                }
                CoinSlot clientSlot = storage.getSlots().get(slotId);
                slotGroup.addSlot(slot.x, slot.y, clientSlot, false);
                used[slotId] = true;

                maxX = Math.max(maxX, slot.x);
                maxY = Math.max(maxY, slot.y);
                minX = Math.min(minX, slot.x);
                minY = Math.min(minY, slot.y);
            }
            slotGroup.endUpdate();

            if (slotGroup.empty()) {
                continue;
            }

            // x
            switch (group.area) {
                case TOP_LEFT, TOP_CENTER , CENTER_LEFT -> {
                    int x0 = screen.getGuiLeft();
                    int y0 = screen.getGuiTop();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }

                case TOP_RIGHT , CENTER_RIGHT -> {
                    int x0 = screen.getGuiLeft() + screen.getXSize();
                    int y0 = screen.getGuiTop();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }

                case BOTTOM_LEFT , BOTTOM_CENTER ->  {
                    int x0 = screen.getGuiLeft();
                    int y0 = screen.getGuiTop() + screen.getYSize();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }

                case BOTTOM_RIGHT -> {
                    int x0 = screen.getGuiLeft() + screen.getXSize();
                    int y0 = screen.getGuiTop() + screen.getYSize();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }
            }

            // make sure the group is inside the screen

            if (slotGroup.getGroupX() <= 0) {
                slotGroup.setGroupX(0);
            }
            if (slotGroup.getGroupY() <= 0) {
                slotGroup.setGroupY(0);
            }

            if (slotGroup.getGroupX() + slotGroup.getGridWidth() * SLOT_SIZE >= screen.width) {
                slotGroup.setGroupX(screen.width - slotGroup.getGridWidth() * SLOT_SIZE);
            }

            if (slotGroup.getGroupY() + slotGroup.getGridHeight() * SLOT_SIZE >= screen.height) {
                slotGroup.setGroupY(screen.height - slotGroup.getGridHeight() * SLOT_SIZE);
            }

            groups.add(slotGroup);
        }

        boolean shouldAddExtra = false;
        CoinSlotGroup extra = new CoinSlotGroup();
        extra.beginUpdate();
        extra.setGroupX(10);
        extra.setGroupY(10);

        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                CoinSlot clientSlot = storage.getSlots().get(i);
                extra.addSlot(0, 0, clientSlot, false);
                shouldAddExtra = true;
            }
        }

        if (shouldAddExtra) {
            extra.endUpdate();
            groups.add(extra);
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
