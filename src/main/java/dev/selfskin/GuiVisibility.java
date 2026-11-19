package dev.selfskin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** 갑옷(부위별) / 머리 블록 / 망토 보임 설정 화면. 오른쪽 미리보기에 바로 반영된다. */
public class GuiVisibility extends GuiScreen {

    private static final int BTN_BACK = 0, BTN_SHOW_ALL = 1, BTN_HIDE_ALL = 2,
            BTN_HEAD_BLOCK = 3, BTN_CAPE = 4, BTN_ARMOR = 10 /* 10~13: 갑옷 칸 */;

    private final GuiScreen parent;
    private int colX, previewX;

    public GuiVisibility(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        colX = width / 2 - 150;
        previewX = width / 2 + 75;
        rebuild();
    }

    private void rebuild() {
        SkinConfig cfg = SelfSkinMod.config;
        buttonList.clear();
        int y = 36;
        // 위에서부터 투구, 흉갑, 레깅스, 부츠
        for (int i = 0; i < 4; i++) {
            int slot = SkinConfig.SLOT_HELMET - i;
            buttonList.add(new GuiButton(BTN_ARMOR + slot, colX, y, 150, 20,
                    SkinConfig.SLOT_NAMES[slot] + ": " + label(cfg.isArmorHidden(slot))));
            y += 22;
        }
        buttonList.add(new GuiButton(BTN_HEAD_BLOCK, colX, y, 150, 20, "머리 블록: " + label(cfg.hideHeadBlock)));
        y += 22;
        buttonList.add(new GuiButton(BTN_CAPE, colX, y, 150, 20, "망토: " + label(cfg.hideCape)));
        y += 28;
        buttonList.add(new GuiButton(BTN_SHOW_ALL, colX, y, 74, 20, "모두 보이기"));
        buttonList.add(new GuiButton(BTN_HIDE_ALL, colX + 76, y, 74, 20, "모두 숨기기"));
        buttonList.add(new GuiButton(BTN_BACK, width / 2 - 100, height - 29, 200, 20, "돌아가기"));
    }

    private static String label(boolean hidden) {
        return hidden ? "§c숨김" : "§a보임";
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        SkinConfig cfg = SelfSkinMod.config;
        if (b.id >= BTN_ARMOR && b.id < BTN_ARMOR + 4) {
            int slot = b.id - BTN_ARMOR;
            SelfSkinMod.setArmorHidden(slot, !cfg.isArmorHidden(slot));
        } else if (b.id == BTN_HEAD_BLOCK) {
            SelfSkinMod.setHeadBlockHidden(!cfg.hideHeadBlock);
        } else if (b.id == BTN_CAPE) {
            SelfSkinMod.setCapeHidden(!cfg.hideCape);
        } else if (b.id == BTN_SHOW_ALL || b.id == BTN_HIDE_ALL) {
            boolean hide = b.id == BTN_HIDE_ALL;
            for (int slot = 0; slot < 4; slot++) cfg.setArmorHidden(slot, hide);
            cfg.hideHeadBlock = hide;
            cfg.hideCape = hide;
            cfg.save();
        } else if (b.id == BTN_BACK) {
            mc.displayGuiScreen(parent);
            return;
        }
        rebuild();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "갑옷·망토 보임 설정 §7— 나에게만 적용", width / 2, 12, 0xFFFFFF);
        if (mc.thePlayer != null) {
            int footY = 36 + 150;
            GuiInventory.drawEntityOnScreen(previewX, footY, 60,
                    previewX - mouseX, footY - 100 - mouseY, mc.thePlayer);
        } else {
            drawCenteredString(fontRendererObj, "§7월드에 들어가면", previewX, 90, 0xFFFFFF);
            drawCenteredString(fontRendererObj, "§7미리보기가 보입니다", previewX, 102, 0xFFFFFF);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
