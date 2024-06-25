package com.warmthdawn.mods.yaoyaocoin.gui;

import com.warmthdawn.mods.yaoyaocoin.config.CoinSaveState;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;

public class LayoutManager {


    private final ArrayList<CoinSlotGroup> groups = new ArrayList<>();
    private final Int2IntOpenHashMap slotIdToGroupIndex = new Int2IntOpenHashMap();

    private static final int SLOT_SIZE = 20;


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
        int prevX = group.getGroupX();
        int prevY = group.getGroupY();

        int dx = x - prevX;
        int dy = y - prevY;

        int newX = x;
        int newY = y;
        // collisions to center rect

        int screenX0 = screen.getGuiLeft();
        int screenY0 = screen.getGuiTop();

        int screenX1 = screenX0 + screen.getXSize();
        int screenY1 = screenY0 + screen.getYSize();

        for (Rect2i rect : group.getCollisionRects()) {
            // check if collisionRects is inside screen rect

            int rectX0 = rect.getX() * SLOT_SIZE;
            int rectY0 = rect.getY() * SLOT_SIZE;

            int rectWidth = rect.getWidth() * SLOT_SIZE;
            int rectHeight = rect.getHeight() * SLOT_SIZE;

            boolean xInside = newX + rectX0 + rectWidth > screenX0 &&
                    newX + rectX0 < screenX1;

            boolean yInside = newY + rectY0 + rectHeight > screenY0 &&
                    newY + rectY0 < screenY1;


            if (xInside && yInside) {

                int xLeft = screenX0 - rectWidth - rectX0;
                int xRight = screenX1 - rectX0;
                int yTop = screenY0 - rectHeight - rectY0;
                int yBottom = screenY1 - rectY0;

                int stoppedX;
                if(Math.abs(newX - xLeft) < Math.abs(newX - xRight)) {
                    stoppedX = xLeft;
                } else {
                    stoppedX = xRight;
                }
                int stoppedY;
                if(Math.abs(newY - yTop) < Math.abs(newY - yBottom)) {
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
        }

        group.setGroupX(newX);
        group.setGroupY(newY);


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
                slotIdToGroupIndex.put(slotId, i);
                slotGroup.addSlot(slot.x, slot.y, clientSlot, false);

                maxX = Math.max(maxX, slot.x);
                maxY = Math.max(maxY, slot.y);
                minX = Math.min(minX, slot.x);
                minY = Math.min(minY, slot.y);
            }
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
            slotGroup.endUpdate();

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
