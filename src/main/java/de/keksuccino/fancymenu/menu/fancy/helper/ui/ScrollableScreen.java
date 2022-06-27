package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ScrollableScreen extends GuiScreen {

    protected static final Color ENTRY_BACKGROUND_COLOR = new Color(92, 92, 92);
    protected static final Color SCREEN_BACKGROUND_COLOR = new Color(54, 54, 54);
    protected static final Color HEADER_FOOTER_COLOR = new Color(33, 33, 33);

    protected ScrollArea scrollArea;
    protected GuiScreen parent;
    protected String title;

    public ScrollableScreen(GuiScreen parent, String title) {

        super();
        this.parent = parent;
        this.title = title;

        this.scrollArea = new ScrollArea(0, 50, 300, 0);
        this.scrollArea.backgroundColor = ENTRY_BACKGROUND_COLOR;

    }

    @Override
    public void initGui() {

        this.scrollArea.x = (this.width / 2) - 150;
        this.scrollArea.height = this.height - 100;

    }

    //On Esc
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (!PopupHandler.isPopupActive()) {
            if (keyCode == 1) {
                this.closeScreen();
                if (this.mc.currentScreen == null) {
                    this.mc.setIngameFocus();
                }
            }
        }
    }

    public void closeScreen() {
        this.mc.displayGuiScreen(this.parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        GlStateManager.enableBlend();

        //Draw screen background
        drawRect(0, 0, this.width, this.height, SCREEN_BACKGROUND_COLOR.getRGB());

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof ScrollAreaEntryBase) {
                ((ScrollAreaEntryBase) e).isOverlayButtonHovered = this.isOverlayButtonHovered();
            }
        }

        this.scrollArea.render();

        //Draw header
        drawRect(0, 0, this.width, 50, HEADER_FOOTER_COLOR.getRGB());

        //Draw title
        drawCenteredString(Minecraft.getMinecraft().fontRenderer, this.title, this.width / 2, 20, -1);

        //Draw footer
        drawRect(0, this.height - 50, this.width, this.height, HEADER_FOOTER_COLOR.getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof ScrollAreaEntryBase) {
                if (((ScrollAreaEntryBase) e).isOverlayButtonHoveredAndOverlapsArea() && (((ScrollAreaEntryBase) e).description != null)) {
                    renderDescription(((ScrollAreaEntryBase) e).description, MouseInput.getMouseX(), MouseInput.getMouseY());
                    break;
                }
            }
        }

    }

    protected static void renderDescription(List<String> desc, int mouseX, int mouseY) {
        if (desc != null) {
            int width = 10;
            int height = 10;
            //Getting the longest string from the list to render the background with the correct width
            for (String s : desc) {
                int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(s) + 10;
                if (i > width) {
                    width = i;
                }
                height += 10;
            }
            mouseX += 5;
            mouseY += 5;
            if (Minecraft.getMinecraft().currentScreen.width < mouseX + width) {
                mouseX -= width + 10;
            }
            if (Minecraft.getMinecraft().currentScreen.height < mouseY + height) {
                mouseY -= height + 10;
            }
            RenderUtils.setZLevelPre(600);
            renderDescriptionBackground(mouseX, mouseY, width, height);
            GlStateManager.enableBlend();
            int i2 = 5;
            for (String s : desc) {
                Minecraft.getMinecraft().currentScreen.drawString(Minecraft.getMinecraft().fontRenderer, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
                i2 += 10;
            }
            RenderUtils.setZLevelPost();
            GlStateManager.disableBlend();
        }
    }

    protected static void renderDescriptionBackground(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
    }

    public boolean isOverlayButtonHovered() {
        return false;
    }

    public static class ScrollAreaEntryBase extends ScrollAreaEntry {

        protected int entryHeight = 25;
        protected List<String> description = null;
        protected Consumer<EntryRenderCallback> renderBody;

        protected boolean isOverlayButtonHovered = false;

        public ScrollAreaEntryBase(ScrollArea parent, Consumer<EntryRenderCallback> renderBody) {
            super(parent);
            this.renderBody = renderBody;
        }

        @Override
        public void renderEntry() {

            EntryRenderCallback c = new EntryRenderCallback();
            c.entry = this;

            this.renderBody.accept(c);

        }

        @Override
        public int getHeight() {
            return this.entryHeight;
        }

        public void setHeight(int height) {
            this.entryHeight = height;
        }

        public List<String> getDescription() {
            return this.description;
        }

        public boolean isOverlayButtonHoveredAndOverlapsArea() {
            return (this.isOverlayButtonHovered && this.isHovered());
        }

        public void setDescription(List<String> desc) {
            this.description = desc;
        }

        public void setDescription(String[] desc) {
            this.description = Arrays.asList(desc);
        }

        public static class EntryRenderCallback {

            public ScrollAreaEntryBase entry;

        }

    }

    public static class ButtonEntry extends ScrollAreaEntryBase {

        public AdvancedButton button;

        public ButtonEntry(ScrollArea parent, AdvancedButton button) {
            super(parent, null);
            this.button = button;
            this.renderBody = (render) -> {
                int xCenter = render.entry.x + (render.entry.getWidth() / 2);
                UIBase.colorizeButton(this.button);
                if (!this.isOverlayButtonHoveredAndOverlapsArea()) {
                    this.button.enabled = true;
                } else {
                    this.button.enabled = false;
                }
                this.button.width = 200;
                this.button.height = 20;
                this.button.x = xCenter - (this.button.width / 2);
                this.button.y = render.entry.y + 2;
                this.button.drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getTickLength());
            };
            this.setHeight(24);
        }

    }

    public static class TextFieldEntry extends ScrollAreaEntryBase {

        public AdvancedTextField textField;

        public TextFieldEntry(ScrollArea parent, AdvancedTextField textField) {
            super(parent, null);
            this.textField = textField;
            this.textField.setMaxStringLength(10000);
            this.renderBody = (render) -> {
                int xCenter = render.entry.x + (render.entry.getWidth() / 2);
                if (!this.isOverlayButtonHoveredAndOverlapsArea()) {
                    this.textField.setEnabled(true);
                } else {
                    this.textField.setEnabled(false);
                }
                this.textField.width = 200;
                this.textField.height = 20;
                this.textField.x = xCenter - (this.textField.getWidth() / 2);
                this.textField.y = render.entry.y + 2;
                this.textField.drawTextBox();
            };
            this.setHeight(24);
        }

    }

    public static class TextEntry extends ScrollAreaEntryBase {

        public String text;
        public boolean bold;

        public TextEntry(ScrollArea parent, String text, boolean bold) {
            super(parent, null);
            this.text = text;
            this.bold = bold;
            this.renderBody = (render) -> {
                if (this.text != null) {
                    FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                    int xCenter = render.entry.x + (render.entry.getWidth() / 2);
                    int yCenter = render.entry.y + (render.entry.getHeight() / 2);
                    String s = this.text;
                    if (this.bold) {
                        s = "§l" + this.text;
                    }
                    drawCenteredString(font, s, xCenter, yCenter - (font.FONT_HEIGHT / 2), -1);
                }
            };
            this.setHeight(18);
        }

    }

    public static class EmptySpaceEntry extends ScrollAreaEntryBase {

        public EmptySpaceEntry(ScrollArea parent, int height) {
            super(parent, null);
            this.renderBody = (render) -> {
            };
            this.setHeight(height);
        }

    }

    public static class SeparatorEntry extends ScrollAreaEntryBase {

        public SeparatorEntry(ScrollArea parent, int height, Color color) {
            super(parent, null);
            this.renderBody = (render) -> {
                drawRect(render.entry.x, render.entry.y, render.entry.x + render.entry.getWidth(), render.entry.y + render.entry.getHeight(), color.getRGB());
            };
            this.setHeight(height);
        }

    }

}
