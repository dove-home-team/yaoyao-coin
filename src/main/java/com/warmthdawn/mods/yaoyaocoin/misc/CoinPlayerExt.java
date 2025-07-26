package com.warmthdawn.mods.yaoyaocoin.misc;

import com.warmthdawn.mods.yaoyaocoin.kubejs.CoinHelper;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;

public interface CoinPlayerExt {

    @Info("Get the coin inventory for player, only works on server")
    default CoinHelper getCoinInv() {
        return CoinHelper.of(((ServerPlayer) this));
    }


}
