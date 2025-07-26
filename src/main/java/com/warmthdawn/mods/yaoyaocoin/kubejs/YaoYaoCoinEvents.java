package com.warmthdawn.mods.yaoyaocoin.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;

public class YaoYaoCoinEvents {

    public static EventGroup GROUP = EventGroup.of("CoinEvents");

    public static EventHandler REGISTER_COINS = GROUP.startup("registerCoins", () -> RegistryCoinEventJS.class).hasResult();
//    public static EventHandler COIN_ADDED = GROUP.common("coinInsert", () -> CoinSlotEventJS.class).extra(Extra.STRING).hasResult();
//    public static EventHandler COIN_REMOVED = GROUP.common("coinExtract", () -> CoinSlotEventJS.class).extra(Extra.STRING).hasResult();

}
