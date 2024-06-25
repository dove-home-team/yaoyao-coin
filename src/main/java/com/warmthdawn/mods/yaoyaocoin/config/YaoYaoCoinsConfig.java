package com.warmthdawn.mods.yaoyaocoin.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class YaoYaoCoinsConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        COMMON_SPEC = configBuilder.build();

        ForgeConfigSpec.Builder clientConfigBuilder = new ForgeConfigSpec.Builder();
        setupClientConfig(clientConfigBuilder);
        CLIENT_SPEC = clientConfigBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
    }

    private static void setupClientConfig(ForgeConfigSpec.Builder builder) {
    }
}
