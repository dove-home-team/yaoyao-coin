package com.warmthdawn.mods.yaoyaocoin.kubejs;

import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import dev.latvian.mods.kubejs.event.EventResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class CoinEventDispatcher {

    public static boolean initCoins(List<IntFunction<CoinType>> builders, AtomicBoolean cancelled) {
        if (!YaoYaoCoinEvents.REGISTER_COINS.hasListeners()) {
            return false;
        }
        RegistryCoinEventJS eventJS = new RegistryCoinEventJS();
        EventResult result = YaoYaoCoinEvents.REGISTER_COINS.post(eventJS);
        for (CoinTypeBuilderJS builder : eventJS.coinBuilders) {
            builders.add(builder::build);
        }

        if(result.interruptFalse()) {
            cancelled.set(true);
        }

        return true;
    }
}
