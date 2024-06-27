package com.warmthdawn.mods.yaoyaocoin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class CoinDefine {

    private static final Logger logger = LogUtils.getLogger();
    private static final String SETTINGS_FILE = "config/YaoYaoCoinDefine.json";
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting().create();

    public static class CoinType {
        public String name;
        public int money;
        public int convertGroup;
        public int maxStackSize;
        public String itemName;
        public String itemTag;
    }


    private static final CoinDefine _instance = new CoinDefine();

    public static CoinDefine instance() {
        return _instance;
    }

    private CoinType[] coinTypes;

    public CoinType[] getCoinTypes() {
        return coinTypes;
    }


    private void initDefault() {
        CoinType coin1 = new CoinType();
        coin1.name = "iron";
        coin1.money = 1;
        coin1.convertGroup = 0;
        coin1.maxStackSize = 999;
        coin1.itemName = "minecraft:iron_ingot";
        coin1.itemTag = null;

        CoinType coin2 = new CoinType();
        coin2.name = "gold";
        coin2.money = 3;
        coin2.convertGroup = 0;
        coin2.maxStackSize = 999;
        coin2.itemName = "minecraft:gold_ingot";

        CoinType coin3 = new CoinType();
        coin3.name = "diamond";
        coin3.money = 10;
        coin3.convertGroup = 0;
        coin3.maxStackSize = 999;
        coin3.itemName = "minecraft:diamond";

        CoinType coin4 = new CoinType();
        coin4.name = "emerald";
        coin4.money = 1;
        coin4.convertGroup = 1;
        coin4.maxStackSize = 999;
        coin4.itemName = "minecraft:emerald";

        CoinType coin5 = new CoinType();
        coin5.name = "netherite";
        coin5.money = 100;
        coin5.convertGroup = 1;
        coin5.maxStackSize = 999;
        coin5.itemName = "minecraft:netherite_ingot";

        coinTypes = new CoinType[]{coin1, coin2, coin3, coin4, coin5};

    }

    public void load() {
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, SETTINGS_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                CoinDefine define = gson.fromJson(reader, CoinDefine.class);
                reader.close();
                if (define != null && define.coinTypes != null) {
                    coinTypes = define.coinTypes;
                } else {
                    initDefault();
                    save();
                }
            } else {
                initDefault();
                save();
            }
        } catch (Exception e) {
            logger.error("Failed to load coin define data", e);
        }
    }

    public void save() {
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, SETTINGS_FILE);
            FileWriter writer = new FileWriter(file);
            gson.toJson(this, writer);
            writer.close();
        } catch (Exception e) {
            logger.error("Failed to save coin define data", e);
        }
    }
}
