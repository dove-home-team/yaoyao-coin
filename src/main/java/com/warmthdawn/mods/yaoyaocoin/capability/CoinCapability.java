package com.warmthdawn.mods.yaoyaocoin.capability;

import com.warmthdawn.mods.yaoyaocoin.YaoYaoCoin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.items.IItemHandlerModifiable;

public class CoinCapability {
    public static final ResourceLocation ID_COIN_INVENTORY = new ResourceLocation(YaoYaoCoin.MODID, "coin_inventory");

    public static final Capability<IItemHandlerModifiable> COIN_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {});


}
