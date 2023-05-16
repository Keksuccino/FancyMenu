package de.keksuccino.fancymenu.rendering.ui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChooseFromStringListScreen extends ScrollableScreen {

    protected Consumer<String> callback;
    protected List<String> parentList = new ArrayList<>();

    protected AdvancedButton backButton;

    public ChooseFromStringListScreen(String screenTitle, Screen parentScreen, List<String> parentList, Consumer<String> callback) {

        super(parentScreen, screenTitle);

        this.callback = callback;
        if (parentList != null) {
            this.parentList = parentList;
        }

        this.backButton = new AdvancedButton(0, 0, 200, 20, I18n.get("fancymenu.guicomponents.back"), true, (press) -> {
            Minecraft.getInstance().setScreen(this.parent);
            this.onCancel();
        });
        UIBase.colorizeButton(this.backButton);

        for (String s : this.parentList) {
            this.scrollArea.addEntry(new StringScrollAreaEntry(this.scrollArea, s, this));
        }

    }

    protected void onCancel() {
        if (this.callback != null) {
            this.callback.accept(null);
        }
    }

    //On Esc
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
        this.onCancel();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {

        super.render(matrix, mouseX, mouseY, partialTicks);

        int xCenter = this.width / 2;

        this.backButton.setX(xCenter - (this.backButton.getWidth() / 2));
        this.backButton.setY(this.height - 35);
        this.backButton.render(matrix, mouseX, mouseY, partialTicks);

    }

    public static class StringScrollAreaEntry extends ScrollAreaEntry {

        protected String entryValue;
        protected Font font = Minecraft.getInstance().font;
        protected ChooseFromStringListScreen parentScreen;

        protected boolean isMouseDown = false;

        public StringScrollAreaEntry(ScrollArea parent, String entryValue, ChooseFromStringListScreen parentScreen) {
            super(parent);
            this.entryValue = entryValue;
            this.parentScreen = parentScreen;
        }

        @Override
        public void renderEntry(PoseStack matrix) {

            int center = this.x + (this.getWidth() / 2);

            if (!this.isHovered()) {
                fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), ENTRY_BACKGROUND_COLOR.getRGB());
            } else {
                fill(matrix, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), ENTRY_BACKGROUND_COLOR.brighter().brighter().getRGB());
            }

            String value = this.entryValue;
            if (font.width(value) > this.getWidth() - 30) {
                value = new StringBuilder(value).reverse().toString();
                value = font.plainSubstrByWidth(value, this.getWidth() - 30);
                value = new StringBuilder(value).reverse().toString();
                value = ".." + value;
            }
            drawCenteredString(matrix, font, value, center, this.y + 10, -1);

            this.handleSelection();

        }

        protected void handleSelection() {

            if (!PopupHandler.isPopupActive() && !this.parentScreen.backButton.isHoveredOrFocused()) {
                if (MouseInput.isLeftMouseDown() && !this.isMouseDown) {
                    if (this.isHovered()) {
                        if (this.parentScreen.callback != null) {
                            Minecraft.getInstance().setScreen(this.parentScreen.parent);
                            this.parentScreen.callback.accept(this.entryValue);
                        }
                    }
                    this.isMouseDown = true;
                }
                if (!MouseInput.isLeftMouseDown()) {
                    this.isMouseDown = false;
                }
            } else if (MouseInput.isLeftMouseDown()) {
                this.isMouseDown = true;
            }

        }

        @Override
        public int getHeight() {
            return 26;
        }

    }

}
