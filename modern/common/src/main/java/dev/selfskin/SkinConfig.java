package dev.selfskin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * config/selfskin.json (1.8.9 버전과 같은 형식)
 * 스킨 png는 selfskin/skins/ 폴더에 둔다.
 */
public class SkinConfig {

    public static final String MODEL_CLASSIC = "default";
    public static final String MODEL_SLIM = "slim";

    /** 갑옷 칸 번호 (아래부터 부츠, 레깅스, 흉갑, 투구) */
    public static final int SLOT_BOOTS = 0, SLOT_LEGGINGS = 1, SLOT_CHESTPLATE = 2, SLOT_HELMET = 3;

    public static final int MAX_NAME_LENGTH = 32;

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

        public boolean isSlim() {
            return MODEL_SLIM.equals(model);
        }
    }

    public boolean enabled = false;
    public String activeFile = null;
    public boolean hideHelmet = false;
    public boolean hideChestplate = false;
    public boolean hideLeggings = false;
    public boolean hideBoots = false;
    public boolean hideHeadBlock = false;
    public boolean hideCape = false;
    public List<SkinEntry> skins = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static transient Path configFile;
    private static transient Path skinsDir;

    public static SkinConfig load(Path gameDir) {
        configFile = gameDir.resolve("config").resolve("selfskin.json");
        skinsDir = gameDir.resolve("selfskin").resolve("skins");
        try {
            Files.createDirectories(skinsDir);
        } catch (IOException e) {
            SelfSkin.LOG.warn("Failed to create skins folder", e);
        }

        SkinConfig cfg = null;
        if (Files.exists(configFile)) {
            try (Reader r = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                cfg = GSON.fromJson(r, SkinConfig.class);
            } catch (Exception e) {
                SelfSkin.LOG.warn("Failed to read selfskin.json, using defaults", e);
            }
        }
        if (cfg == null) cfg = new SkinConfig();
        if (cfg.skins == null) cfg.skins = new ArrayList<>();
        cfg.syncWithFolder();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer w = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            SelfSkin.LOG.warn("Failed to save selfskin.json", e);
        }
    }

    public static Path getSkinsDir() {
        return skinsDir;
    }

    // ---------- 갑옷 / 머리 블록 / 망토 ----------

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
            default: break;
        }
    }

    public boolean isAllArmorHidden() {
        return hideHelmet && hideChestplate && hideLeggings && hideBoots;
    }

    public void setAllHidden(boolean hide) {
        for (int slot = 0; slot < 4; slot++) setArmorHidden(slot, hide);
        hideHeadBlock = hide;
        hideCape = hide;
    }

    // ---------- 스킨 목록 ----------

    /** 폴더에 새로 들어온 png는 추가하고, 지워진 파일의 항목은 제거한다. 바뀐 게 있을 때만 저장한다. */
    public void syncWithFolder() {
        Set<String> onDisk = new HashSet<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(skinsDir, "*.{png,PNG}")) {
            for (Path p : ds) if (Files.isRegularFile(p)) onDisk.add(p.getFileName().toString());
        } catch (IOException e) {
            SelfSkin.LOG.warn("Failed to list skins folder", e);
        }
        boolean changed = false;
        Set<String> known = new HashSet<>();
        Iterator<SkinEntry> it = skins.iterator();
        while (it.hasNext()) {
            SkinEntry e = it.next();
            if (e.file == null || !onDisk.contains(e.file)) {
                it.remove();
                changed = true;
            } else {
                known.add(e.file);
            }
        }
        List<String> added = new ArrayList<>(onDisk);
        added.sort(String.CASE_INSENSITIVE_ORDER);
        for (String file : added) {
            if (!known.contains(file)) {
                skins.add(new SkinEntry(file.substring(0, file.length() - 4), file, MODEL_CLASSIC, "file"));
                changed = true;
            }
        }
        if (activeFile != null && find(activeFile) == null) {
            activeFile = null;
            enabled = false;
            changed = true;
        }
        if (changed) save();
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

    /** 목록에서 순서를 옮긴다 (delta: -1 위로, +1 아래로). 옮겼으면 true */
    public boolean move(SkinEntry e, int delta) {
        int from = skins.indexOf(e);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= skins.size()) return false;
        Collections.swap(skins, from, to);
        save();
        return true;
    }

    // ---------- 이름 바꾸기 / 삭제 ----------

    private static final String BAD_CHARS = "\\/:*?\"<>|§";
    private static final String[] RESERVED = {"CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    /** 스킨 이름 검사. 문제가 있으면 번역 키, 없으면 null */
    public static String checkName(String name) {
        if (name == null || name.trim().isEmpty()) return "error.nameEmpty";
        if (!name.equals(name.trim()) || name.endsWith(".")) return "error.nameEdges";
        if (name.length() > MAX_NAME_LENGTH) return "error.nameTooLong";
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 32 || BAD_CHARS.indexOf(c) >= 0) return "error.nameBadChars";
        }
        for (String r : RESERVED) if (r.equalsIgnoreCase(name)) return "error.nameReserved";
        return null;
    }

    /** 스킨 이름(=파일 이름)을 바꾼다. 실패하면 번역 키 */
    public String rename(SkinEntry e, String newName) {
        String err = checkName(newName);
        if (err != null) return err;
        String newFile = newName + ".png";
        if (newFile.equals(e.file)) {
            e.name = newName;
            save();
            return null;
        }
        for (SkinEntry o : skins) {
            if (o != e && newFile.equalsIgnoreCase(o.file)) return "error.nameDuplicate";
        }
        Path from = skinsDir.resolve(e.file);
        Path to = skinsDir.resolve(newFile);
        // 대소문자만 바꾸는 경우 Windows에서는 to가 자기 자신을 가리키므로 제외
        if (Files.exists(to) && !newFile.equalsIgnoreCase(e.file)) return "error.fileExists";
        try {
            Files.move(from, to);
        } catch (IOException ex) {
            return "error.renameFailed";
        }
        if (e.file.equals(activeFile)) activeFile = newFile;
        e.file = newFile;
        e.name = newName;
        save();
        return null;
    }

    /** 스킨 파일을 지우고 목록에서 뺀다. 실패하면 번역 키 */
    public String delete(SkinEntry e) {
        try {
            Files.deleteIfExists(skinsDir.resolve(e.file));
        } catch (IOException ex) {
            return "error.deleteFailed";
        }
        skins.remove(e);
        if (e.file.equals(activeFile)) {
            activeFile = null;
            enabled = false;
        }
        save();
        return null;
    }
}
