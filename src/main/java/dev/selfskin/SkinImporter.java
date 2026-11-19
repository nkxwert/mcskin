package dev.selfskin;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * 클립보드에서 스킨 png를 가져와 skins 폴더에 넣는다. (메뉴에서 Ctrl+V)
 * 1.8.9(LWJGL 2) 창은 파일 끌어놓기를 받을 수 없어서, 탐색기에서 복사(Ctrl+C)한 파일을 붙여넣는 방식으로 대신한다.
 */
public final class SkinImporter {

    public static final class Result {
        public int added;
        public int skipped;
        /** 마지막으로 추가한 파일 이름 (없으면 null) */
        public String lastFile;
        /** 건너뛴 이유 또는 전체 실패 이유 (없으면 null) */
        public String error;
    }

    private SkinImporter() {}

    public static Result fromClipboard(File skinsDir) {
        Result r = new Result();
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = cb.getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                for (File f : files) importFile(f, skinsDir, r);
            } else if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                importImage((Image) t.getTransferData(DataFlavor.imageFlavor), skinsDir, r);
            } else {
                r.error = L.t("error.clipboardEmpty");
            }
        } catch (Exception e) {
            r.error = L.t("error.clipboardFailed", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        return r;
    }

    private static void importFile(File f, File skinsDir, Result r) {
        String name = f.getName();
        if (!f.isFile() || !name.toLowerCase().endsWith(".png")) {
            skip(r, L.t("error.notPngFile", name));
            return;
        }
        try {
            String err = checkSkin(ImageIO.read(f));
            if (err != null) {
                skip(r, name + ": " + err);
                return;
            }
            File out = uniqueFile(skinsDir, name.substring(0, name.length() - 4));
            Files.copy(f.toPath(), out.toPath());
            r.added++;
            r.lastFile = out.getName();
        } catch (Exception e) {
            skip(r, name + ": " + e.getMessage());
        }
    }

    /** 브라우저 등에서 "이미지 복사"한 경우 */
    private static void importImage(Image img, File skinsDir, Result r) throws Exception {
        if (!(img instanceof BufferedImage)) {
            r.error = L.t("error.notPng");
            return;
        }
        BufferedImage bi = (BufferedImage) img;
        // Windows 클립보드 이미지는 투명도가 사라지는 경우가 많다. 투명도 없이 저장하면 겉옷 레이어가 검게 덮여 스킨이 깨진다.
        if (!bi.getColorModel().hasAlpha()) {
            r.error = L.t("error.clipboardNoAlpha");
            return;
        }
        String err = checkSkin(bi);
        if (err != null) {
            r.error = err;
            return;
        }
        File out = uniqueFile(skinsDir, "pasted");
        ImageIO.write(bi, "png", out);
        r.added++;
        r.lastFile = out.getName();
    }

    /** SkinManager.load와 같은 크기 규칙 */
    private static String checkSkin(BufferedImage img) {
        if (img == null) return L.t("error.notPng");
        if (img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) {
            return L.t("error.badSize", img.getWidth(), img.getHeight());
        }
        return null;
    }

    private static void skip(Result r, String why) {
        r.skipped++;
        if (r.error == null) r.error = why;
    }

    /** 같은 이름이 있으면 "이름 (2).png", "이름 (3).png" ... 이름으로 쓸 수 없는 문자가 있으면 "skin" */
    private static File uniqueFile(File dir, String base) {
        if (SkinConfig.checkName(base) != null) base = "skin";
        File f = new File(dir, base + ".png");
        for (int i = 2; exists(dir, f.getName()); i++) {
            f = new File(dir, base + " (" + i + ").png");
        }
        return f;
    }

    /** 대소문자를 구분하지 않고 확인 (Windows에서 덮어쓰기 방지) */
    private static boolean exists(File dir, String name) {
        String[] names = dir.list();
        if (names == null) return false;
        for (String n : names) if (n.equalsIgnoreCase(name)) return true;
        return false;
    }
}
