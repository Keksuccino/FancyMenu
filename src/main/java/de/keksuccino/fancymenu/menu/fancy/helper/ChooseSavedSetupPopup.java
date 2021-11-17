package de.keksuccino.fancymenu.menu.fancy.helper;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.MenuBar;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.function.Consumer;

public class ChooseSavedSetupPopup extends FMPopup {

    public Color overlayColor;
    protected ScrollArea scroll;
    protected Consumer<File> callback;
    protected AdvancedButton chooseButton;
    protected AdvancedButton closeButton;
    protected SetupEntry focused;

    protected int lastWidth = 0;
    protected int lastHeight = 0;

    public ChooseSavedSetupPopup(Consumer<File> callback) {
        super(240);

        this.overlayColor = new Color(26, 26, 26);
        this.callback = callback;

        KeyboardHandler.addKeyPressedListener(this::onEscapePressed);

        this.updateScrollList();

        this.chooseButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.choosefile.choose", new String[0]), true, (press) -> {
            if (this.focused != null) {
                this.close();
            }
        });
        this.addButton(this.chooseButton);
        this.colorizePopupButton(this.chooseButton);

        this.closeButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.yesno.cancel", new String[0]), true, (press) -> {
            if (this.callback != null) {
                this.callback.accept(null);
            }
            this.setDisplayed(false);
        });
        this.addButton(this.closeButton);
        this.colorizePopupButton(this.closeButton);

    }

    public void render(int mouseX, int mouseY, GuiScreen renderIn) {

        super.render(mouseX, mouseY, renderIn);

        if (this.lastWidth != renderIn.width || this.lastHeight != renderIn.height) {
            this.updateScrollList();
            this.focused = null;
        }
        this.lastWidth = renderIn.width;
        this.lastHeight = renderIn.height;

        this.scroll.height = renderIn.height - 100;
        this.scroll.y = 40;
        this.scroll.x = renderIn.width / 2 - this.scroll.width / 2;
        this.scroll.render();

        drawRect(0, 0, renderIn.width, 40, this.overlayColor.getRGB());
        drawRect(0, renderIn.height - 60, renderIn.width, renderIn.height, this.overlayColor.getRGB());

        drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§l" + Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved"), renderIn.width / 2, 17, Color.WHITE.getRGB());

        this.chooseButton.x = renderIn.width / 2 - this.chooseButton.width - 5;
        this.chooseButton.y = renderIn.height - 40;

        this.closeButton.x = renderIn.width / 2 + 5;
        this.closeButton.y = renderIn.height - 40;

        this.renderButtons(mouseX, mouseY);

        if ((this.focused != null) && !this.focused.focused) {
            this.focused = null;
        }

        //Render hovered entry tooltip
        SetupEntry hoveredEntry = getHoveredEntry();
        if (hoveredEntry != null) {
            File props = new File(hoveredEntry.setupFolder.getPath() + "/setup.properties");
            if (props.isFile()) {
                SetupSharingEngine.SetupProperties sp = SetupSharingEngine.deserializePropertiesFile(props.getPath());
                if (sp != null) {
                    String modified = "???";
                    try {
                        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(hoveredEntry.setupFolder.lastModified()), ZoneId.systemDefault());
                        DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
                        dtf.withZone(ZoneId.systemDefault());
                        dtf.withLocale(Locale.getDefault());
                        modified = dt.format(dtf);
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                    String[] desc = new String[] {
                            Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved.tooltip"),
                            " ",
                            Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved.tooltip.datemodified", modified),
                            Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved.tooltip.mcversion", sp.mcVersion),
                            Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved.tooltip.fmversion", sp.fmVersion),
                            Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved.tooltip.modloader", sp.modLoader)
                    };
                    renderDescription(MouseInput.getMouseX(), MouseInput.getMouseY(), desc);
                }
            }
        }

    }

    protected SetupEntry getHoveredEntry() {
        for (ScrollAreaEntry e : this.scroll.getEntries()) {
            if (e.isHovered()) {
                return (SetupEntry) e;
            }
        }
        return null;
    }

    public void updateScrollList() {

        this.scroll = new ScrollArea(0, 0, 200, 0);
        this.scroll.backgroundColor = new Color(255, 255, 255, 20);

        File f = SetupSharingEngine.FM_SETUPS_DIR;
        if (f.isDirectory()) {
            for (File setupDir : f.listFiles()) {
                if (SetupSharingEngine.isValidSetup(setupDir.getPath())) {
                    this.scroll.addEntry(new SetupEntry(this, setupDir));
                }
            }
        }

    }

    public void close() {
        this.setDisplayed(false);
        if (this.callback != null) {
            if (this.focused != null) {
                this.callback.accept(focused.setupFolder);
            } else {
                this.callback.accept(null);
            }
        }
    }

    public void onEscapePressed(KeyboardData d) {
        if (d.keycode == 256 && this.isDisplayed()) {
            if (this.callback != null) {
                this.callback.accept(null);
            }
            this.setDisplayed(false);
        }
    }

    private static void renderDescription(int mouseX, int mouseY, String... desc) {
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

            GlStateManager.enableBlend();

            renderDescriptionBackground(mouseX, mouseY, width, height);

            int i2 = 5;
            for (String s : desc) {
                Minecraft.getMinecraft().fontRenderer.drawString(s, mouseX + 5, mouseY + i2, -1);
                i2 += 10;
            }

        }
    }

    private static void renderDescriptionBackground(int x, int y, int width, int height) {
        Color borderColor = Color.WHITE;
        Color backColor = new Color(26, 26, 26, 250);
        //background
        drawRect(x, y, x + width, y + height, backColor.getRGB());
        //left border
        drawRect(x, y, x + 1, y + height, borderColor.getRGB());
        //top border
        drawRect(x, y, x + width, y + 1, borderColor.getRGB());
        //right border
        drawRect(x + width - 1, y, x + width, y + height, borderColor.getRGB());
        //bottom border
        drawRect(x, y + height - 1, x + width, y + height, borderColor.getRGB());
    }

    public static class SetupEntry extends ScrollAreaEntry {

        public ChooseSavedSetupPopup chooser;
        protected File setupFolder;

        protected int clickTick = 0;
        protected boolean clickPre = false;
        protected boolean click = false;
        protected boolean focused = false;

        public SetupEntry(ChooseSavedSetupPopup chooser, File setupFolder) {
            super(chooser.scroll);
            this.chooser = chooser;
            this.setupFolder = setupFolder;
        }

        @Override
        public void render() {
            if (this.isHovered() && this.isVisible() && MouseInput.isLeftMouseDown()) {
                this.focused = true;
                this.chooser.focused = this;
                if (!this.click) {
                    this.clickPre = true;
                    this.clickTick = 0;
                }
            }

            if (!this.isHovered() && MouseInput.isLeftMouseDown()) {
                this.focused = false;
            }

            super.render();
        }

        @Override
        public void renderEntry() {

            //Render FM logo icon
            GlStateManager.enableBlend();
            Minecraft.getMinecraft().getTextureManager().bindTexture(MenuBar.FM_LOGO_TEXTURE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawModalRectWithCustomSizedTexture(this.x, this.y, 0.0F, 0.0F, 20, 20, 20, 20);

            //Render setup name
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            String name = this.setupFolder.getName();
            int maxNameWidth = this.getWidth() - 30 - 8;
            if (font.getStringWidth(name) > maxNameWidth) {
                name = font.trimStringToWidth(name, maxNameWidth) + "..";
            }
            font.drawStringWithShadow(name, this.x + 30, this.y + 7, -1);

            if (!MouseInput.isLeftMouseDown() && this.clickPre) {
                this.click = true;
                this.clickPre = false;
                this.clickTick = 0;
            }

            if (this.click) {
                if (this.clickTick < 15) {
                    ++this.clickTick;
                } else {
                    this.click = false;
                    this.clickTick = 0;
                }

                if (MouseInput.isLeftMouseDown() && this.isHovered()) {
                    this.onClick();
                    this.click = false;
                    this.clickTick = 0;
                }
            }

            if (this.focused) {
                this.renderBorder();
            }

        }

        private void renderBorder() {
            drawRect(this.x, this.y, this.x + 1, this.y + this.getHeight(), Color.WHITE.getRGB());
            drawRect(this.x + this.getWidth() - 1, this.y, this.x + this.getWidth(), this.y + this.getHeight(), Color.WHITE.getRGB());
            drawRect(this.x, this.y, this.x + this.getWidth(), this.y + 1, Color.WHITE.getRGB());
            drawRect(this.x, this.y + this.getHeight() - 1, this.x + this.getWidth(), this.y + this.getHeight(), Color.WHITE.getRGB());
        }

        public void onClick() {
            this.chooser.close();
        }

        @Override
        public int getHeight() {
            return 20;
        }

    }

}
