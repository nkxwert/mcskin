package dev.selfskin.gui;

import com.mojang.blaze3d.Blaze3D;
import dev.selfskin.L;
import dev.selfskin.SelfSkin;
import dev.selfskin.SkinConfig;
import dev.selfskin.SkinFetcher;
import dev.selfskin.SkinImporter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

/** Self Skin 메인 메뉴 */
public class SelfSkinScreen extends Screen {

    private static final int GREEN = 0xFF55FF55, RED = 0xFFFF5555, YELLOW = 0xFFFFFF55, GRAY = 0xFFAAAAAA;

    /** 이름 바꾸기/삭제/순서 대상. 목록에서 누른 스킨 (적용에 실패해도 선택은 된다) */
    private String selectedFile = SelfSkin.config.activeFile;
    private int page;
    private int rows;
    private EditBox nameField;
    private String nameText = "";
    private Component status;
    private int statusColor = GRAY;
    private boolean fetching;

    private int left, colX, previewX0, previewX1;
    private static final int LIST_W = 140, COL_W = 90;

    public SelfSkinScreen() {
        super(L.c("gui.title"));
    }

    @Override
    protected void init() {
        SkinConfig cfg = SelfSkin.config;
        left = width / 2 - 180;
        colX = width / 2 + 90;
        previewX0 = left + LIST_W + 5;
        previewX1 = colX - 5;
        rows = Math.max(3, Math.min(10, (height - 120) / 22));

        // ----- 왼쪽: 스킨 목록 -----
        List<SkinConfig.SkinEntry> list = cfg.skins;
        int pages = Math.max(1, (list.size() + rows - 1) / rows);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < rows; i++) {
            int idx = page * rows + i;
            if (idx >= list.size()) break;
            SkinConfig.SkinEntry e = list.get(idx);
            addRenderableWidget(Button.builder(entryLabel(e), b -> select(e))
                    .bounds(left, 32 + i * 22, LIST_W, 20).build());
        }

        // [<] [이름] [삭제] [>]
        int pageY = 32 + rows * 22;
        int midW = (LIST_W - 40 - 6) / 2;
        SkinConfig.SkinEntry sel = getSelected();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(left, pageY, 20, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(L.c("gui.rename"), b -> {
            SkinConfig.SkinEntry e = getSelected();
            if (e != null) minecraft.gui.setScreen(new RenameScreen(this, e));
        }).bounds(left + 22, pageY, midW, 20).build()).active = sel != null;
        addRenderableWidget(Button.builder(L.c("gui.delete"), b -> confirmDelete())
                .bounds(left + LIST_W - 22 - midW, pageY, midW, 20).build()).active = sel != null;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(left + LIST_W - 20, pageY, 20, 20).build()).active = page < pages - 1;

        // ----- 오른쪽 버튼 -----
        SkinConfig.SkinEntry active = cfg.getActive();
        addRenderableWidget(Button.builder(L.c("gui.apply", L.onOff(cfg.enabled)), b -> {
            Component err = SelfSkin.setEnabled(!cfg.enabled);
            if (err != null) setStatus(err, RED);
            rebuildWidgets();
        }).bounds(colX, 32, COL_W, 20).build()).active = active != null;
        addRenderableWidget(Button.builder(L.c("gui.model", active != null && active.isSlim() ? "Alex" : "Steve"), b -> {
            SkinConfig.SkinEntry e = cfg.getActive();
            if (e != null) SelfSkin.setModel(e, !e.isSlim());
            rebuildWidgets();
        }).bounds(colX, 54, COL_W, 20).build()).active = active != null;
        int folderW = 38;
        addRenderableWidget(Button.builder(L.c("gui.folder"), b -> Blaze3D.openPath(SkinConfig.getSkinsDir()))
                .bounds(colX, 76, folderW, 20).build());
        addRenderableWidget(Button.builder(L.c("gui.refresh"), b -> {
            Component err = SelfSkin.refresh();
            if (err != null) setStatus(err, RED);
            else setStatus(L.c("status.count", cfg.skins.size()), GRAY);
            rebuildWidgets();
        }).bounds(colX + folderW + 2, 76, COL_W - folderW - 2, 20).build());
        addRenderableWidget(Button.builder(L.c("gui.visibility"), b -> minecraft.gui.setScreen(new VisibilityScreen(this)))
                .bounds(colX, 98, COL_W, 20).build());
        int selIdx = sel != null ? list.indexOf(sel) : -1;
        int upW = (COL_W - 2) / 2;
        addRenderableWidget(Button.builder(L.c("gui.moveUp"), b -> move(-1))
                .bounds(colX, 120, upW, 20).build()).active = selIdx > 0;
        addRenderableWidget(Button.builder(L.c("gui.moveDown"), b -> move(1))
                .bounds(colX + COL_W - upW, 120, upW, 20).build()).active = selIdx >= 0 && selIdx < list.size() - 1;

        // ----- 아래: 닉네임으로 가져오기 -----
        nameField = new EditBox(font, left + 1, height - 28, LIST_W - 2, 18, L.c("gui.fetchLabel"));
        nameField.setMaxLength(16);
        nameField.setValue(nameText);
        nameField.setResponder(s -> nameText = s);
        addRenderableWidget(nameField);
        addRenderableWidget(Button.builder(L.c("gui.fetch"), b -> startFetch())
                .bounds(left + LIST_W + 4, height - 29, 60, 20).build()).active = !fetching;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(colX, height - 29, COL_W, 20).build());
    }

    private Component entryLabel(SkinConfig.SkinEntry e) {
        SkinConfig cfg = SelfSkin.config;
        boolean isActive = e.file.equals(cfg.activeFile);
        boolean isSelected = e.file.equals(selectedFile);
        String mark = isActive ? "§a● " : isSelected ? "§e▶ " : "";
        String tag = e.isSlim() ? " §7[A]" : " §7[S]";
        String name = font.plainSubstrByWidth(e.name, LIST_W - 16 - font.width(mark + tag));
        return Component.literal(mark + (isSelected ? "§n" : "") + name + "§r" + tag);
    }

    private void select(SkinConfig.SkinEntry e) {
        selectedFile = e.file;
        Component err = SelfSkin.apply(e);
        setStatus(err == null ? L.c("status.applied", e.name) : err, err == null ? GREEN : RED);
        rebuildWidgets();
    }

    private void move(int delta) {
        SkinConfig.SkinEntry e = getSelected();
        if (e != null && SelfSkin.config.move(e, delta)) {
            page = SelfSkin.config.skins.indexOf(e) / rows; // 다른 페이지로 넘어가면 따라간다
        }
        rebuildWidgets();
    }

    private void confirmDelete() {
        SkinConfig.SkinEntry e = getSelected();
        if (e == null) return;
        minecraft.gui.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                Component err = SelfSkin.delete(e);
                if (err == null) {
                    selectedFile = SelfSkin.config.activeFile;
                    setStatus(L.c("status.deleted", e.name), GRAY);
                } else {
                    setStatus(err, RED);
                }
            }
            minecraft.gui.setScreen(this);
        }, L.c("delete.title"), L.c("delete.question", e.name), L.c("gui.delete"), CommonComponents.GUI_CANCEL));
    }

    /** 이름 바꾸기 화면에서 돌아올 때 */
    void onRenamed(SkinConfig.SkinEntry e) {
        selectedFile = e.file;
        setStatus(L.c("status.renamed", e.name), GREEN);
    }

    private SkinConfig.SkinEntry getSelected() {
        return SelfSkin.config.find(selectedFile);
    }

    private void setStatus(Component s, int color) {
        status = s;
        statusColor = color;
    }

    private void startFetch() {
        final String name = nameText.trim();
        if (!SkinFetcher.isValidName(name)) {
            setStatus(L.c("status.badUsername"), RED);
            return;
        }
        fetching = true;
        setStatus(L.c("status.fetching", name), YELLOW);
        rebuildWidgets();
        Thread t = new Thread(() -> {
            try {
                SkinFetcher.Result r = SkinFetcher.fetch(name, SkinConfig.getSkinsDir());
                minecraft.execute(() -> {
                    fetching = false;
                    SkinConfig cfg = SelfSkin.config;
                    cfg.syncWithFolder();
                    // Windows는 파일 이름 대소문자를 구분하지 않아 기존 "notch.png"에 덮어쓸 수 있다
                    SkinConfig.SkinEntry e = cfg.findIgnoreCase(r.file().getFileName().toString());
                    if (e == null) {
                        setStatus(L.c("status.saveFailed"), RED);
                    } else {
                        e.model = r.slim() ? SkinConfig.MODEL_SLIM : SkinConfig.MODEL_CLASSIC;
                        e.source = "username";
                        cfg.save();
                        selectedFile = e.file;
                        page = cfg.skins.indexOf(e) / rows;
                        Component err = SelfSkin.apply(e);
                        setStatus(err == null ? L.c("status.applied", e.name) : err, err == null ? GREEN : RED);
                    }
                    rebuildWidgets();
                });
            } catch (Exception ex) {
                Component msg = ex instanceof SkinFetcher.FetchException fe ? fe.message
                        : Component.literal(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                minecraft.execute(() -> {
                    fetching = false;
                    setStatus(L.c("status.failed", msg), RED);
                    rebuildWidgets();
                });
            }
        }, "SelfSkin-Fetch");
        t.setDaemon(true);
        t.start();
    }

    /** 파일을 게임 창에 끌어다 놓으면 스킨 폴더에 추가하고, 마지막 것을 적용한다 */
    @Override
    public void onFilesDrop(List<Path> files) {
        SkinImporter.Result r = SkinImporter.importFiles(files, SkinConfig.getSkinsDir());
        SkinConfig cfg = SelfSkin.config;
        if (r.added > 0) {
            cfg.syncWithFolder();
            SkinConfig.SkinEntry e = cfg.find(r.lastFile);
            Component err = null;
            if (e != null) {
                selectedFile = e.file;
                page = cfg.skins.indexOf(e) / rows;
                err = SelfSkin.apply(e);
            }
            if (err != null) setStatus(err, RED);
            else if (r.skipped > 0) setStatus(L.c("status.importedSkipped", r.added, r.skipped, r.error), YELLOW);
            else setStatus(L.c("status.imported", r.added), GREEN);
        } else {
            setStatus(r.error != null ? r.error : L.c("error.notPng"), RED);
        }
        rebuildWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (getFocused() == nameField && event.isConfirmation()) {
            if (!fetching) startFetch();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);

        // 미리보기
        if (minecraft.player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, previewX0, 32, previewX1, 182, 60, 0.0625F,
                    mouseX, mouseY, minecraft.player);
        } else {
            int cx = (previewX0 + previewX1) / 2;
            graphics.centeredText(font, L.c("gui.preview1"), cx, 90, GRAY);
            graphics.centeredText(font, L.c("gui.preview2"), cx, 102, GRAY);
        }

        if (SelfSkin.config.skins.isEmpty()) {
            graphics.text(font, L.c("gui.empty1"), left, 36, GRAY);
            graphics.text(font, L.c("gui.empty2"), left, 48, GRAY);
            graphics.text(font, L.c("gui.empty3"), left, 60, GRAY);
        }

        graphics.text(font, L.c("gui.fetchLabel"), left, height - 40, GRAY);
        if (status != null) {
            graphics.centeredText(font, status, width / 2, height - 52, statusColor);
        } else {
            graphics.centeredText(font, L.c("gui.dropHint"), width / 2, height - 52, GRAY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
