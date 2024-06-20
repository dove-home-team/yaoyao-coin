package com.warmthdawn.mods.yaoyaocoin.data;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class CoinManager {

    private final List<CoinType> coinTypes = new ArrayList<>();

    private int[] coins;

    private static CoinManager instance;

    public static CoinManager getInstance() {
        if (instance == null) {
            instance = new CoinManager();
        }
        return instance;
    }


    public void init() {

        this.coinTypes.add(new CoinType(0, "iron", 1, 9999, new ResourceLocation("minecraft", "iron_ingot"), null));
        this.coinTypes.add(new CoinType(1, "gold", 3, 999, new ResourceLocation("minecraft", "gold_ingot"), null));
        this.coinTypes.add(new CoinType(2, "diamond", 10, 100, new ResourceLocation("minecraft", "diamond"), null));

        coins = new int[coinTypes.size()];
        for (int i = 0; i < coins.length; i++) {
            coins[i] = this.coinTypes.get(i).money();
        }
    }


    public CoinType findCoinType(String name) {
        for (CoinType type : coinTypes) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }


    public CoinType getCoinType(int id) {
        if (id < 0 || id >= coinTypes.size()) {
            return null;
        }
        return coinTypes.get(id);
    }

    public int getCoinTypeCount() {
        return coinTypes.size();
    }


    public long getTotalMoney(int[] count) {
        long total = 0;
        for (int i = 0; i < count.length; i++) {
            total += (long) count[i] * coins[i];
        }
        return total;
    }

    public void inspectCoins(int[] count, int inspectIndex) {
        if(count.length != coins.length) {
            throw new IllegalArgumentException("count length must be equal to coins length");
        }

        long total = getTotalMoney(count);

        long max = (int) (total / coins[inspectIndex]);

        CoinType inspectType = getCoinType(inspectIndex);
        max = Math.min(max, inspectType.maxStackSize());


        long remaining = total - max * coins[inspectIndex];

        for(int i = coins.length - 1; i >= 0; i--) {
            int maxTake = count[i];
            if(i == inspectIndex) {
                continue;
            }

            int coinValue = coins[i];

            int available = (int) (remaining / coinValue);

            count[i] = Math.min(maxTake, available);

            remaining = remaining - (long) count[i] * coinValue;
        }

        count[inspectIndex] = (int) max;
    }

    public void computeCoins(long total, int[] count, int takenIndex, int takenCount) {
        if (count.length != coins.length) {
            throw new IllegalArgumentException("count length must be equal to coins length");
        }

        long remaining = total - (long) takenCount * coins[takenIndex];

        if (remaining < 0) {
            return;
        }

        long remainingFromTaken = 0;

        for (int i = coins.length - 1; i >= 0; i--) {
            int maxTake = count[i];
            if (i == takenIndex) {
                remainingFromTaken = remaining;
                continue;
            }

            int coinValue = coins[i];

            int available = (int) (remaining / coinValue);

            count[i] = Math.min(maxTake, available);

            remaining = remaining - (long) count[i] * coinValue;
        }

        if (takenIndex == 0) {
            count[0] = (int) remaining;
        } else if (remaining > 0) {
            // the taken index should rest more

            int takenRemaining = (int) (remaining / coins[takenIndex]);
            remainingFromTaken -= (long) takenRemaining * coins[takenIndex];

            for (int i = takenIndex - 1; i > 0; i--) {
                int maxTake = count[i];
                int coinValue = coins[i];
                int available = (int) (remainingFromTaken / coinValue);

                count[i] = Math.min(maxTake, available);

                remainingFromTaken = remainingFromTaken - (long) count[i] * coinValue;
            }

            count[0] = (int) remainingFromTaken;
        } else {
            count[takenIndex] = 0;
        }
    }


}
