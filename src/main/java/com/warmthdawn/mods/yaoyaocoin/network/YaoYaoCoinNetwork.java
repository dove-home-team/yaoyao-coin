package com.warmthdawn.mods.yaoyaocoin.network;

import com.warmthdawn.mods.yaoyaocoin.YaoYaoCoin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class YaoYaoCoinNetwork {

    public static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(YaoYaoCoin.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        int id = 0;
        INSTANCE.messageBuilder(PacketSyncCoin.class, id++)
                .encoder(PacketSyncCoin::encoder)
                .decoder(PacketSyncCoin::decoder)
                .consumerMainThread(PacketSyncCoin::messageConsumer)
                .add();

        INSTANCE.messageBuilder(PacketCoinSlotClicked.class, id++)
                .encoder(PacketCoinSlotClicked::encoder)
                .decoder(PacketCoinSlotClicked::decoder)
                .consumerMainThread(PacketCoinSlotClicked::handle)
                .add();

        INSTANCE.messageBuilder(PacketSyncCoinSingle.class, id++)
                .encoder(PacketSyncCoinSingle::encoder)
                .decoder(PacketSyncCoinSingle::decoder)
                .consumerMainThread(PacketSyncCoinSingle::messageConsumer)
                .add();
    }
}
