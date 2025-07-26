package com.warmthdawn.mods.yaoyaocoin;

import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.command.CoinCommand;
import com.warmthdawn.mods.yaoyaocoin.command.CoinNameArgument;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.gui.ClientCoinStorage;
import com.warmthdawn.mods.yaoyaocoin.gui.CoinGuiHandler;
import com.warmthdawn.mods.yaoyaocoin.network.YaoYaoCoinNetwork;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("yaoyaocoin")
public class YaoYaoCoin {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MODID = "yaoyaocoin";

    // 指令参数类型注册
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
        DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, MODID);

    static {
        ARGUMENT_TYPES.register("coin_name", () -> ArgumentTypeInfos.registerByClass(CoinNameArgument.class, SingletonArgumentInfo.contextFree(CoinNameArgument::coinName)));
    }

    public YaoYaoCoin() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        ARGUMENT_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
        CoinGuiHandler guiHandler = new CoinGuiHandler();
        guiHandler.initialize(MinecraftForge.EVENT_BUS);

        YaoYaoCoinNetwork.init();
    }

    private void setup(final FMLCommonSetupEvent event) {
        CoinManager.getInstance().init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientCoinStorage.INSTANCE::init);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CoinCommand.register(event.getDispatcher());
    }

}
