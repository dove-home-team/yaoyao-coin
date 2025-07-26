package com.warmthdawn.mods.yaoyaocoin.config;


import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class CoinSaveState {


    private static final Logger logger = LogUtils.getLogger();
    private static final String SETTINGS_FILE = "CoinScreenLayout.json";
    private static final Gson gson = new Gson();

    public void addSlot(Slot saveSlot) {
        slots.add(saveSlot);
    }

    public void addGroup(Group saveGroup) {
        groups.add(saveGroup);
    }

    public static class Group {
        public int id;
        public int horizontal;
        public int vertical;
        public CoinLayoutArea area;
        public boolean visible = true;
    }

    public static class Slot {
        public int x;
        public int y;
        public String name;
        public int groupId;
    }

    private static final CoinSaveState _instance = new CoinSaveState();

    public static CoinSaveState instance() {
        return _instance;
    }


    private final List<Group> groups = new ArrayList<>();
    private final List<Slot> slots = new ArrayList<>();

    // not serialized with gson
    private transient boolean loaded = false;

    public void save() {

        logger.info("Saving Coin Screen Layout");


        File saveStateFile = new File(Minecraft.getInstance().gameDirectory, SETTINGS_FILE);

        try (FileWriter writer = new FileWriter(saveStateFile)) {
            gson.toJson(this, writer);
        } catch (Exception e) {
            logger.error("Failed to save Coin Screen Layout", e);
        }

    }


    public void clear() {
        groups.clear();
        slots.clear();
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<Slot> getSlots() {
        return slots;
    }


    public void load() {
        if(loaded) {
            return;
        }
        logger.info("Loading Coin Screen Layout");

        File saveStateFile = new File(Minecraft.getInstance().gameDirectory, SETTINGS_FILE);

        if (!saveStateFile.exists()) {
            logger.info("No Coin Screen Layout found");
            loaded = true;
            return;
        }


        try (FileReader reader = new FileReader(saveStateFile)) {
            CoinSaveState state = gson.fromJson(reader, CoinSaveState.class);
            if (state != null) {
                groups.clear();
                groups.addAll(state.groups);
                slots.clear();
                slots.addAll(state.slots);
            }
        } catch (Exception e) {
            logger.error("Failed to load Coin Screen Layout", e);
        }

        loaded = true;
    }
}
