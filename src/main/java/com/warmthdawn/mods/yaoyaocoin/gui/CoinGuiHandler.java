package com.warmthdawn.mods.yaoyaocoin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.SlotKind;
import com.warmthdawn.mods.yaoyaocoin.event.CoinRefreshedEvent;
import com.warmthdawn.mods.yaoyaocoin.event.ScreenTooltipEvent;
import com.warmthdawn.mods.yaoyaocoin.misc.CoinUtils;
import com.warmthdawn.mods.yaoyaocoin.misc.GroupCollision;
import com.warmthdawn.mods.yaoyaocoin.misc.Rectangle2i;
import com.warmthdawn.mods.yaoyaocoin.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ItemDecoratorHandler;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CoinGuiHandler {

    private final LayoutManager layoutManager = new LayoutManager();
    private static final int SLOT_SIZE = 20;

    public void initialize(IEventBus modEventBus) {
        modEventBus.addListener(this::onDrawBackground);
        modEventBus.addListener(this::onDrawForeground);
        modEventBus.addListener(this::onInit);
        modEventBus.addListener(this::onMouseClick);
        modEventBus.addListener(this::onMouseRelease);
        modEventBus.addListener(this::onRenderTooltip);
        modEventBus.addListener(this::onCoinUpdate);

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

    private Rectangle2i guiBounds = new Rectangle2i(0, 0, 0, 0);
    private Rectangle2i guiInnerBounds = new Rectangle2i(0, 0, 0, 0);

    private void onRenderTooltip(ScreenTooltipEvent event) {
        if (!enabled) {
            return;
        }

        if (hoveringSlot != null) {
            AbstractContainerScreen<?> screen = event.getContainerScreen();
            ItemStack itemstack = this.hoveringSlot.getStack();
            event.getGuiGraphics().renderTooltip(screen.getMinecraft().font, Screen.getTooltipFromItem(screen.getMinecraft(), itemstack), itemstack.getTooltipImage(), itemstack, event.getMouseX(), event.getMouseY());
        }
    }

    private void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
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

    private void onCoinUpdate(CoinRefreshedEvent event) {
        if (!enabled) {
            return;
        }
        layoutManager.updateGroupVisibility();
    }

    private void resetDragging() {
        draggingGroup = null;
        draggingStartX = 0;
        draggingStartY = 0;
        prevDraggingX = 0;
        prevDraggingY = 0;
    }

    private void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
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
                if (!isShiftHolding || hoveringGroup.isSingle()) {
                    draggingGroup = hoveringGroup;
                    draggingStartX = mouseX;
                    draggingStartY = mouseY;
                    prevDraggingX = draggingGroup.getGroupX();
                    prevDraggingY = draggingGroup.getGroupY();
                } else {

                    int hoveringSlotId = findHoveringSlotConsideringBorder(hoveringGroup, (int) mouseX, (int) mouseY);

                    if (hoveringSlotId >= 0) {
                        if (isLeftMouseDown) {
                            draggingGroup = layoutManager.takeSlot(hoveringSlotId, hoveringGroup);
                            hoveringGroup = draggingGroup;

                            draggingStartX = mouseX;
                            draggingStartY = mouseY;
                            prevDraggingX = draggingGroup.getGroupX();
                            prevDraggingY = draggingGroup.getGroupY();
                        } else if (event.getButton() == 1) {
                            // TEMP: hide the slot by center click
                        }


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

        return !(screen instanceof InventoryScreen) || !Minecraft.getInstance().player.isCreative();
    }


    public void onInit(ScreenEvent.Init.Post event) {

        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && isSupportedScreen(screen)) {
            layoutManager.init(screen);

            final int borderSize = 4;
            guiBounds = new Rectangle2i(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize()).expand(-borderSize);
            guiInnerBounds = guiBounds.expand(-(SLOT_SIZE + 1));
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

    public void onDrawBackground(ContainerScreenEvent.Render.Background event) {
        if (!enabled) {
            return;
        }
        // BindTexture
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        RenderSystem.setShaderTexture(0, GuiTextures.COIN_SLOT);

        for (CoinSlotGroup group : layoutManager.getGroups()) {
            if (!group.isVisible()) {
                continue;
            }
            if (group.isDiscard()) {
                continue;
            }
            int x0 = group.getGroupX();
            int y0 = group.getGroupY();
            for (int i = -1; i <= group.getGridHeight(); i++) {
                for (int j = -1; j <= group.getGridWidth(); j++) {
                    drawSlot(event.getGuiGraphics(), group, x0 + j * 20, y0 + i * 20, j, i, 0);
                }
            }
        }


        GuiGraphics graphics = event.getGuiGraphics();

        if (Minecraft.getInstance().options.renderDebug) {

            // draw collision rects
            for (CoinSlotGroup group : layoutManager.getGroups()) {
                if (!group.isVisible() || group == draggingGroup) {
                    continue;
                }
                GroupCollision collision = group.getCollision();

                // Red line border
                for (Rectangle2i rect : collision.getCollisionRects()) {
                    drawRect(graphics, rect, 0x44FF0000);
                }

                // draw overlap rect with green line
                Rectangle2i rect = collision.getOverlapRect();
                drawRect(graphics, rect, 0x4400FF00);
            }

            // draw current dragging rect
            if (draggingGroup != null) {
                GroupCollision collision = draggingGroup.getCollision().expand(SLOT_SIZE / 2);
                drawRect(graphics, collision.getOverlapRect(), 0x440000FF);
                for (Rectangle2i rect : collision.getCollisionRects()) {
                    drawRect(graphics, rect, 0x4400FFFF);
                }
            }
        }

    }

    private static void drawRect(GuiGraphics graphics, Rectangle2i rect, int color) {

        graphics.hLine(rect.getX(), rect.getX1() - 1, rect.getY(), color);
        graphics.hLine(rect.getX(), rect.getX1() - 1, rect.getY1() - 1, color);
        graphics.vLine(rect.getX(), rect.getY(), rect.getY1() - 1, color);
        graphics.vLine(rect.getX1() - 1, rect.getY(), rect.getY1() - 1, color);
    }

    private void handleDragging(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (draggingGroup == null) {
            return;
        }

        double dx = mouseX - draggingStartX;
        double dy = mouseY - draggingStartY;

        int x0 = prevDraggingX;
        int y0 = prevDraggingY;

        boolean isShiftHolding = Screen.hasShiftDown();

        layoutManager.updateGroupPosition(screen, draggingGroup, x0 + (int) Math.round(dx), y0 + (int) Math.round(dy), isShiftHolding);
    }

    public void onDrawForeground(ContainerScreenEvent.Render.Foreground event) {

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
            if (!group.isVisible()) {
                continue;
            }
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
            if (!group.isVisible()) {
                continue;
            }
            int x0 = group.getGroupX();
            int y0 = group.getGroupY();

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

                drawSlotItem(event.getGuiGraphics(), x, y, stack, count);

            });


        }

        if (hoveringSlot != null) {
            CoinSlotGroup group = layoutManager.getGroup(hoveringSlot.getId());
            int x0 = group.getGroupX();
            int y0 = group.getGroupY();

            int gridX = group.getSlotX(hoveringSlot.getId());
            int gridY = group.getSlotY(hoveringSlot.getId());
            int x = x0 + gridX * 20 + 2;
            int y = y0 + gridY * 20 + 2;
            AbstractContainerScreen.renderSlotHighlight(event.getGuiGraphics(), x, y, 0, SLOT_COLOR);
        }

        renderPoseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private void drawSlotItem(GuiGraphics graphics, int x, int y, ItemStack stack, int count) {
        Minecraft minecraft = Minecraft.getInstance();

        graphics.renderItem(stack, x, y, 114514, 100);
        // use smaller font if count is too large
        Font font = minecraft.font;

        graphics.pose().pushPose();
        String s = String.valueOf(count);
        graphics.pose().translate(x + 18, y + 18, 300.0D);
        if (count > 999) {
            graphics.pose().scale(0.6F, 0.6F, 0.6F);
        } else if (count > 99) {
            graphics.pose().scale(0.8F, 0.8F, 0.8F);
        }

        graphics.drawString(font, s, (float) (-font.width(s) - 1), -font.lineHeight, 0x00FFFFFF, true);
        ItemDecoratorHandler.of(stack).render(graphics, font, stack, x, y);

        graphics.pose().popPose();


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


    private boolean isStickingSlot(int x, int y) {

        Rectangle2i slotRect = new Rectangle2i(x, y, SLOT_SIZE, SLOT_SIZE);
//        return guiBounds.contains(slotRect) && !slotRect.intersects(guiInnerBounds);
        return guiBounds.contains(slotRect);
    }

    private enum PaintRegion {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
    }


    private SlotKind neighborKind(CoinSlotGroup group, int x0, int y0, int slotX, int slotY, CoinSlotGroup.Neighbour neighbour) {
        SlotKind base = group.getSlotKind(slotX + neighbour.offsetX, slotY + neighbour.offsetY);
        if (base != SlotKind.Empty) {
            return base;
        }


        int x = x0 + neighbour.offsetX * SLOT_SIZE;
        int y = y0 + neighbour.offsetY * SLOT_SIZE;

        if (isStickingSlot(x, y)) {
            return SlotKind.Virtual;
        }

        return SlotKind.Empty;
    }

    private void drawSlot(GuiGraphics graphics, CoinSlotGroup group, int x0, int y0, int slotX, int slotY, int blitOffset) {
        boolean hasSlot = group.hasSlot(slotX, slotY);

        int x = x0;
        int y = y0;

        if (hasSlot) {
            drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.FILL);
            return;
        }

        if (group.hasSlot(slotX, slotY, false) || isStickingSlot(x, y)) {
            return;
        }
        SlotKind up = neighborKind(group, x0, y0, slotX, slotY, CoinSlotGroup.Neighbour.UP);
        SlotKind left = neighborKind(group, x0, y0, slotX, slotY, CoinSlotGroup.Neighbour.LEFT);
        SlotKind right = neighborKind(group, x0, y0, slotX, slotY, CoinSlotGroup.Neighbour.RIGHT);
        SlotKind down = neighborKind(group, x0, y0, slotX, slotY, CoinSlotGroup.Neighbour.DOWN);

        {
            // top left
            boolean upLeft = group.hasSlot(slotX - 1, slotY - 1);

            if (up != SlotKind.Empty) {
                if (left != SlotKind.Empty) {
                    if (left != SlotKind.Virtual || up != SlotKind.Virtual) {
                        int xWidth = up == SlotKind.Real ? 10 : 4;
                        int yHeight = left == SlotKind.Real ? 10 : 4;
                        drawSlotPart(graphics, x, y, 0, 0, xWidth, yHeight, blitOffset, GuiTextures.SlotPart.TOP_LEFT_3);
                    }
                } else if (up == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.TOP_2);
                }
            } else if (left != SlotKind.Empty) {
                if (left == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.LEFT_2);
                }
            } else if (upLeft) {
                drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.TOP_LEFT_1);
            }
        }

        x = x0 + 10;
        y = y0;
        // top right
        {
            boolean upRight = group.hasSlot(slotX + 1, slotY - 1);
            if (up != SlotKind.Empty) {
                if (right != SlotKind.Empty) {
                    if (right != SlotKind.Virtual || up != SlotKind.Virtual) {
                        // top has slot and right has slot
                        int xWidth = up == SlotKind.Real ? 10 : 4;
                        int yHeight = right == SlotKind.Real ? 10 : 4;
                        drawSlotPart(graphics, x, y, 10 - xWidth, 0, xWidth, yHeight, blitOffset, GuiTextures.SlotPart.TOP_RIGHT_3);
                    }
                } else if (up == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.TOP_2);
                }
            } else if (right != SlotKind.Empty) {
                if (right == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.RIGHT_2);
                }
            } else if (upRight) {
                drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.TOP_RIGHT_1);
            }
        }

        x = x0;
        y = y0 + 10;
        // bottom left
        {

            boolean bottomLeft = group.hasSlot(slotX - 1, slotY + 1);

            if (down != SlotKind.Empty) {
                if (left != SlotKind.Empty) {
                    if (left != SlotKind.Virtual || down != SlotKind.Virtual) {
                        int xWidth = down == SlotKind.Real ? 10 : 4;
                        int yHeight = left == SlotKind.Real ? 10 : 4;
                        drawSlotPart(graphics, x, y, 0, 10 - yHeight, xWidth, yHeight, blitOffset, GuiTextures.SlotPart.BOTTOM_LEFT_3);
                    }
                } else if (down == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.BOTTOM_2);
                }
            } else if (left != SlotKind.Empty) {
                if (left == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.LEFT_2);
                }
            } else if (bottomLeft) {
                drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.BOTTOM_LEFT_1);
            }
        }


        // bottom right
        x = x0 + 10;
        y = y0 + 10;

        {

            boolean bottomRight = group.hasSlot(slotX + 1, slotY + 1);

            if (down != SlotKind.Empty) {
                if (right != SlotKind.Empty) {
                    if (right != SlotKind.Virtual || down != SlotKind.Virtual) {
                        int xWidth = down == SlotKind.Real ? 10 : 4;
                        int yHeight = right == SlotKind.Real ? 10 : 4;
                        drawSlotPart(graphics, x, y, 10 - xWidth, 10 - yHeight, xWidth, yHeight, blitOffset, GuiTextures.SlotPart.BOTTOM_RIGHT_3);
                    }
                } else if (down == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.BOTTOM_2);
                }
            } else if (right != SlotKind.Empty) {
                if (right == SlotKind.Real) {
                    drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.RIGHT_2);
                }
            } else if (bottomRight) {
                drawSlotPart(graphics, x, y, blitOffset, GuiTextures.SlotPart.BOTTOM_RIGHT_1);
            }
        }


    }


    private void drawSlotPart(GuiGraphics graphics, int x, int y, int xOff, int yOff, int width, int height, int blitOffset, GuiTextures.SlotPart part) {
        graphics.blit(GuiTextures.COIN_SLOT, x + xOff, y + yOff, blitOffset, part.uOffset + xOff, part.vOffset + yOff, width, height, 40, 40);
    }

    private void drawSlotPart(GuiGraphics graphics, int x, int y, int blitOffset, GuiTextures.SlotPart part) {
        graphics.blit(GuiTextures.COIN_SLOT, x, y, blitOffset, part.uOffset, part.vOffset, part.uWidth, part.vHeight, 40, 40);
    }


}
