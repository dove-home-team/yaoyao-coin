package com.warmthdawn.mods.yaoyaocoin.config;


import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

    public enum LayoutArea {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, INVALID
    }

    public static class Group {
        public int id;
        public int horizontal;
        public int vertical;
        public LayoutArea area;
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


    private void initDefault() {
        Group group = new Group();
        group.id = 0;
        group.horizontal = -10;
        group.vertical = 0;
        group.area = LayoutArea.CENTER_LEFT;
        groups.add(group);

        CoinManager manager = CoinManager.getInstance();

        HashMap<Integer, List<Slot>> groupSlots = new HashMap<>();
        for (int i = 0; i < manager.getCoinTypeCount(); i++) {
            CoinType type = manager.getCoinType(i);
            Slot slot = new Slot();
            slot.x = 0;
            slot.y = 0;
            slot.name = type.name();
            slot.groupId = 0;
            slots.add(slot);
            groupSlots.computeIfAbsent(type.convertGroup(), it -> new ArrayList<>()).add(slot);
        }

        List<List<Slot>> groupList = new ArrayList<>(groupSlots.values());

        groupList.sort(Comparator.comparingInt(List::size));

        for (int i = 0; i < groupList.size(); i++) {
            List<Slot> slotList = groupList.get(i);
            // sort by money
            slotList.sort(Comparator.comparingInt(slot -> manager.findCoinType(slot.name).money()));
            for (int j = 0; j < slotList.size(); j++) {
                Slot slot = slotList.get(j);
                slot.x = i;
                slot.y = j;
            }
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
            initDefault();
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
