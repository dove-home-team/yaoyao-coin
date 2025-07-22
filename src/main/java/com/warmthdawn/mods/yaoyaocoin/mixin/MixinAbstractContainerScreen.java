package com.warmthdawn.mods.yaoyaocoin.mixin;

import com.warmthdawn.mods.yaoyaocoin.event.ScreenTooltipEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void inject_renderTooltip(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new ScreenTooltipEvent((AbstractContainerScreen<?>) (Object) this, guiGraphics, x, y));
    }

}
