package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import com.warmthdawn.mods.yaoyaocoin.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CoinGuiHandler extends GuiComponent {

    private final LayoutManager layoutManager = new LayoutManager();

    public void initialize(IEventBus modEventBus) {
        modEventBus.addListener(this::onDrawBackground);
        modEventBus.addListener(this::onDrawForeground);
        modEventBus.addListener(this::onInit);
        modEventBus.addListener(this::onMouseClick);
        modEventBus.addListener(this::onMouseRelease);

    }

    private static final int SLOT_COLOR = 0x80FFFFFF;


    private boolean isLeftMouseDown = false;
    private boolean ignoreMouseUp = false;
    private CoinSlot hoveringSlot = null;
    private CoinSlotGroup hoveringGroup = null;

    private double draggingStartX = 0;
    private double draggingStartY = 0;
    private int prevDraggingX = 0;
    private int prevDraggingY = 0;
    private CoinSlotGroup draggingGroup = null;

    private boolean enabled = false;

    private void onMouseRelease(ScreenEvent.MouseReleasedEvent.Pre event) {
        if (!enabled) {
            return;
        }
        if (event.getButton() == 0) {
            isLeftMouseDown = false;
        }


        if (ignoreMouseUp) {
            event.setCanceled(true);
            ignoreMouseUp = false;
        }

        if (draggingGroup != null) {
            resetDragging();

            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                layoutManager.finishMovement(screen);
            }
        }
    }

    private void resetDragging() {
        draggingGroup = null;
        draggingStartX = 0;
        draggingStartY = 0;
        prevDraggingX = 0;
        prevDraggingY = 0;
    }

    private void onMouseClick(ScreenEvent.MouseClickedEvent.Pre event) {
        if (!enabled) {
            return;
        }
        if (event.getButton() == 0) {
            isLeftMouseDown = true;
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            double mouseX = event.getMouseX();
            double mouseY = event.getMouseY();

            if (hoveringSlot != null) {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    ItemStack mouseItem = screen.getMenu().getCarried();
                    boolean isRightClick = event.getButton() == 1;

                    boolean isShiftHolding = Screen.hasShiftDown();
                    ItemStack stack = CoinUtils.handleSlotClick(hoveringSlot, mouseItem, isRightClick, isShiftHolding);
                    screen.getMenu().setCarried(stack);
                }
                ignoreMouseUp = true;
                event.setCanceled(true);
            } else if (hoveringGroup != null) {
                // Prevent click-through on the background and border of the slot
                boolean isShiftHolding = Screen.hasShiftDown();
                if (isShiftHolding || hoveringGroup.isSingle()) {
                    draggingGroup = hoveringGroup;
                    draggingStartX = mouseX;
                    draggingStartY = mouseY;
                    prevDraggingX = draggingGroup.getGroupX();
                    prevDraggingY = draggingGroup.getGroupY();
                } else {

                    int hoveringSlotId = findHoveringSlotConsideringBorder(hoveringGroup, (int) mouseX, (int) mouseY);

                    if (hoveringSlotId >= 0) {

                        draggingGroup = layoutManager.takeSlot(hoveringSlotId, hoveringGroup);
                        hoveringGroup = draggingGroup;

                        draggingStartX = mouseX;
                        draggingStartY = mouseY;
                        prevDraggingX = draggingGroup.getGroupX();
                        prevDraggingY = draggingGroup.getGroupY();

                    }

                }


                event.setCanceled(true);
                ignoreMouseUp = true;
            }
        }
    }

    private boolean isSupportedScreen(AbstractContainerScreen<?> screen) {
        // disable for creative inventory

        if (screen instanceof CreativeModeInventoryScreen) {
            return false;
        }

        return true;
    }


    public void onInit(ScreenEvent.InitScreenEvent.Post event) {

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && isSupportedScreen(screen)) {
            layoutManager.init(screen);
            enabled = true;
        } else {
            layoutManager.clear();
            enabled = false;
        }


        resetDragging();
        this.hoveringSlot = null;
        this.hoveringGroup = null;
        this.isLeftMouseDown = false;
        this.ignoreMouseUp = false;
    }

    public void onDrawBackground(ContainerScreenEvent.DrawBackground event) {
        if (!enabled) {
            return;
        }
        // BindTexture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GuiTextures.COIN_SLOT);

        PoseStack poseStack = event.getPoseStack();
        for (CoinSlotGroup group : layoutManager.getGroups()) {
            int x0 = group.getGroupX();
            int y0 = group.getGroupY();
            for (int i = -1; i <= group.getGridHeight(); i++) {
                for (int j = -1; j <= group.getGridWidth(); j++) {
                    drawSlot(poseStack, group, x0 + j * 20, y0 + i * 20, j, i);
                }
            }
        }
    }

    private void handleDragging(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (draggingGroup == null) {
            return;
        }

        double dx = mouseX - draggingStartX;
        double dy = mouseY - draggingStartY;

        int x0 = prevDraggingX;
        int y0 = prevDraggingY;

        layoutManager.updateGroupPosition(screen, draggingGroup, x0 + (int) Math.round(dx), y0 + (int) Math.round(dy));
    }

    public void onDrawForeground(ContainerScreenEvent.DrawForeground event) {

        if (!enabled) {
            return;
        }
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        AbstractContainerScreen<?> screen = event.getContainerScreen();

        handleDragging(screen, mouseX, mouseY);

        PoseStack renderPoseStack = RenderSystem.getModelViewStack();
        renderPoseStack.pushPose();
        renderPoseStack.translate(-screen.getGuiLeft(), -screen.getGuiTop(), 0);
        RenderSystem.applyModelViewMatrix();

        hoveringSlot = null;
        hoveringGroup = null;
        for (CoinSlotGroup group : layoutManager.getGroups()) {

            int x0 = group.getGroupX();
            int y0 = group.getGroupY();
            group.iterateSlots((gridX, gridY, slot, isBorrowed) -> {
                if (isBorrowed) {
                    return;
                }
                if (isMouseOverSlot(mouseX, mouseY, x0, y0, gridX, gridY)) {
                    AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
                    accessor.setHoveredSlot(null);
                    hoveringSlot = slot;
                } else if (isMouseOverSlotBorder(mouseX, mouseY, x0, y0, gridX, gridY)) {
                    hoveringGroup = group;
                }
            });
        }

        int[] coinCounts = null;
        boolean isShiftHolding = Screen.hasShiftDown();
        if (hoveringSlot != null && isShiftHolding) {
            ClientCoinStorage storage = ClientCoinStorage.INSTANCE;
            List<CoinSlot> slots = storage.getSlots();
            coinCounts = new int[slots.size()];
            for (int i = 0; i < slots.size(); i++) {
                coinCounts[i] = slots.get(i).getCount();
            }

            CoinManager manager = CoinManager.getInstance();
            manager.inspectCoins(coinCounts, hoveringSlot.getId());
        }

        int[] finalCoinCounts = coinCounts;

        for (CoinSlotGroup group : layoutManager.getGroups()) {
            int x0 = group.getGroupX();
            int y0 = group.getGroupY();

            event.getContainerScreen().setBlitOffset(100);
            group.iterateSlots((gridX, gridY, slot, isBorrowed) -> {
                // draw items
                if (isBorrowed) {
                    return;
                }

                RenderSystem.setShader(GameRenderer::getPositionTexShader);

                ItemStack stack = slot.getStack();
                int count = 0;
                if (finalCoinCounts != null) {
                    count = finalCoinCounts[slot.getId()];
                } else {
                    count = slot.getCount();
                }
                int x = x0 + gridX * 20 + 2;
                int y = y0 + gridY * 20 + 2;

                drawSlotItem(event.getPoseStack(), x, y, stack, count);

            });

            event.getContainerScreen().setBlitOffset(0);

        }

        if (hoveringSlot != null) {
            CoinSlotGroup group = layoutManager.getGroup(hoveringSlot.getId());
            int x0 = group.getGroupX();
            int y0 = group.getGroupY();

            int gridX = group.getSlotX(hoveringSlot.getId());
            int gridY = group.getSlotY(hoveringSlot.getId());
            int x = x0 + gridX * 20 + 2;
            int y = y0 + gridY * 20 + 2;
            AbstractContainerScreen.renderSlotHighlight(event.getPoseStack(), x, y, screen.getBlitOffset(), SLOT_COLOR);
        }

        renderPoseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private void drawSlotItem(PoseStack poseStack, int x, int y, ItemStack stack, int count) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Minecraft minecraft = Minecraft.getInstance();
        itemRenderer.blitOffset = 100.0F;

        RenderSystem.enableDepthTest();
        itemRenderer.renderAndDecorateItem(stack, x, y);
        // use smaller font if count is too large
        Font font = minecraft.font;

        poseStack.pushPose();
        String s = String.valueOf(count);
        poseStack.translate(x + 18, y + 18, 300.0D);
        if (count > 999) {
            poseStack.scale(0.6F, 0.6F, 0.6F);
        } else if (count > 99) {
            poseStack.scale(0.8F, 0.8F, 0.8F);
        }
        MultiBufferSource.BufferSource multibuffersource$buffersource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        font.drawInBatch(s, (float) (-font.width(s) - 1), -font.lineHeight, 0x00FFFFFF, true, poseStack.last().pose(), multibuffersource$buffersource, false, 0, 0xF000F0);
        multibuffersource$buffersource.endBatch();
//        itemRenderer.renderGuiItemDecorations(minecraft.font, stack, x, y, );

        poseStack.popPose();
        itemRenderer.blitOffset = 0.0F;


    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int x0, int y0, int slotX, int slotY) {
        return mouseX >= x0 + slotX * 20 + 2 && mouseX < x0 + slotX * 20 + 18 &&
                mouseY >= y0 + slotY * 20 + 2 && mouseY < y0 + slotY * 20 + 18;
    }

    private boolean isMouseOverSlotBorder(int mouseX, int mouseY, int x0, int y0, int slotX, int slotY) {
        return isMouseOverSlotBorder(mouseX, mouseY, x0, y0, slotX, slotY, 4);
    }

    private boolean isMouseOverSlotBorder(int mouseX, int mouseY, int x0, int y0, int slotX, int slotY, int borderSize) {
        return mouseX >= x0 + slotX * 20 - borderSize && mouseX < x0 + slotX * 20 + 20 + borderSize &&
                mouseY >= y0 + slotY * 20 - borderSize && mouseY < y0 + slotY * 20 + 20 + borderSize;
    }

    private int findHoveringSlotConsideringBorder(CoinSlotGroup group, int mouseX, int mouseY) {
        int x0 = group.getGroupX();
        int y0 = group.getGroupY();
        int slotId = group.findSlot((slotX, slotY, slot, isBorrowed) -> {
            if (isBorrowed) {
                return false;
            }
            return isMouseOverSlotBorder(mouseX, mouseY, x0, y0, slotX, slotY, 0);
        });

        if (slotId < 0) {
            AtomicInteger slotIdRef = new AtomicInteger(-1);
            AtomicInteger minDistanceSqRef = new AtomicInteger(Integer.MAX_VALUE);
            group.iterateSlots((slotX, slotY, slot, isBorrowed) -> {
                if (isBorrowed) {
                    return;
                }
                boolean isMouseOver = isMouseOverSlotBorder(mouseX, mouseY, x0, y0, slotX, slotY, 4);
                if (!isMouseOver) {
                    return;
                }

                int dx = mouseX - x0 - slotX * 20 - 10;
                int dy = mouseY - y0 - slotY * 20 - 10;

                int distanceSq = dx * dx + dy * dy;

                if (distanceSq < minDistanceSqRef.get()) {
                    minDistanceSqRef.set(distanceSq);
                    slotIdRef.set(slot.getId());
                }

            });

            slotId = slotIdRef.get();
        }

        return slotId;
    }

    private void drawSlot(PoseStack poseStack, CoinSlotGroup group, int x0, int y0, int slotX, int slotY) {
        boolean hasSlot = group.hasSlot(slotX, slotY);

        int x = x0;
        int y = y0;

        if (hasSlot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.FILL);
            return;
        }

        if (group.hasSlot(slotX, slotY, false)) {
            return;
        }

        // top left
        CoinSlotGroup.NeighborKind up = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.UP);
        CoinSlotGroup.NeighborKind left = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.LEFT);
        CoinSlotGroup.NeighborKind upLeft = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.UP_LEFT);

        if (up != CoinSlotGroup.NeighborKind.Empty) {

            if (left != CoinSlotGroup.NeighborKind.Empty) {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_LEFT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_2);
            }
        } else if (left != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.LEFT_2);
        } else if (upLeft != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_LEFT_1);
        }

        x = x0 + 10;
        y = y0;
        // top right
        CoinSlotGroup.NeighborKind upRight = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.UP_RIGHT);
        CoinSlotGroup.NeighborKind right = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.RIGHT);


        if (up != CoinSlotGroup.NeighborKind.Empty) {
            if (right != CoinSlotGroup.NeighborKind.Empty) {
                // top has slot and right has slot
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_RIGHT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_2);
            }
        } else if (right != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.RIGHT_2);
        } else if (upRight != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_RIGHT_1);
        }


        x = x0;
        y = y0 + 10;
        // bottom left
        CoinSlotGroup.NeighborKind down = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.DOWN);
        CoinSlotGroup.NeighborKind bottomLeft = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.DOWN_LEFT);

        if (down != CoinSlotGroup.NeighborKind.Empty) {
            if (left != CoinSlotGroup.NeighborKind.Empty) {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_LEFT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_2);
            }
        } else if (left != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.LEFT_2);
        } else if (bottomLeft != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_LEFT_1);
        }


        // bottom right
        x = x0 + 10;
        y = y0 + 10;
        CoinSlotGroup.NeighborKind bottomRight = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.DOWN_RIGHT);

        if (down != CoinSlotGroup.NeighborKind.Empty) {
            if (right != CoinSlotGroup.NeighborKind.Empty) {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_RIGHT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_2);
            }
        } else if (right != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.RIGHT_2);
        } else if (bottomRight != CoinSlotGroup.NeighborKind.Empty) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_RIGHT_1);
        }


    }


    private void drawSlotPart(PoseStack poseStack, int x, int y, GuiTextures.SlotPart part) {
        blit(poseStack, x, y, this.getBlitOffset(), part.uOffset, part.vOffset, part.uWidth, part.vHeight, 40, 40);
    }


}
