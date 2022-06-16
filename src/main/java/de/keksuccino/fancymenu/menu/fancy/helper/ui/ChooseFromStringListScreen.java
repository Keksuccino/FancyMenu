package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChooseFromStringListScreen extends ScrollableScreen {

    protected Consumer<String> callback;
    protected List<String> parentList = new ArrayList<>();

    protected AdvancedButton backButton;

    public ChooseFromStringListScreen(String screenTitle, GuiScreen parentScreen, List<String> parentList, Consumer<String> callback) {

        super(parentScreen, screenTitle);

        this.callback = callback;
        if (parentList != null) {
            this.parentList = parentList;
        }

        this.backButton = new AdvancedButton(0, 0, 200, 20, Locals.localize("fancymenu.guicomponents.back"), true, (press) -> {
            this.onCancel();
            Minecraft.getMinecraft().displayGuiScreen(this.parent);
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
    public void closeScreen() {
        this.onCancel();
        Minecraft.getMinecraft().displayGuiScreen(this.parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        super.drawScreen(mouseX, mouseY, partialTicks);

        int xCenter = this.width / 2;

        this.backButton.x = xCenter - (this.backButton.width / 2);
        this.backButton.y = this.height - 35;
        this.backButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);

    }

    public static class StringScrollAreaEntry extends ScrollAreaEntry {

        protected String entryValue;
        protected FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        protected ChooseFromStringListScreen parentScreen;

        protected boolean isMouseDown = false;

        public StringScrollAreaEntry(ScrollArea parent, String entryValue, ChooseFromStringListScreen parentScreen) {
            super(parent);
            this.entryValue = entryValue;
            this.parentScreen = parentScreen;
        }

        @Override
        public void renderEntry() {

            int center = this.x + (this.getWidth() / 2);

            if (!this.isHovered()) {
                drawRect(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), ENTRY_BACKGROUND_COLOR.getRGB());
            } else {
                drawRect(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), ENTRY_BACKGROUND_COLOR.brighter().brighter().getRGB());
            }

            String value = this.entryValue;
            if (font.getStringWidth(value) > this.getWidth() - 30) {
                value = new StringBuilder(value).reverse().toString();
                value = font.trimStringToWidth(value, this.getWidth() - 30);
                value = new StringBuilder(value).reverse().toString();
                value = ".." + value;
            }
            drawCenteredString(font, value, center, this.y + 10, -1);

            this.handleSelection();

        }

        protected void handleSelection() {

            if (!PopupHandler.isPopupActive() && !this.parentScreen.backButton.isMouseOver()) {
                if (MouseInput.isLeftMouseDown() && !this.isMouseDown) {
                    if (this.isHovered()) {
                        if (this.parentScreen.callback != null) {
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
