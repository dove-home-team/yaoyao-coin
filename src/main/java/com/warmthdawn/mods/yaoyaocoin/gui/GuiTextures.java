package com.warmthdawn.mods.yaoyaocoin.gui;

import com.warmthdawn.mods.yaoyaocoin.YaoYaoCoin;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class GuiTextures {

    public static final ResourceLocation COIN_SLOT = new ResourceLocation(YaoYaoCoin.MODID, "textures/gui/slot.png");



    enum SlotPart {
        FILL(0, 0, 20, 20),

        TOP_LEFT_3(20, 0, 10, 10),
        TOP_RIGHT_3(30, 0, 10, 10),
        BOTTOM_LEFT_3(20, 10, 10, 10),
        BOTTOM_RIGHT_3(30, 10, 10, 10),

        TOP_2(20, 20, 10, 10),
        BOTTOM_2(30, 20, 10, 10),

        RIGHT_2(20, 30, 10, 10),
        LEFT_2(30, 30, 10, 10),

        BOTTOM_RIGHT_1(0, 20, 10, 10),
        BOTTOM_LEFT_1(10, 20, 10, 10),
        TOP_RIGHT_1(0, 30, 10, 10),
        TOP_LEFT_1(10, 30, 10, 10)

        ;


        public final int uOffset;
        public final int vOffset;
        public final int uWidth;
        public final int vHeight;

        SlotPart(int uOffset, int vOffset, int uWidth, int vHeight) {
            this.uOffset = uOffset;
            this.vOffset = vOffset;
            this.uWidth = uWidth;
            this.vHeight = vHeight;
        }


    }

}
