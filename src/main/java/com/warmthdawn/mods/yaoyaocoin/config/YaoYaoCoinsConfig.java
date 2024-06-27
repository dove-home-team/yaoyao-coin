package com.warmthdawn.mods.yaoyaocoin.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class YaoYaoCoinsConfig {

    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        COMMON_SPEC = configBuilder.build();

    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {


    }

}
