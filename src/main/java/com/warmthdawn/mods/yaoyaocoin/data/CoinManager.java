package com.warmthdawn.mods.yaoyaocoin.data;

import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.config.CoinDefine;
import com.warmthdawn.mods.yaoyaocoin.config.LayoutArea;
import com.warmthdawn.mods.yaoyaocoin.kubejs.CoinEventDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

public class CoinManager {
    private static final Logger logger = LogUtils.getLogger();
    private final List<CoinType> coinTypes = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(0);


    private final Map<Integer, int[]> coinsMap = new HashMap<>();
    private final Map<Integer, List<CoinType>> convertMap = new HashMap<>();

    private static CoinManager instance;

    public static CoinManager getInstance() {
        if (instance == null) {
            instance = new CoinManager();
        }
        return instance;
    }

    private int[] getCoins(int convertGroup) {
        return coinsMap.getOrDefault(convertGroup, new int[0]);
    }


    private void loadDefine() {

        // load by kubejs
        boolean firstInit = true;
        List<IntFunction<CoinType>> builders = new LinkedList<>();
        AtomicBoolean cancel = new AtomicBoolean(false);

        if(ModList.get().isLoaded("kubejs")) {
            if(CoinEventDispatcher.initCoins(builders, cancel)) {
                firstInit = false;
                for (IntFunction<CoinType> builder : builders) {
                    CoinType type = builder.apply(nextId.getAndIncrement());
                    coinTypes.add(type);
                }
            }

        }

        if (cancel.get()) {
            return;
        }

        if(!loadJsonDefine()) {
            firstInit = false;
        }

        if (firstInit) {
            logger.info("No coin define found, using default define");
            CoinDefine.instance().outputDefault();
            loadJsonDefine();
        }

    }

    private boolean loadJsonDefine() {
        boolean firstInit = CoinDefine.instance().load();
        if (CoinDefine.instance().getCoinTypes() == null) {
            return firstInit;
        }

        for (CoinDefine.CoinType coinType : CoinDefine.instance().getCoinTypes()) {
            // convert string to tag
            CompoundTag tag = null;

            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(coinType.itemName));
            if (item == null) {
                logger.error("Failed to find item for coin type {}", coinType.name);
                continue;
            }

            ItemStack stack = new ItemStack(item);
            stack.setCount(1);

            if (coinType.itemTag != null) {
                try {
                    tag = TagParser.parseTag(coinType.itemTag);
                    stack.setTag(tag);
                } catch (Exception e) {
                    logger.error("Failed to parse item tag for coin type {}", coinType.name);
                    e.printStackTrace();
                }
            }
            if (coinType.defaultArea == null) {
                coinType.defaultArea = LayoutArea.TOP_LEFT;
            }
            coinTypes.add(new CoinType(nextId.getAndIncrement(), coinType.name, coinType.money, coinType.convertGroup, coinType.maxStackSize, coinType.hideDefault, stack, coinType.defaultArea));
        }

        return firstInit;
    }

    public void init() {
        loadDefine();

        convertMap.clear();
        for (CoinType type : coinTypes) {
            convertMap.computeIfAbsent(type.convertGroup(), k -> new ArrayList<>()).add(type);
        }

        coinsMap.clear();
        for (Map.Entry<Integer, List<CoinType>> entry : convertMap.entrySet()) {
            int convertGroup = entry.getKey();
            List<CoinType> types = entry.getValue();

            types.sort(Comparator.comparingInt(CoinType::money));

            int[] coins = new int[types.size()];
            for (int i = 0; i < types.size(); i++) {
                coins[i] = types.get(i).money();
            }


            coinsMap.put(convertGroup, coins);
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

    public List<CoinType> getCoinTypes() {
        return Collections.unmodifiableList(coinTypes);
    }


    public long getTotalMoneyInGroup(int[] count, int convertGroup) {
        if (count.length != coinTypes.size()) {
            throw new IllegalArgumentException("count length must be equal to coinTypes length");
        }
        long total = 0;
        for (int i = 0; i < coinTypes.size(); i++) {
            CoinType type = getCoinType(i);
            if (type.convertGroup() == convertGroup) {
                total += (long) count[i] * type.money();
            }
        }
        return total;
    }


    public void inspectCoins(int[] coinCounts, int inspectIndex) {

        CoinType inspectType = getCoinType(inspectIndex);
        int convertGroup = inspectType.convertGroup();

        List<CoinType> convertTypes = convertMap.get(convertGroup);

        int[] counts = new int[convertTypes.size()];

        int mappedId = -1;

        for (int i = 0; i < convertTypes.size(); i++) {
            int coinId = convertTypes.get(i).id();
            counts[i] = coinCounts[coinId];
            if (coinId == inspectIndex) {
                mappedId = i;
            }
        }

        inspectCoinsInGroup(counts, mappedId, convertGroup);

        // map back
        for (int i = 0; i < convertTypes.size(); i++) {
            int coinId = convertTypes.get(i).id();
            coinCounts[coinId] = counts[i];
        }
    }

    public void computeCoins(long totalMoney, int[] coinCounts, int inspectIndex, int count) {

        CoinType inspectType = getCoinType(inspectIndex);
        int convertGroup = inspectType.convertGroup();

        List<CoinType> convertTypes = convertMap.get(convertGroup);

        int[] counts = new int[convertTypes.size()];

        int mappedId = -1;

        for (int i = 0; i < convertTypes.size(); i++) {
            int coinId = convertTypes.get(i).id();
            if (coinId == inspectIndex) {
                mappedId = i;
            }
            counts[i] = coinCounts[coinId];
        }
        computeCoinsInGroup(totalMoney, counts, mappedId, count, convertGroup);

        // map back
        for (int i = 0; i < convertTypes.size(); i++) {
            int coinId = convertTypes.get(i).id();
            coinCounts[coinId] = counts[i];
        }
    }

    public void inspectCoinsInGroup(int[] count, int inspectIndex, int convertGroup) {
        int[] coins = getCoins(convertGroup);
        if (count.length != coins.length) {
            throw new IllegalArgumentException("count length must be equal to coins length");
        }

        long total = 0;
        for (int i = 0; i < count.length; i++) {
            total += (long) count[i] * coins[i];
        }

        long max = (int) (total / coins[inspectIndex]);

        CoinType inspectType = getCoinType(inspectIndex);
        max = Math.min(max, inspectType.maxStackSize());


        long remaining = total - max * coins[inspectIndex];

        for (int i = coins.length - 1; i >= 0; i--) {
            int maxTake = count[i];
            if (i == inspectIndex) {
                continue;
            }

            int coinValue = coins[i];

            int available = (int) (remaining / coinValue);

            count[i] = Math.min(maxTake, available);

            remaining = remaining - (long) count[i] * coinValue;
        }

        count[inspectIndex] = (int) max;
    }

    public void computeCoinsInGroup(long total, int[] count, int takenIndex, int takenCount, int convertGroup) {
        int[] coins = getCoins(convertGroup);
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
            count[takenIndex] = takenRemaining;
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
