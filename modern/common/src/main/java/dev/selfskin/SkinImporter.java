package dev.selfskin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 게임 창에 끌어다 놓은 png 파일을 skins 폴더로 복사한다. */
public final class SkinImporter {

    public static final class Result {
        public int added;
        public int skipped;
        /** 마지막으로 추가한 파일 이름 (없으면 null) */
        public String lastFile;
        /** 건너뛴 첫 번째 이유 (없으면 null) */
        public Component error;
    }

    private SkinImporter() {}

    public static Result importFiles(List<Path> files, Path skinsDir) {
        Result r = new Result();
        for (Path f : files) importFile(f, skinsDir, r);
        return r;
    }

    private static void importFile(Path f, Path skinsDir, Result r) {
        String name = f.getFileName().toString();
        if (!Files.isRegularFile(f) || !name.toLowerCase().endsWith(".png")) {
            skip(r, L.c("error.notPngFile", name));
            return;
        }
        try {
            Component err = checkSkin(f);
            if (err != null) {
                skip(r, Component.literal(name + ": ").append(err));
                return;
            }
            Path out = uniqueFile(skinsDir, name.substring(0, name.length() - 4));
            Files.copy(f, out);
            r.added++;
            r.lastFile = out.getFileName().toString();
        } catch (Exception e) {
            skip(r, Component.literal(name + ": " + e.getMessage()));
        }
    }

    /** SkinTextures.load와 같은 크기 규칙 */
    private static Component checkSkin(Path f) {
        try (InputStream in = Files.newInputStream(f); NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth(), h = img.getHeight();
            if (w != 64 || (h != 64 && h != 32)) return L.c("error.badSize", w, h);
            return null;
        } catch (Exception e) {
            return L.c("error.notPng");
        }
    }

    private static void skip(Result r, Component why) {
        r.skipped++;
        if (r.error == null) r.error = why;
    }

    /** 같은 이름이 있으면 "이름 (2).png", "이름 (3).png" ... 이름으로 쓸 수 없는 문자가 있으면 "skin" */
    private static Path uniqueFile(Path dir, String base) throws Exception {
        if (SkinConfig.checkName(base) != null) base = "skin";
        String name = base + ".png";
        for (int i = 2; exists(dir, name); i++) name = base + " (" + i + ").png";
        return dir.resolve(name);
    }

    /** 대소문자를 구분하지 않고 확인 (Windows에서 덮어쓰기 방지) */
    private static boolean exists(Path dir, String name) throws Exception {
        try (var s = Files.list(dir)) {
            return s.anyMatch(p -> p.getFileName().toString().equalsIgnoreCase(name));
        }
    }
}
