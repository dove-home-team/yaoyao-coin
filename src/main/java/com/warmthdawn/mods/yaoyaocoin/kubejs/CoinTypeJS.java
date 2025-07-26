package com.warmthdawn.mods.yaoyaocoin.kubejs;

import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;

public class CoinTypeJS {

  public static CoinType of(Object obj) {
    if (obj instanceof CoinType) {
      return (CoinType) obj;
    }
    if (obj == null) {
      return null;
    }

    String name = String.valueOf(obj);
    CoinManager manager = CoinManager.getInstance();
    return manager.findCoinType(name);
  }
}
