package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import com.warmthdawn.mods.yaoyaocoin.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.IEventBus;

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

    private void onMouseRelease(ScreenEvent.MouseReleasedEvent.Pre event) {
        if (event.getButton() == 0) {
            isLeftMouseDown = false;
        }

        if (ignoreMouseUp) {
            event.setCanceled(true);
            ignoreMouseUp = false;
        }
    }

    private void onMouseClick(ScreenEvent.MouseClickedEvent.Pre event) {
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
                event.setCanceled(true);
                ignoreMouseUp = true;
            }
        }
    }


    public void onInit(ScreenEvent.InitScreenEvent.Post event) {
        layoutManager.init();
    }

    public void onDrawBackground(ContainerScreenEvent.DrawBackground event) {
        // BindTexture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GuiTextures.COIN_SLOT);

//        for (int i = -1; i <= layoutManager.getGridHeight(); i++) {
//            for (int j = -1; j <= layoutManager.getGridWidth(); j++) {
//                drawSlot(event.getPoseStack(), x + j * 20, y + i * 20, j, i);
//            }
//        }

        for (CoinSlotGroup group : layoutManager.getGroups()) {
            int x0 = group.getGroupX();
            int y0 = group.getGroupY();
            for (int i = -1; i <= group.getGridHeight(); i++) {
                for (int j = -1; j <= group.getGridWidth(); j++) {
                    drawSlot(event.getPoseStack(), group, x0 + j * 20, y0 + i * 20, j, i);
                }
            }
        }

    }

    public void onDrawForeground(ContainerScreenEvent.DrawForeground event) {

        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        AbstractContainerScreen<?> screen = event.getContainerScreen();

        PoseStack renderPoseStack = RenderSystem.getModelViewStack();
        renderPoseStack.pushPose();
        renderPoseStack.translate(-screen.getGuiLeft(), -screen.getGuiTop(), 0);
        RenderSystem.applyModelViewMatrix();

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
                drawSlotItem(event.getPoseStack(), x0, y0, gridX, gridY, slot);

            });

            event.getContainerScreen().setBlitOffset(0);

        }
        hoveringSlot = null;
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

                    int x = x0 + gridX * 20 + 2;
                    int y = y0 + gridY * 20 + 2;
                    AbstractContainerScreen.renderSlotHighlight(event.getPoseStack(), x, y, screen.getBlitOffset(), SLOT_COLOR);
                }
            });
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
        itemRenderer.renderGuiItemDecorations(minecraft.font, stack, x, y, String.valueOf(count));

        itemRenderer.blitOffset = 0.0F;

    }

    private void drawSlotItem(PoseStack poseStack, int x0, int y0, int gridX, int gridY, CoinSlot slot) {

        int x = x0 + gridX * 20 + 2;
        int y = y0 + gridY * 20 + 2;

        ItemStack stack = slot.getStack();
        int count = slot.getCount();

        drawSlotItem(poseStack, x, y, stack, count);


    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int x0, int y0, int slotX, int slotY) {
        return mouseX >= x0 + slotX * 20 + 2 && mouseX < x0 + slotX * 20 + 18 &&
                mouseY >= y0 + slotY * 20 + 2 && mouseY < y0 + slotY * 20 + 18;
    }

    private void drawSlot(PoseStack poseStack, CoinSlotGroup group, int x0, int y0, int slotX, int slotY) {
        boolean hasSlot = group.hasSlot(slotX, slotY);

        int x = x0;
        int y = y0;

        if (hasSlot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.FILL);
            return;
        }

        // top left
        CoinSlotGroup.NeighborKind up = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.UP);
        CoinSlotGroup.NeighborKind left = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.LEFT);
        CoinSlotGroup.NeighborKind upLeft = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.UP_LEFT);

        if (up == CoinSlotGroup.NeighborKind.Slot) {

            if (left == CoinSlotGroup.NeighborKind.Slot) {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_LEFT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_2);
            }
        } else if (left == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.LEFT_2);
        } else if (upLeft == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_LEFT_1);
        }

        x = x0 + 10;
        y = y0;
        // top right
        CoinSlotGroup.NeighborKind upRight = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.UP_RIGHT);
        CoinSlotGroup.NeighborKind right = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.RIGHT);

        if (up == CoinSlotGroup.NeighborKind.Slot) {
            if (right == CoinSlotGroup.NeighborKind.Slot) {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_RIGHT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_2);
            }
        } else if (right == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.RIGHT_2);
        } else if (upRight == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.TOP_RIGHT_1);
        }


        x = x0;
        y = y0 + 10;
        // bottom left
        CoinSlotGroup.NeighborKind down = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.DOWN);
        CoinSlotGroup.NeighborKind bottomLeft = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.DOWN_LEFT);

        if (down == CoinSlotGroup.NeighborKind.Slot) {
            if (left == CoinSlotGroup.NeighborKind.Slot) {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_LEFT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_2);
            }
        } else if (left == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.LEFT_2);
        } else if (bottomLeft == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_LEFT_1);
        }


        // bottom right
        x = x0 + 10;
        y = y0 + 10;
        CoinSlotGroup.NeighborKind bottomRight = group.getNeighbourKind(slotX, slotY, CoinSlotGroup.Neighbour.DOWN_RIGHT);

        if (down == CoinSlotGroup.NeighborKind.Slot) {
            if (right == CoinSlotGroup.NeighborKind.Slot) {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_RIGHT_3);
            } else {
                drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_2);
            }
        } else if (right == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.RIGHT_2);
        } else if (bottomRight == CoinSlotGroup.NeighborKind.Slot) {
            drawSlotPart(poseStack, x, y, GuiTextures.SlotPart.BOTTOM_RIGHT_1);
        }


    }


    private void drawSlotPart(PoseStack poseStack, int x, int y, GuiTextures.SlotPart part) {

        blit(poseStack, x, y, this.getBlitOffset(), part.uOffset, part.vOffset, part.uWidth, part.vHeight, 40, 40);
    }


}
