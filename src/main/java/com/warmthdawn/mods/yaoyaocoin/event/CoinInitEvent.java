package com.warmthdawn.mods.yaoyaocoin.event;

import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import net.minecraftforge.eventbus.api.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.function.IntFunction;

public class CoinInitEvent extends Event {
    private final List<IntFunction<CoinType>> builders;
    private boolean handled = false;

    public CoinInitEvent(List<IntFunction<CoinType>> builders) {
        this.builders = builders;
    }


    public CoinInitEvent() {
        this.builders = new LinkedList<>();
    }

    public List<IntFunction<CoinType>> getBuilders() {
        return builders;
    }

    public void add(IntFunction<CoinType> builder) {
        builders.add(builder);
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }


}
