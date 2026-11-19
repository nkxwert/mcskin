package dev.selfskin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * config/selfskin.json
 * {
 *   "enabled": true,
 *   "activeFile": "myskin.png",
 *   "skins": [ { "name": "myskin", "file": "myskin.png", "model": "slim", "source": "file" } ]
 * }
 */
public class SkinConfig {

    public static final String MODEL_CLASSIC = "default";
    public static final String MODEL_SLIM = "slim";

    public static class SkinEntry {
        public String name;
        public String file;
        public String model = MODEL_CLASSIC;
        public String source = "file"; // "file" | "username"

        public SkinEntry() {}

        public SkinEntry(String name, String file, String model, String source) {
            this.name = name;
            this.file = file;
            this.model = model;
            this.source = source;
        }
    }

    public boolean enabled = false;
    public String activeFile = null;
    /** 이전 버전 설정(갑옷 전체 숨김). 불러올 때 부위별 설정으로 옮긴다. */
    public boolean hideArmor = false;
    /** 내 캐릭터의 갑옷을 부위별로 나에게만 숨긴다 (스킨 적용 여부와 무관) */
    public boolean hideHelmet = false;
    public boolean hideChestplate = false;
    public boolean hideLeggings = false;
    public boolean hideBoots = false;
    /** 머리에 쓴 블록(호박, 머리, 블록 등)을 나에게만 숨긴다 */
    public boolean hideHeadBlock = false;

    /** 갑옷 칸 번호 (InventoryPlayer.armorInventory 순서) */
    public static final int SLOT_BOOTS = 0, SLOT_LEGGINGS = 1, SLOT_CHESTPLATE = 2, SLOT_HELMET = 3;
    public static final String[] SLOT_NAMES = {"부츠", "레깅스", "흉갑", "투구"};

    public boolean isArmorHidden(int slot) {
        switch (slot) {
            case SLOT_HELMET: return hideHelmet;
            case SLOT_CHESTPLATE: return hideChestplate;
            case SLOT_LEGGINGS: return hideLeggings;
            case SLOT_BOOTS: return hideBoots;
            default: return false;
        }
    }

    public void setArmorHidden(int slot, boolean hide) {
        switch (slot) {
            case SLOT_HELMET: hideHelmet = hide; break;
            case SLOT_CHESTPLATE: hideChestplate = hide; break;
            case SLOT_LEGGINGS: hideLeggings = hide; break;
            case SLOT_BOOTS: hideBoots = hide; break;
        }
    }

    public boolean isAnyArmorHidden() {
        return hideHelmet || hideChestplate || hideLeggings || hideBoots;
    }

    public boolean isAllArmorHidden() {
        return hideHelmet && hideChestplate && hideLeggings && hideBoots;
    }
    public List<SkinEntry> skins = new ArrayList<SkinEntry>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static transient File configFile;
    private static transient File skinsDir;

    public static SkinConfig load(File mcDir) {
        configFile = new File(new File(mcDir, "config"), "selfskin.json");
        skinsDir = new File(new File(mcDir, "selfskin"), "skins");
        if (!skinsDir.exists()) skinsDir.mkdirs();

        SkinConfig cfg = null;
        if (configFile.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                cfg = GSON.fromJson(r, SkinConfig.class);
            } catch (Exception e) {
                SelfSkinMod.LOG.warn("selfskin.json 읽기 실패, 기본값 사용", e);
            }
        }
        if (cfg == null) cfg = new SkinConfig();
        if (cfg.skins == null) cfg.skins = new ArrayList<SkinEntry>();
        if (cfg.hideArmor) {
            // 이전 버전에서 "갑옷 숨김"을 켜 뒀다면 모든 부위를 숨김으로 옮긴다
            for (int slot = 0; slot < 4; slot++) cfg.setArmorHidden(slot, true);
            cfg.hideArmor = false;
        }
        cfg.syncWithFolder();
        return cfg;
    }

    public void save() {
        try {
            configFile.getParentFile().mkdirs();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            SelfSkinMod.LOG.warn("selfskin.json 저장 실패", e);
        }
    }

    public static File getSkinsDir() {
        return skinsDir;
    }

    /** 폴더에 새로 들어온 png는 추가하고, 지워진 파일의 항목은 제거한다. */
    public void syncWithFolder() {
        Set<String> onDisk = new HashSet<String>();
        File[] files = skinsDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".png")) onDisk.add(f.getName());
            }
        }
        Iterator<SkinEntry> it = skins.iterator();
        Set<String> known = new HashSet<String>();
        while (it.hasNext()) {
            SkinEntry e = it.next();
            if (e.file == null || !onDisk.contains(e.file)) it.remove();
            else known.add(e.file);
        }
        List<String> added = new ArrayList<String>(onDisk);
        Collections.sort(added, String.CASE_INSENSITIVE_ORDER); // HashSet 순서는 매번 달라지므로 정렬
        for (String file : added) {
            if (!known.contains(file)) {
                String name = file.substring(0, file.length() - 4);
                skins.add(new SkinEntry(name, file, MODEL_CLASSIC, "file"));
            }
        }
        if (activeFile != null && find(activeFile) == null) {
            activeFile = null;
            enabled = false;
        }
        save();
    }

    public SkinEntry find(String file) {
        if (file == null) return null;
        for (SkinEntry e : skins) if (file.equals(e.file)) return e;
        return null;
    }

    public SkinEntry findIgnoreCase(String file) {
        SkinEntry exact = find(file);
        if (exact != null || file == null) return exact;
        for (SkinEntry e : skins) if (file.equalsIgnoreCase(e.file)) return e;
        return null;
    }

    public SkinEntry getActive() {
        return find(activeFile);
    }

    public void upsert(SkinEntry entry) {
        SkinEntry old = find(entry.file);
        if (old != null) skins.remove(old);
        skins.add(entry);
        save();
    }
}
