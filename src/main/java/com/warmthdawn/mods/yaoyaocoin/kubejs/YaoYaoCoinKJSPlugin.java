package com.warmthdawn.mods.yaoyaocoin.kubejs;

import com.warmthdawn.mods.yaoyaocoin.config.CoinLayoutArea;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;

public class YaoYaoCoinKJSPlugin extends KubeJSPlugin {



    @Override
    public void registerTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        typeWrappers.registerSimple(CoinType.class, CoinTypeJS::of);
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("CoinType", CoinTypeJS.class);
        event.add("CoinLayoutArea", CoinLayoutAreaJS.class);
    }

    @Override
    public void registerEvents() {
        YaoYaoCoinEvents.GROUP.register();
    }
}
