package dev.selfskin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** 스킨 이름 바꾸기 화면. 이름을 바꾸면 png 파일 이름도 같이 바뀐다. */
public class GuiRenameSkin extends GuiScreen {

    private static final int BTN_OK = 0, BTN_CANCEL = 1;

    private final GuiSelfSkin parent;
    private final SkinConfig.SkinEntry entry;
    private GuiTextField field;
    private String error = "";

    public GuiRenameSkin(GuiSelfSkin parent, SkinConfig.SkinEntry entry) {
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        String old = field != null ? field.getText() : entry.name;
        field = new GuiTextField(2, fontRendererObj, width / 2 - 100, height / 2 - 20, 200, 20);
        field.setMaxStringLength(SkinConfig.MAX_NAME_LENGTH);
        field.setText(old);
        field.setFocused(true);
        buttonList.clear();
        buttonList.add(new GuiButton(BTN_OK, width / 2 - 100, height / 2 + 12, 98, 20, L.t("gui.ok")));
        buttonList.add(new GuiButton(BTN_CANCEL, width / 2 + 2, height / 2 + 12, 98, 20, L.t("gui.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == BTN_OK) confirm();
        else if (b.id == BTN_CANCEL) mc.displayGuiScreen(parent);
    }

    private void confirm() {
        String err = SelfSkinMod.rename(entry, field.getText().trim());
        if (err != null) {
            error = err;
            return;
        }
        parent.onRenamed(entry);
        mc.displayGuiScreen(parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, L.t("rename.title"), width / 2, height / 2 - 52, 0xFFFFFF);
        drawCenteredString(fontRendererObj, L.t("rename.hint"), width / 2, height / 2 - 38, 0xFFFFFF);
        field.drawTextBox();
        if (!error.isEmpty()) drawCenteredString(fontRendererObj, error, width / 2, height / 2 + 40, 0xFF5555);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            confirm();
            return;
        }
        field.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        field.mouseClicked(x, y, button);
    }

    @Override
    public void updateScreen() {
        field.updateCursorCounter();
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
