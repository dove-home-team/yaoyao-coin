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
        public boolean hideDefault;
        public String itemName;
        public String itemTag;
        public LayoutArea defaultArea;
    }


    private static final CoinDefine _instance = new CoinDefine();

    public static CoinDefine instance() {
        return _instance;
    }

    private CoinType[] coinTypes;

    public CoinType[] getCoinTypes() {
        return coinTypes;
    }

    static final String DEFAULT = """
              {
              "coinTypes": [
                {
                  "name": "iron",
                  "money": 1,
                  "convertGroup": 0,
                  "maxStackSize": 999,
                  "itemName": "minecraft:iron_ingot"
                },
                {
                  "name": "gold",
                  "money": 3,
                  "convertGroup": 0,
                  "maxStackSize": 999,
                  "itemName": "minecraft:gold_ingot"
                },
                {
                  "name": "diamond",
                  "money": 10,
                  "convertGroup": 0,
                  "maxStackSize": 999,
                  "itemName": "minecraft:diamond"
                },
                {
                  "name": "emerald",
                  "money": 1,
                  "convertGroup": 1,
                  "maxStackSize": 999,
                  "itemName": "minecraft:emerald"
                },
                {
                  "name": "netherite",
                  "money": 100,
                  "convertGroup": 1,
                  "maxStackSize": 999,
                  "hideDefault": true,
                  "itemName": "minecraft:netherite_ingot"
                },
                {
                  "name": "special",
                  "money": 1,
                  "convertGroup": 0,
                  "maxStackSize": 500,
                  "itemName": "minecraft:tipped_arrow",
                  "itemTag": "{Potion:\\"minecraft:strength\\"}",
                  "defaultArea": "BOTTOM_RIGHT",
                }
              ]
            }
            """;

    public void outputDefault() {
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, SETTINGS_FILE);
            FileWriter writer = new FileWriter(file);
            writer.write(DEFAULT);
            writer.close();
        } catch (Exception e) {
            logger.error("Failed to output default coin define data", e);
        }
    }

    public boolean load() {
        boolean firstInit = false;
        try {
            File file = new File(Minecraft.getInstance().gameDirectory, SETTINGS_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                CoinDefine define = gson.fromJson(reader, CoinDefine.class);
                reader.close();
                if (define != null && define.coinTypes != null) {
                    coinTypes = define.coinTypes;
                } else {
                    logger.error("Failed to load coin define data: missing 'coinTypes' macro");
                }
            } else {
                firstInit = true;
            }
        } catch (Exception e) {
            logger.error("Failed to load coin define data", e);
        }
        return firstInit;
    }
}
