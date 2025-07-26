package com.warmthdawn.mods.yaoyaocoin.mixin;

import com.warmthdawn.mods.yaoyaocoin.misc.CoinPlayerExt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class MixinPlayer implements CoinPlayerExt {

}
