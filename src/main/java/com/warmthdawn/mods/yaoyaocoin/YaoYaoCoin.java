package com.warmthdawn.mods.yaoyaocoin;

import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.gui.ClientCoinStorage;
import com.warmthdawn.mods.yaoyaocoin.gui.CoinGuiHandler;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("yaoyaocoin")
public class YaoYaoCoin {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MODID = "yaoyaocoin";

    public YaoYaoCoin() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
        CoinGuiHandler guiHandler = new CoinGuiHandler();
        guiHandler.initialize(MinecraftForge.EVENT_BUS);

        YaoYaoCoinNetwork.init();
    }

    private void setup(final FMLCommonSetupEvent event) {
        CoinManager.getInstance().init();


        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientCoinStorage.INSTANCE::init);
    }

}
