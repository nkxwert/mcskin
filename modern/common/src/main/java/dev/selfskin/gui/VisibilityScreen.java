package dev.selfskin.gui;

import dev.selfskin.L;
import dev.selfskin.SelfSkin;
import dev.selfskin.SkinConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.CommonComponents;

/** 갑옷(부위별) / 머리 블록 / 망토 보임 설정. 오른쪽 미리보기에 바로 반영된다. */
public class VisibilityScreen extends Screen {

    private final Screen parent;
    private int colX;

    public VisibilityScreen(Screen parent) {
        super(L.c("vis.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SkinConfig cfg = SelfSkin.config;
        colX = width / 2 - 150;
        int y = 36;
        // 위에서부터 투구, 흉갑, 레깅스, 부츠
        for (int i = 0; i < 4; i++) {
            int slot = SkinConfig.SLOT_HELMET - i;
            addRenderableWidget(Button.builder(L.c("vis.line", L.slot(slot), L.hidden(cfg.isArmorHidden(slot))), b -> {
                SelfSkin.setArmorHidden(slot, !cfg.isArmorHidden(slot));
                rebuildWidgets();
            }).bounds(colX, y, 150, 20).build());
            y += 22;
        }
        addRenderableWidget(Button.builder(L.c("vis.line", L.c("headBlock"), L.hidden(cfg.hideHeadBlock)), b -> {
            SelfSkin.setHeadBlockHidden(!cfg.hideHeadBlock);
            rebuildWidgets();
        }).bounds(colX, y, 150, 20).build());
        y += 22;
        addRenderableWidget(Button.builder(L.c("vis.line", L.c("cape"), L.hidden(cfg.hideCape)), b -> {
            SelfSkin.setCapeHidden(!cfg.hideCape);
            rebuildWidgets();
        }).bounds(colX, y, 150, 20).build());
        y += 28;
        addRenderableWidget(Button.builder(L.c("vis.showAll"), b -> setAll(false)).bounds(colX, y, 74, 20).build());
        addRenderableWidget(Button.builder(L.c("vis.hideAll"), b -> setAll(true)).bounds(colX + 76, y, 74, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(width / 2 - 100, height - 29, 200, 20).build());
    }

    private void setAll(boolean hide) {
        SelfSkin.config.setAllHidden(hide);
        SelfSkin.config.save();
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        int x0 = width / 2 + 20, x1 = width / 2 + 130;
        if (minecraft.player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x0, 36, x1, 186, 60, 0.0625F,
                    mouseX, mouseY, minecraft.player);
        } else {
            graphics.centeredText(font, L.c("gui.preview1"), (x0 + x1) / 2, 90, 0xFFAAAAAA);
            graphics.centeredText(font, L.c("gui.preview2"), (x0 + x1) / 2, 102, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
