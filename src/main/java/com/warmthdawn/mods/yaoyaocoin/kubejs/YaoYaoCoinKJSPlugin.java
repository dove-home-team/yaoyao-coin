package com.warmthdawn.mods.yaoyaocoin.kubejs;

import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import com.warmthdawn.mods.yaoyaocoin.event.CoinInitEvent;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import net.minecraftforge.common.MinecraftForge;

public class YaoYaoCoinKJSPlugin extends KubeJSPlugin {


    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCoin);
    }

    private void onRegisterCoin(CoinInitEvent event) {

        if (!YaoYaoCoinEvents.REGISTER_COINS.hasListeners()) {
            return;
        }
        RegistryCoinEventJS eventJS = new RegistryCoinEventJS();
        YaoYaoCoinEvents.REGISTER_COINS.post(eventJS);
        for (CoinTypeBuilderJS builder : eventJS.coinBuilders) {
            event.add(builder::build);
        }
        event.setHandled(true);
    }

    @Override
    public void registerTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        typeWrappers.registerSimple(CoinType.class, CoinTypeJS::of);
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("CoinType", CoinTypeJS.class);
    }

    @Override
    public void registerEvents() {
        YaoYaoCoinEvents.GROUP.register();
    }
}
