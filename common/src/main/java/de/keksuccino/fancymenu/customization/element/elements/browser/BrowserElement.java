package de.keksuccino.fancymenu.customization.element.elements.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.List;

public class BrowserElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.of(Color.RED);

    @NotNull
    public String url = "https://docs.fancymenu.net";
    public boolean interactable = true;
    @Nullable
    public WrappedMCEFBrowser browser = null;
    public int lastTickWidth = -1;
    public int lastTickHeight = -1;

    public BrowserElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void afterConstruction() {
        if (MCEFUtil.isMCEFLoaded()) {
            this.browser = BrowserHandler.get(this.getInstanceIdentifier());
            if (this.browser == null) this.browser = WrappedMCEFBrowser.build(PlaceholderParser.replacePlaceholders(this.url), true, false);
            BrowserHandler.notifyHandler(this.getInstanceIdentifier(), this.browser);
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        if (this.browser == null) return null;
        return List.of(this.browser);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();

            if (this.browser != null) {

                BrowserHandler.notifyHandler(this.getInstanceIdentifier(), this.browser);

                this.browser.setPosition(x, y);

                if ((this.lastTickWidth != w) || (this.lastTickHeight != h)) {
                    this.browser.setSize(w, h);
                }
                this.lastTickWidth = w;
                this.lastTickHeight = h;

                String finalUrl = PlaceholderParser.replacePlaceholders(this.url);
                if (!finalUrl.equals(this.getLastTickUrl())) {
                    this.browser.setUrl(finalUrl);
                    this.setLastTickUrl(finalUrl);
                }

                this.browser.setInteractable(this.interactable);

                RenderSystem.enableBlend();

                this.browser.render(graphics, mouseX, mouseY, partial);

            } else {

                RenderSystem.enableBlend();
                graphics.fill(RenderType.guiOverlay(), x, y, x + w, y + h, BACKGROUND_COLOR.getColorInt());
                graphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("fancymenu.elements.browser.mcef_not_loaded.line_1").setStyle(Style.EMPTY.withBold(true)), x + (w / 2), y + (h / 2) - Minecraft.getInstance().font.lineHeight - 2, -1);
                graphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("fancymenu.elements.browser.mcef_not_loaded.line_2").setStyle(Style.EMPTY.withBold(true)), x + (w / 2), y + (h / 2) + 2, -1);

            }

        }

    }

    @Nullable
    public String getLastTickUrl() {
        return this.getMemory().getStringProperty("last_tick_url");
    }

    public void setLastTickUrl(@Nullable String url) {
        this.getMemory().putProperty("last_tick_url", url);
    }

}
