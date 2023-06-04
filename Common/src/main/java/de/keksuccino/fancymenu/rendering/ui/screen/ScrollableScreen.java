package de.keksuccino.fancymenu.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ScrollableScreen extends Screen {

    protected static final Color ENTRY_BACKGROUND_COLOR = new Color(92, 92, 92);
    protected static final Color SCREEN_BACKGROUND_COLOR = new Color(54, 54, 54);
    protected static final Color HEADER_FOOTER_COLOR = new Color(33, 33, 33);

    protected ScrollArea scrollArea;
    protected Screen parent;
    protected String title;

    public ScrollableScreen(Screen parent, String title) {

        super(Component.literal(""));
        this.parent = parent;
        this.title = title;

        this.scrollArea = new ScrollArea(0, 50, 300, 0);
        this.scrollArea.backgroundColor = ENTRY_BACKGROUND_COLOR;

    }

    @Override
    protected void init() {

        this.scrollArea.x = (this.width / 2) - 150;
        this.scrollArea.height = this.height - 100;

    }

    //On Esc
    @Override
    public void onClose() {
        if (!PopupHandler.isPopupActive()) {
            Minecraft.getInstance().setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {

        RenderSystem.enableBlend();

        //Draw screen background
        fill(matrix, 0, 0, this.width, this.height, SCREEN_BACKGROUND_COLOR.getRGB());

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof ScrollAreaEntryBase) {
                ((ScrollAreaEntryBase) e).isOverlayButtonHovered = this.isOverlayButtonHovered();
            }
        }

        this.scrollArea.render(matrix);

        //Draw header
        fill(matrix, 0, 0, this.width, 50, HEADER_FOOTER_COLOR.getRGB());

        //Draw title
        drawCenteredString(matrix, font, this.title, this.width / 2, 20, -1);

        //Draw footer
        fill(matrix, 0, this.height - 50, this.width, this.height, HEADER_FOOTER_COLOR.getRGB());

        super.render(matrix, mouseX, mouseY, partialTicks);

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof ScrollAreaEntryBase) {
                if (((ScrollAreaEntryBase) e).isOverlayButtonHoveredAndOverlapsArea() && (((ScrollAreaEntryBase) e).description != null)) {
                    renderDescription(matrix, ((ScrollAreaEntryBase) e).description, MouseInput.getMouseX(), MouseInput.getMouseY());
                    break;
                }
            }
        }

    }

    protected static void renderDescription(PoseStack matrix, List<String> desc, int mouseX, int mouseY) {
        if (desc != null) {
            int width = 10;
            int height = 10;
            //Getting the longest string from the list to render the background with the correct width
            for (String s : desc) {
                int i = Minecraft.getInstance().font.width(s) + 10;
                if (i > width) {
                    width = i;
                }
                height += 10;
            }
            mouseX += 5;
            mouseY += 5;
            if (Minecraft.getInstance().screen.width < mouseX + width) {
                mouseX -= width + 10;
            }
            if (Minecraft.getInstance().screen.height < mouseY + height) {
                mouseY -= height + 10;
            }
            RenderUtils.setZLevelPre(matrix, 600);
            renderDescriptionBackground(matrix, mouseX, mouseY, width, height);
            RenderSystem.enableBlend();
            int i2 = 5;
            for (String s : desc) {
                drawString(matrix, Minecraft.getInstance().font, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
                i2 += 10;
            }
            RenderUtils.setZLevelPost(matrix);
            RenderSystem.disableBlend();
        }
    }

    protected static void renderDescriptionBackground(PoseStack matrix, int x, int y, int width, int height) {
        Gui.fill(matrix, x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
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
        public void renderEntry(PoseStack matrix) {

            EntryRenderCallback c = new EntryRenderCallback();
            c.entry = this;
            c.matrix = matrix;

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
            public PoseStack matrix;

        }

    }

    public static class ButtonEntry extends ScrollAreaEntryBase {

        public AdvancedButton button;

        public ButtonEntry(ScrollArea parent, AdvancedButton button) {
            super(parent, null);
            this.button = button;
            this.renderBody = (render) -> {
                int xCenter = render.entry.x + (render.entry.getWidth() / 2);
                UIBase.applyDefaultButtonSkinTo(this.button);
                if (!this.isOverlayButtonHoveredAndOverlapsArea()) {
                    this.button.active = true;
                } else {
                    this.button.active = false;
                }
                this.button.setWidth(200);
                ((IMixinAbstractWidget)this.button).setHeightFancyMenu(20);
                this.button.setX(xCenter - (this.button.getWidth() / 2));
                this.button.setY(render.entry.y + 2);
                this.button.render(render.matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime());
            };
            this.setHeight(24);
        }

    }

    public static class TextFieldEntry extends ScrollAreaEntryBase {

        public AdvancedTextField textField;

        public TextFieldEntry(ScrollArea parent, AdvancedTextField textField) {
            super(parent, null);
            this.textField = textField;
            this.textField.setMaxLength(10000);
            this.renderBody = (render) -> {
                int xCenter = render.entry.x + (render.entry.getWidth() / 2);
                if (!this.isOverlayButtonHoveredAndOverlapsArea()) {
                    this.textField.active = true;
                } else {
                    this.textField.active = false;
                }
                this.textField.setWidth(200);
                ((IMixinAbstractWidget)this.textField).setHeightFancyMenu(20);
                this.textField.setX(xCenter - (this.textField.getWidth() / 2));
                this.textField.setY(render.entry.y + 2);
                this.textField.render(render.matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime());
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
                    Font font = Minecraft.getInstance().font;
                    int xCenter = render.entry.x + (render.entry.getWidth() / 2);
                    int yCenter = render.entry.y + (render.entry.getHeight() / 2);
                    String s = this.text;
                    if (this.bold) {
                        s = "§l" + this.text;
                    }
                    drawCenteredString(render.matrix, font, s, xCenter, yCenter - (font.lineHeight / 2), -1);
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
                fill(render.matrix, render.entry.x, render.entry.y, render.entry.x + render.entry.getWidth(), render.entry.y + render.entry.getHeight(), color.getRGB());
            };
            this.setHeight(height);
        }

    }

}
