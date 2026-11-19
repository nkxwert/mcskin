package dev.selfskin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.Util;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GuiSelfSkin extends GuiScreen {

    private static final int BTN_DONE = 0, BTN_TOGGLE = 1, BTN_MODEL = 2, BTN_FOLDER = 3,
            BTN_REFRESH = 4, BTN_FETCH = 5, BTN_PREV = 10, BTN_NEXT = 11, BTN_ENTRY = 100;

    private int page = 0;
    private int rows;
    private GuiTextField nameField;
    private String status = "";
    private int statusColor = 0xAAAAAA;
    private boolean fetching;
    private boolean dirty;

    private int left, listW = 140, colX, colW = 90, previewX;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        left = width / 2 - 180;
        previewX = width / 2 + 25;
        colX = width / 2 + 90;
        rows = Math.max(3, Math.min(10, (height - 110) / 22));

        String oldText = nameField != null ? nameField.getText() : "";
        nameField = new GuiTextField(50, fontRendererObj, left + 1, height - 28, listW - 2, 18);
        nameField.setMaxStringLength(16);
        nameField.setText(oldText);
        rebuildButtons();
    }

    private void rebuildButtons() {
        buttonList.clear();
        List<SkinConfig.SkinEntry> list = SelfSkinMod.config.skins;
        int pages = Math.max(1, (list.size() + rows - 1) / rows);
        if (page >= pages) page = pages - 1;

        for (int i = 0; i < rows; i++) {
            int idx = page * rows + i;
            if (idx >= list.size()) break;
            SkinConfig.SkinEntry e = list.get(idx);
            boolean isActive = e.file.equals(SelfSkinMod.config.activeFile);
            String label = (isActive ? "§a● " : "") + e.name + " §7" + (SkinConfig.MODEL_SLIM.equals(e.model) ? "[A]" : "[S]");
            label = fontRendererObj.trimStringToWidth(label, listW - 10);
            buttonList.add(new GuiButton(BTN_ENTRY + idx, left, 32 + i * 22, listW, 20, label));
        }
        int pageY = 32 + rows * 22;
        GuiButton prev = new GuiButton(BTN_PREV, left, pageY, 30, 20, "<");
        GuiButton next = new GuiButton(BTN_NEXT, left + listW - 30, pageY, 30, 20, ">");
        prev.enabled = page > 0;
        next.enabled = page < pages - 1;
        buttonList.add(prev);
        buttonList.add(next);

        SkinConfig.SkinEntry active = SelfSkinMod.config.getActive();
        GuiButton toggle = new GuiButton(BTN_TOGGLE, colX, 32, colW, 20,
                "적용: " + (SelfSkinMod.config.enabled ? "§a켜짐" : "§c꺼짐"));
        toggle.enabled = active != null;
        GuiButton model = new GuiButton(BTN_MODEL, colX, 56, colW, 20,
                "체형: " + (active != null && SkinConfig.MODEL_SLIM.equals(active.model) ? "Alex" : "Steve"));
        model.enabled = active != null;
        buttonList.add(toggle);
        buttonList.add(model);
        buttonList.add(new GuiButton(BTN_FOLDER, colX, 80, colW, 20, "스킨 폴더 열기"));
        buttonList.add(new GuiButton(BTN_REFRESH, colX, 104, colW, 20, "새로고침"));

        GuiButton fetch = new GuiButton(BTN_FETCH, left + listW + 4, height - 29, 60, 20, "가져오기");
        fetch.enabled = !fetching;
        buttonList.add(fetch);
        buttonList.add(new GuiButton(BTN_DONE, colX, height - 29, colW, 20, "완료"));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        SkinConfig cfg = SelfSkinMod.config;
        if (b.id >= BTN_ENTRY) {
            SkinConfig.SkinEntry e = cfg.skins.get(b.id - BTN_ENTRY);
            String err = SelfSkinMod.apply(e);
            setStatus(err == null ? "적용됨: " + e.name : err, err == null ? 0x55FF55 : 0xFF5555);
        } else if (b.id == BTN_TOGGLE) {
            SelfSkinMod.setEnabled(!cfg.enabled);
        } else if (b.id == BTN_MODEL) {
            SkinConfig.SkinEntry e = cfg.getActive();
            if (e != null) {
                e.model = SkinConfig.MODEL_SLIM.equals(e.model) ? SkinConfig.MODEL_CLASSIC : SkinConfig.MODEL_SLIM;
                cfg.save();
                SelfSkinMod.skins.setModel(e.model);
            }
        } else if (b.id == BTN_FOLDER) {
            openFolder(SkinConfig.getSkinsDir());
        } else if (b.id == BTN_REFRESH) {
            cfg.syncWithFolder();
            if (cfg.getActive() == null) SelfSkinMod.skins.unload();
            setStatus("스킨 " + cfg.skins.size() + "개", 0xAAAAAA);
        } else if (b.id == BTN_FETCH) {
            startFetch();
        } else if (b.id == BTN_PREV) {
            page--;
        } else if (b.id == BTN_NEXT) {
            page++;
        } else if (b.id == BTN_DONE) {
            mc.displayGuiScreen(null);
            return;
        }
        dirty = true;
    }

    private void startFetch() {
        final String name = nameField.getText().trim();
        if (!SkinFetcher.isValidName(name)) {
            setStatus("닉네임은 영문/숫자/_ 1~16자", 0xFF5555);
            return;
        }
        fetching = true;
        setStatus(name + " 스킨 불러오는 중...", 0xFFFF55);
        new Thread(() -> {
            try {
                final SkinFetcher.Result r = SkinFetcher.fetch(name, SkinConfig.getSkinsDir());
                mc.addScheduledTask(() -> {
                    fetching = false;
                    SkinConfig cfg = SelfSkinMod.config;
                    cfg.syncWithFolder();
                    SkinConfig.SkinEntry e = cfg.find(r.file.getName());
                    if (e == null) {
                        setStatus("저장 실패", 0xFF5555);
                    } else {
                        e.model = r.model;
                        e.source = "username";
                        cfg.save();
                        String err = SelfSkinMod.apply(e);
                        setStatus(err == null ? "적용됨: " + e.name : err, err == null ? 0x55FF55 : 0xFF5555);
                    }
                    dirty = true;
                });
            } catch (Exception ex) {
                final String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                mc.addScheduledTask(() -> {
                    fetching = false;
                    setStatus("실패: " + msg, 0xFF5555);
                    dirty = true;
                });
            }
        }, "SelfSkin-Fetch").start();
        dirty = true;
    }

    private void setStatus(String s, int color) {
        status = s;
        statusColor = color;
    }

    private static void openFolder(File dir) {
        String path = dir.getAbsolutePath();
        try {
            Util.EnumOS os = Util.getOSType();
            if (os == Util.EnumOS.OSX) {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/open", path});
                return;
            }
            if (os == Util.EnumOS.WINDOWS) {
                Runtime.getRuntime().exec(new String[]{"explorer.exe", path});
                return;
            }
        } catch (IOException ignored) {
        }
        Sys.openURL("file://" + path);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (dirty) {
            dirty = false;
            rebuildButtons();
        }
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Self Skin §7— 나에게만 보이는 스킨", width / 2, 12, 0xFFFFFF);

        // 미리보기
        if (mc.thePlayer != null) {
            int footY = 32 + 150;
            GuiInventory.drawEntityOnScreen(previewX, footY, 60,
                    previewX - mouseX, footY - 100 - mouseY, mc.thePlayer);
        } else {
            drawCenteredString(fontRendererObj, "§7월드에 들어가면", previewX, 90, 0xFFFFFF);
            drawCenteredString(fontRendererObj, "§7미리보기가 보입니다", previewX, 102, 0xFFFFFF);
        }

        if (SelfSkinMod.config.skins.isEmpty()) {
            drawString(fontRendererObj, "§7스킨이 없습니다.", left, 36, 0xFFFFFF);
            drawString(fontRendererObj, "§7폴더에 png를 넣거나", left, 48, 0xFFFFFF);
            drawString(fontRendererObj, "§7닉네임으로 가져오세요.", left, 60, 0xFFFFFF);
        }

        drawString(fontRendererObj, "닉네임으로 가져오기", left, height - 40, 0xAAAAAA);
        nameField.drawTextBox();
        if (!status.isEmpty()) {
            drawCenteredString(fontRendererObj, status, width / 2, height - 52, statusColor);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (nameField.isFocused()) {
            if (key == Keyboard.KEY_RETURN) {
                if (!fetching) startFetch();
                return;
            }
            if (key != Keyboard.KEY_ESCAPE) {
                nameField.textboxKeyTyped(c, key);
                return;
            }
        }
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        nameField.mouseClicked(x, y, button);
    }

    @Override
    public void updateScreen() {
        nameField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
