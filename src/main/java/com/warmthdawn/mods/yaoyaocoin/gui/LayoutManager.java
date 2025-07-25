package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.config.CoinSaveState;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.misc.Block;
import com.warmthdawn.mods.yaoyaocoin.misc.GroupCollision;
import com.warmthdawn.mods.yaoyaocoin.misc.Rectangle2i;
import com.warmthdawn.mods.yaoyaocoin.misc.Vector2i;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.util.Tuple;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LayoutManager {

    private final ArrayList<CoinSlotGroup> groups = new ArrayList<>();
    private final Int2IntOpenHashMap slotIdToGroupIndex = new Int2IntOpenHashMap();

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_BORDER_SIZE = 4;

    private final Logger logger = LogUtils.getLogger();
    private AbstractContainerScreen<?> screen;

    public LayoutManager() {

    }

    private Rectangle2i computeScreenRect(AbstractContainerScreen<?> screen) {
        int borderSize = 4;

        Rectangle2i rect = new Rectangle2i(screen.getGuiLeft() + borderSize, screen.getGuiTop() + borderSize,
                screen.getXSize() - borderSize * 2, screen.getYSize() - borderSize * 2);

        if (screen instanceof ContainerScreen) {
            // chest has one pixel offset
            rect.setHeight(rect.getHeight() - 1);
        }

        return rect;
    }

    void clear() {
        groups.clear();
        slotIdToGroupIndex.clear();
        screen = null;
    }

    public void finishMovement(AbstractContainerScreen<?> screen) {
        computeGroupOverlap(false, true);
        CoinSaveState saveState = CoinSaveState.instance();
        CoinManager manager = CoinManager.getInstance();

        Rectangle2i screenRect = computeScreenRect(screen);

        saveState.clear();
        int id = 0;
        for (CoinSlotGroup group : groups) {
            CoinSaveState.Group saveGroup = new CoinSaveState.Group();
            int groupId = id++;
            saveGroup.id = groupId;
            saveGroup.visible = group.isVisible();

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
            Rectangle2i groupRect = new Rectangle2i(group.getGroupX(), group.getGroupY(),
                    group.getGridWidth() * SLOT_SIZE, group.getGridHeight() * SLOT_SIZE);

            boolean dockTop = true;
            boolean dockLeft = true;

            // 垂直
            {
                if (groupRect.getY1() <= screenRect.getY()) {
                    // 上
                    saveGroup.vertical = groupRect.getY() - screenRect.getY();
                } else if (groupRect.getY() >= screenRect.getY1()) {
                    // 下
                    dockTop = false;
                    saveGroup.vertical = groupRect.getY() - screenRect.getY1();
                } else {
                    int topDist = groupRect.getY() - screenRect.getY();
                    int bottomDist = groupRect.getY1() - screenRect.getY1();

                    boolean topStick = topDist % SLOT_SIZE == 0;
                    boolean bottomStick = bottomDist % SLOT_SIZE == 0;

                    if (topStick == bottomStick) {
                        if (topDist <= bottomDist) {
                            saveGroup.vertical = topDist;
                        } else {
                            dockTop = false;
                            saveGroup.vertical = bottomDist - groupRect.getHeight();
                        }
                    } else if (topStick) {
                        saveGroup.vertical = topDist;
                    } else {
                        dockTop = false;
                        saveGroup.vertical = bottomDist - groupRect.getHeight();
                    }
                }
            }

            // 水平
            {
                if (groupRect.getX1() <= screenRect.getX()) {
                    saveGroup.horizontal = groupRect.getX() - screenRect.getX();
                } else if (groupRect.getX() >= screenRect.getX1()) {
                    saveGroup.horizontal = groupRect.getX() - screenRect.getX1();
                    dockLeft = false;
                } else {
                    int leftDist = groupRect.getX() - screenRect.getX();
                    int rightDist = groupRect.getX1() - screenRect.getX1();

                    boolean leftStick = leftDist % SLOT_SIZE == 0;
                    boolean rightStick = rightDist % SLOT_SIZE == 0;

                    if (leftStick == rightStick) {
                        if (leftDist <= rightDist) {
                            saveGroup.horizontal = leftDist;
                        } else {
                            dockLeft = false;
                            saveGroup.horizontal = rightDist - groupRect.getWidth();
                        }
                    } else if (leftStick) {
                        saveGroup.horizontal = leftDist;
                    } else {
                        dockLeft = false;
                        saveGroup.horizontal = rightDist - groupRect.getWidth();
                    }
                }
            }

            if (dockTop && dockLeft) {
                saveGroup.area = CoinSaveState.LayoutArea.TOP_LEFT;
            } else if (dockTop) {
                saveGroup.area = CoinSaveState.LayoutArea.TOP_RIGHT;
            } else if (dockLeft) {
                saveGroup.area = CoinSaveState.LayoutArea.BOTTOM_LEFT;
            } else {
                saveGroup.area = CoinSaveState.LayoutArea.BOTTOM_RIGHT;
            }

            saveState.addGroup(saveGroup);
        }

        saveState.save();
    }

    private boolean collisionWithScreen(Rectangle2i screenRect, CoinSlotGroup group, Vector2i pos, int stickyDistance) {
        int newX = pos.getX();
        int newY = pos.getY();
        boolean collision = false;

        Rectangle2i innerBounds = screenRect.expand(-SLOT_SIZE);
        // 计算碰撞
        for (Rectangle2i rect : group.getCollisionRects()) {
            // check if collisionRects is inside screen rect

            Rectangle2i rectActual = rect.scaled(SLOT_SIZE);
            Rectangle2i rectSticky = rectActual.expand(stickyDistance);

            if (!screenRect.intersects(rectSticky.translated(new Vector2i(newX, newY)))) {
                continue;
            }

            collision = true;

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

        if (collision) {
            // 计算吸附
            Rectangle2i groupRect = new Rectangle2i(newX, newY, group.getGridWidth() * SLOT_SIZE,
                    group.getGridHeight() * SLOT_SIZE);
            if (newY < innerBounds.getY()) {
                // 将 newY 对其到整数倍的 SLOT_SIZE (四舍五入)
                newY = screenRect.getY() + Math.round((newY - screenRect.getY()) / (float) SLOT_SIZE) * SLOT_SIZE;
            } else if (newY + groupRect.getHeight() > innerBounds.getY1()) {
                // 将 newY 对其到整数倍的 SLOT_SIZE (四舍五入)
                newY = screenRect.getY1() - groupRect.getHeight()
                        + Math.round((newY - screenRect.getY1() + groupRect.getHeight()) / (float) SLOT_SIZE)
                                * SLOT_SIZE;
            }

            if (newX < innerBounds.getX()) {
                // 将 newX 对其到整数倍的 SLOT_SIZE (四舍五入)
                newX = screenRect.getX() + Math.round((newX - screenRect.getX()) / (float) SLOT_SIZE) * SLOT_SIZE;
            } else if (newX + groupRect.getWidth() > innerBounds.getX1()) {
                // 将 newX 对其到整数倍的 SLOT_SIZE (四舍五入)
                newX = screenRect.getX1() - groupRect.getWidth()
                        + Math.round((newX - screenRect.getX1() + groupRect.getWidth()) / (float) SLOT_SIZE)
                                * SLOT_SIZE;
            }
            pos.setX(newX);
            pos.setY(newY);

        }

        return collision;

    }

    public void updateGroupPosition(AbstractContainerScreen<?> screen, CoinSlotGroup group, int x, int y,
            boolean forceAdsorb) {
        int newX = x;
        int newY = y;
        // collisions to center rect

        Rectangle2i screenRect = computeScreenRect(screen);

        Vector2i screenSticky = new Vector2i(newX, newY);
        if (collisionWithScreen(screenRect, group, screenSticky, 8)) {
            newX = screenSticky.getX();
            newY = screenSticky.getY();
        }

        Vector2i newPos = new Vector2i(newX, newY);
        GroupCollision offsetCollision = GroupCollision.compute(group, SLOT_SIZE, Vector2i.ZERO);
        GroupCollision currentCollision = offsetCollision.translated(newPos);
        GroupCollision screenCollision = GroupCollision.createSingle(screenRect);

        GroupCollision[] collisions = new GroupCollision[groups.size()];

        for (int i = 0; i < groups.size(); i++) {
            CoinSlotGroup otherGroup = groups.get(i);
            if (otherGroup == group) {
                continue;
            }
            if (!otherGroup.isVisible()) {
                continue;
            }
            collisions[i] = otherGroup.getCollision();
        }

        Vector2i nearestOffset = null;
        Vector2i finalNewPos = null;

        for (int i = 0; i < groups.size(); i++) {
            CoinSlotGroup otherGroup = groups.get(i);
            if (otherGroup == group) {
                continue;
            }
            if (!otherGroup.isVisible()) {
                continue;
            }

            if (!forceAdsorb) {

                GroupCollision adsorptionCollision = otherGroup.getCollision().expand(SLOT_SIZE / 2);
                if (!currentCollision.intersects(adsorptionCollision)) {
                    continue;
                }
            }
            Vector2i otherOffset = otherGroup.getOffset();

            Block otherBlock = otherGroup.createAdsorptionBlock(SLOT_SIZE, Vector2i.ZERO);
            Block currentBlock = group.createAdsorptionBlock(SLOT_SIZE, newPos.subtract(otherOffset));

            Vector2i blockOffset = otherOffset
                    .add(new Vector2i(currentBlock.getX(), currentBlock.getY()).scaleInPlace(SLOT_SIZE));
            offsetCollision.translateInPlace(blockOffset);
            final int currentGroupId = i;
            Vector2i offset = Block.moveBlocks(currentBlock, otherBlock, (pos) -> {
                Vector2i offsetPos = pos.scaled(SLOT_SIZE);
                offsetCollision.translateInPlace(offsetPos);
                boolean valid = true;

                if (screenCollision.intersects(offsetCollision)) {
                    valid = false;
                } else {

                    for (int j = 0; j < collisions.length; j++) {
                        GroupCollision collision = collisions[j];
                        if (collision == null || j == currentGroupId) {
                            continue;
                        }

                        CoinSlotGroup collideGroup = groups.get(j);
                        if ((collideGroup.getGroupX() - otherGroup.getGroupX()) % SLOT_SIZE == 0
                                && (collideGroup.getGroupY() - otherGroup.getGroupY()) % SLOT_SIZE == 0) {
                            // same grid, may merge so ignore borders
                            if (collision.intersects(offsetCollision)) {
                                valid = false;
                                break;
                            }
                        } else {

                            if (collision.intersects(offsetCollision, 2 * SLOT_BORDER_SIZE)) {
                                valid = false;
                                break;
                            }
                        }

                    }
                }
                offsetCollision.translateInPlace(offsetPos.invertInPlace());
                return valid;
            });
            offsetCollision.translateInPlace(blockOffset.invertInPlace());

            if (offset == null) {
                continue;
            }

            if (nearestOffset == null || offset.lengthManhattan() < nearestOffset.lengthManhattan()) {
                Vector2i pos = otherOffset
                        .add(new Vector2i(currentBlock.getX(), currentBlock.getY()).scaleInPlace(SLOT_SIZE));

                nearestOffset = offset;
                finalNewPos = pos;

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
        computeGroupOverlap(true, true);

    }

    private void computeGroupOverlap(boolean borrow, boolean rebuildIndex) {
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
                // 借用模式，每个组内互相借用槽位
                for (int i = 0; i < list.size(); i++) {
                    CoinSlotGroup groupA = list.get(i);
                    if (groupA.isDiscard() || !groupA.isVisible()) {
                        continue;
                    }
                    groupA.clearBorrowedSlots();
                    groupA.beginUpdate();
                    boolean modified = false;
                    for (int j = 0; j < list.size(); j++) {
                        if (i == j) {
                            continue;
                        }
                        CoinSlotGroup groupB = list.get(j);
                        if (groupB.isDiscard() || !groupB.isVisible()) {
                            continue;
                        }
                        if (groupA.borrowSlots(groupB, SLOT_SIZE)) {
                            modified = true;
                        }
                    }
                    groupA.endUpdate();
                }
            } else {
                List<CoinSlotGroup> added = new ArrayList<>();
                boolean modified = CoinSlotGroup.combineGroups(list, added, SLOT_SIZE);
                if (modified) {
                    changed = true;
                    for (CoinSlotGroup group : list) {
                        if (group.isDiscard()) {
                            groups.remove(group);
                        }
                    }
                }
                if (!added.isEmpty()) {
                    groups.addAll(added);
                    changed = true;
                }
            }
        }

        if (changed && rebuildIndex) {
            rebuildGroupIndex();
        }
    }

    public void updateGroupVisibility() {
        ClientCoinStorage storage = ClientCoinStorage.INSTANCE;
        ArrayList<Tuple<CoinSlotGroup, CoinSlot>> toShow = new ArrayList<>();
        ArrayList<Tuple<CoinSlotGroup, CoinSlot>> toHide = new ArrayList<>();
        for (CoinSlotGroup group : groups) {
            group.iterateSlots((slotX, slotY, slot, borrowed) -> {
                if (borrowed) {
                    return;
                }
                boolean visible = storage.getSlots().get(slot.getId()).isVisible();

                if (visible == group.isVisible()) {
                    return;
                }
                if (visible) {
                    toShow.add(new Tuple<>(group, slot));
                } else {
                    toHide.add(new Tuple<>(group, slot));
                }
            });
        }

        for (Tuple<CoinSlotGroup, CoinSlot> tuple : toHide) {
            CoinSlotGroup group = tuple.getA();

            if (group.isSingle()) {
                group.setVisible(false);
                continue;
            }

            CoinSlotGroup newGroup = new CoinSlotGroup();
            newGroup.setVisible(false);
            newGroup.setGroupX(group.getGroupX());
            newGroup.setGroupY(group.getGroupY());
            newGroup.takeSlot(tuple.getB().getId(), group);
            groups.add(newGroup);
            splitGroup(group);
        }

        for (Tuple<CoinSlotGroup, CoinSlot> tuple : toShow) {
            CoinSlotGroup group = tuple.getA();
            group.setVisible(true);
            updateGroupPosition(screen, group, group.getGroupX(), group.getGroupY(), false);
        }

        computeGroupOverlap(false, false);
        rebuildGroupIndex();
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
        return this.takeSlot(slotId, group, true);
    }

    public CoinSlotGroup takeSlot(int slotId, CoinSlotGroup group, boolean rebuildIndex) {
        if (group.isSingle()) {
            return group;
        }
        CoinSlotGroup newGroup = new CoinSlotGroup();
        newGroup.setGroupX(group.getGroupX());
        newGroup.setGroupY(group.getGroupY());
        newGroup.takeSlot(slotId, group);
        groups.add(newGroup);
        splitGroup(group);

        computeGroupOverlap(true, false);
        if (rebuildIndex) {
            rebuildGroupIndex();
        }
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
        this.screen = screen;

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
                    logger.warn("Slot {} is already used", (Object) slotId);
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
            slotGroup.setVisible(group.visible);

            if (slotGroup.empty() || slotGroup.isDiscard()) {
                continue;
            }

            Rectangle2i screenRect = computeScreenRect(screen);

            switch (group.area) {
                case TOP_LEFT, TOP_CENTER, CENTER_LEFT, INVALID -> {
                    int x0 = screenRect.getX();
                    int y0 = screenRect.getY();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }

                case TOP_RIGHT, CENTER_RIGHT -> {
                    int x0 = screenRect.getX1();
                    int y0 = screenRect.getY();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }

                case BOTTOM_LEFT, BOTTOM_CENTER -> {
                    int x0 = screenRect.getX();
                    int y0 = screenRect.getY1();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }

                case BOTTOM_RIGHT -> {
                    int x0 = screenRect.getX1();
                    int y0 = screenRect.getY1();
                    slotGroup.setGroupX(x0 + group.horizontal);
                    slotGroup.setGroupY(y0 + group.vertical);
                }
            }

            // move out
            if (group.area == CoinSaveState.LayoutArea.INVALID) {
                Vector2i pos = new Vector2i(slotGroup.getGroupX(), slotGroup.getGroupY());
                if (collisionWithScreen(screenRect, slotGroup, pos, 0)) {
                    slotGroup.setGroupX(pos.getX());
                    slotGroup.setGroupY(pos.getY());
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

        updateGroupVisibility();
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
