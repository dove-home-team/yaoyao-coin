package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class CoinGuiHandler extends GuiComponent {

    private final LayoutManager layoutManager = new LayoutManager();

    public void initialize(IEventBus modEventBus) {
        modEventBus.addListener(this::onDrawBackground);
        modEventBus.addListener(this::onDrawForeground);


    }


    public void onInit(ScreenEvent.InitScreenEvent.Post event) {

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
