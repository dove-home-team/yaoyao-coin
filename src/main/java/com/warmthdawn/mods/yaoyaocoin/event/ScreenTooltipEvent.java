package com.warmthdawn.mods.yaoyaocoin.event;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.eventbus.api.Event;

public class ScreenTooltipEvent extends ContainerScreenEvent.Render {

    public ScreenTooltipEvent(AbstractContainerScreen<?> guiContainer, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super(guiContainer, guiGraphics, mouseX, mouseY);
    }
}
