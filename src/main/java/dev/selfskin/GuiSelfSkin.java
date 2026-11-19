package dev.selfskin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.Util;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GuiSelfSkin extends GuiScreen implements GuiYesNoCallback {

    private static final int BTN_DONE = 0, BTN_TOGGLE = 1, BTN_MODEL = 2, BTN_FOLDER = 3,
            BTN_REFRESH = 4, BTN_FETCH = 5, BTN_VISIBILITY = 6, BTN_RENAME = 7, BTN_DELETE = 8,
            BTN_PREV = 10, BTN_NEXT = 11, BTN_ENTRY = 100;
    private static final int CONFIRM_DELETE = 1;

    /** 이름 바꾸기/삭제 대상. 목록에서 누른 스킨 (적용에 실패해도 선택은 된다) */
    private String selectedFile = SelfSkinMod.config.activeFile;

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
            boolean isSelected = e.file.equals(selectedFile);
            String mark = isActive ? "§a● " : isSelected ? "§e▶ " : "";
            String label = mark + (isSelected ? "§n" : "") + e.name + "§r §7" + (SkinConfig.MODEL_SLIM.equals(e.model) ? "[A]" : "[S]");
            label = fontRendererObj.trimStringToWidth(label, listW - 10);
            buttonList.add(new GuiButton(BTN_ENTRY + idx, left, 32 + i * 22, listW, 20, label));
        }
        int pageY = 32 + rows * 22;
        // [<] [이름] [삭제] [>]
        GuiButton prev = new GuiButton(BTN_PREV, left, pageY, 20, 20, "<");
        GuiButton next = new GuiButton(BTN_NEXT, left + listW - 20, pageY, 20, 20, ">");
        prev.enabled = page > 0;
        next.enabled = page < pages - 1;
        int midW = (listW - 40 - 6) / 2; // 영어 "Rename"도 들어가도록 가운데 버튼을 넓게
        boolean hasSelection = getSelected() != null;
        GuiButton rename = new GuiButton(BTN_RENAME, left + 22, pageY, midW, 20, L.t("gui.rename"));
        GuiButton delete = new GuiButton(BTN_DELETE, left + listW - 22 - midW, pageY, midW, 20, L.t("gui.delete"));
        rename.enabled = hasSelection;
        delete.enabled = hasSelection;
        buttonList.add(prev);
        buttonList.add(next);
        buttonList.add(rename);
        buttonList.add(delete);

        SkinConfig.SkinEntry active = SelfSkinMod.config.getActive();
        GuiButton toggle = new GuiButton(BTN_TOGGLE, colX, 32, colW, 20,
                L.t("gui.apply", L.t(SelfSkinMod.config.enabled ? "state.on" : "state.off")));
        toggle.enabled = active != null;
        GuiButton model = new GuiButton(BTN_MODEL, colX, 54, colW, 20,
                L.t("gui.model", active != null && SkinConfig.MODEL_SLIM.equals(active.model) ? "Alex" : "Steve"));
        model.enabled = active != null;
        buttonList.add(toggle);
        buttonList.add(model);
        // 작은 화면에서도 '완료' 버튼과 겹치지 않도록 폴더/새로고침은 한 줄에 둔다
        int folderW = 38; // "새로고침"/"Refresh"가 더 길어서 폴더 버튼을 좁게
        buttonList.add(new GuiButton(BTN_FOLDER, colX, 76, folderW, 20, L.t("gui.folder")));
        buttonList.add(new GuiButton(BTN_REFRESH, colX + folderW + 2, 76, colW - folderW - 2, 20, L.t("gui.refresh")));
        // 갑옷 / 머리 블록 / 망토 보임 설정은 별도 화면
        buttonList.add(new GuiButton(BTN_VISIBILITY, colX, 98, colW, 20, L.t("gui.visibility")));

        GuiButton fetch = new GuiButton(BTN_FETCH, left + listW + 4, height - 29, 60, 20, L.t("gui.fetch"));
        fetch.enabled = !fetching;
        buttonList.add(fetch);
        buttonList.add(new GuiButton(BTN_DONE, colX, height - 29, colW, 20, L.t("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        SkinConfig cfg = SelfSkinMod.config;
        if (b.id >= BTN_ENTRY) {
            SkinConfig.SkinEntry e = cfg.skins.get(b.id - BTN_ENTRY);
            selectedFile = e.file;
            String err = SelfSkinMod.apply(e);
            setStatus(err == null ? L.t("status.applied", e.name) : err, err == null ? 0x55FF55 : 0xFF5555);
        } else if (b.id == BTN_TOGGLE) {
            String err = SelfSkinMod.setEnabled(!cfg.enabled);
            if (err != null) setStatus(err, 0xFF5555);
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
            SkinConfig.SkinEntry active = cfg.getActive();
            if (active == null) {
                SelfSkinMod.skins.unload();
                SelfSkinMod.skins.setActive(false);
                setStatus(L.t("status.count", cfg.skins.size()), 0xAAAAAA);
            } else if (cfg.enabled) {
                // 사용 중인 png를 수정했을 수 있으므로 다시 읽는다
                String err = SelfSkinMod.apply(active);
                setStatus(err == null ? L.t("status.count", cfg.skins.size()) : err, err == null ? 0xAAAAAA : 0xFF5555);
            } else {
                setStatus(L.t("status.count", cfg.skins.size()), 0xAAAAAA);
            }
        } else if (b.id == BTN_VISIBILITY) {
            mc.displayGuiScreen(new GuiVisibility(this));
            return;
        } else if (b.id == BTN_RENAME) {
            SkinConfig.SkinEntry e = getSelected();
            if (e != null) mc.displayGuiScreen(new GuiRenameSkin(this, e));
            return;
        } else if (b.id == BTN_DELETE) {
            SkinConfig.SkinEntry e = getSelected();
            if (e != null) {
                mc.displayGuiScreen(new GuiYesNo(this, L.t("delete.title"),
                        L.t("delete.question", e.name), L.t("gui.delete"), L.t("gui.cancel"), CONFIRM_DELETE));
            }
            return;
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
            setStatus(L.t("status.badUsername"), 0xFF5555);
            return;
        }
        fetching = true;
        setStatus(L.t("status.fetching", name), 0xFFFF55);
        new Thread(() -> {
            try {
                final SkinFetcher.Result r = SkinFetcher.fetch(name, SkinConfig.getSkinsDir());
                mc.addScheduledTask(() -> {
                    fetching = false;
                    SkinConfig cfg = SelfSkinMod.config;
                    cfg.syncWithFolder();
                    // Windows는 파일명 대소문자를 구분하지 않아 기존 "notch.png"에 덮어쓸 수 있다
                    SkinConfig.SkinEntry e = cfg.findIgnoreCase(r.file.getName());
                    if (e == null) {
                        setStatus(L.t("status.saveFailed"), 0xFF5555);
                    } else {
                        e.model = r.model;
                        e.source = "username";
                        cfg.save();
                        selectedFile = e.file;
                        String err = SelfSkinMod.apply(e);
                        setStatus(err == null ? L.t("status.applied", e.name) : err, err == null ? 0x55FF55 : 0xFF5555);
                    }
                    dirty = true;
                });
            } catch (Exception ex) {
                final String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                mc.addScheduledTask(() -> {
                    fetching = false;
                    setStatus(L.t("status.failed", msg), 0xFF5555);
                    dirty = true;
                });
            }
        }, "SelfSkin-Fetch").start();
        dirty = true;
    }

    /** 삭제 확인 창의 결과 */
    @Override
    public void confirmClicked(boolean yes, int id) {
        if (id == CONFIRM_DELETE && yes) {
            SkinConfig.SkinEntry e = getSelected();
            if (e != null) {
                String err = SelfSkinMod.delete(e);
                if (err == null) {
                    selectedFile = SelfSkinMod.config.activeFile;
                    setStatus(L.t("status.deleted", e.name), 0xAAAAAA);
                } else {
                    setStatus(err, 0xFF5555);
                }
            }
        }
        mc.displayGuiScreen(this);
    }

    /** 이름 바꾸기 화면에서 돌아올 때 */
    void onRenamed(SkinConfig.SkinEntry e) {
        selectedFile = e.file;
        setStatus(L.t("status.renamed", e.name), 0x55FF55);
        dirty = true;
    }

    private SkinConfig.SkinEntry getSelected() {
        return SelfSkinMod.config.find(selectedFile);
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
        drawCenteredString(fontRendererObj, L.t("gui.title"), width / 2, 12, 0xFFFFFF);

        // 미리보기
        if (mc.thePlayer != null) {
            int footY = 32 + 150;
            GuiInventory.drawEntityOnScreen(previewX, footY, 60,
                    previewX - mouseX, footY - 100 - mouseY, mc.thePlayer);
        } else {
            drawCenteredString(fontRendererObj, L.t("gui.preview1"), previewX, 90, 0xFFFFFF);
            drawCenteredString(fontRendererObj, L.t("gui.preview2"), previewX, 102, 0xFFFFFF);
        }

        if (SelfSkinMod.config.skins.isEmpty()) {
            drawString(fontRendererObj, L.t("gui.empty1"), left, 36, 0xFFFFFF);
            drawString(fontRendererObj, L.t("gui.empty2"), left, 48, 0xFFFFFF);
            drawString(fontRendererObj, L.t("gui.empty3"), left, 60, 0xFFFFFF);
        }

        drawString(fontRendererObj, L.t("gui.fetchLabel"), left, height - 40, 0xAAAAAA);
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
