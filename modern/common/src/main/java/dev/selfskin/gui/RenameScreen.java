package dev.selfskin.gui;

import dev.selfskin.L;
import dev.selfskin.SelfSkin;
import dev.selfskin.SkinConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** 스킨 이름 바꾸기. png 파일 이름도 같이 바뀐다. */
public class RenameScreen extends Screen {

    private final SelfSkinScreen parent;
    private final SkinConfig.SkinEntry entry;
    private EditBox field;
    private String text;
    private Component error;

    public RenameScreen(SelfSkinScreen parent, SkinConfig.SkinEntry entry) {
        super(L.c("rename.title"));
        this.parent = parent;
        this.entry = entry;
        this.text = entry.name;
    }

    @Override
    protected void init() {
        field = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, title);
        field.setMaxLength(SkinConfig.MAX_NAME_LENGTH);
        field.setValue(text);
        field.setResponder(s -> text = s);
        addRenderableWidget(field);
        addRenderableWidget(Button.builder(CommonComponents.GUI_OK, b -> confirm())
                .bounds(width / 2 - 100, height / 2 + 12, 98, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(width / 2 + 2, height / 2 + 12, 98, 20).build());
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(field);
    }

    private void confirm() {
        Component err = SelfSkin.rename(entry, text.trim());
        if (err != null) {
            error = err;
            return;
        }
        parent.onRenamed(entry);
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation()) {
            confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, height / 2 - 52, 0xFFFFFFFF);
        graphics.centeredText(font, L.c("rename.hint"), width / 2, height / 2 - 38, 0xFFAAAAAA);
        if (error != null) graphics.centeredText(font, error, width / 2, height / 2 + 40, 0xFFFF5555);
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
