package de.keksuccino.fancymenu.customization.element.elements.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
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

    @Nullable
    public ResourceSupplier<ITexture> textureSupplier;
    public boolean repeat = false;
    public boolean nineSlice = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;
    @Nullable
    public WrappedMCEFBrowser browser = MCEFUtil.isMCEFLoaded() ? WrappedMCEFBrowser.build("https://google.de", true) : null;
    public int lastTickWidth = -1;
    public int lastTickHeight = -1;

    public BrowserElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void onDestroyElement() {
        CloseableUtils.closeQuietly(this.browser);
        super.onDestroyElement();
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

                this.browser.setPosition(x, y);

                if ((this.lastTickWidth != w) || (this.lastTickHeight != h)) {
                    this.browser.setSize(w, h);
                }
                this.lastTickWidth = w;
                this.lastTickHeight = h;

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
    public ITexture getTextureResource() {
        if (this.textureSupplier != null) return this.textureSupplier.get();
        return null;
    }

    public void restoreAspectRatio() {
        ITexture t = this.getTextureResource();
        AspectRatio ratio = (t != null) ? t.getAspectRatio() : new AspectRatio(10, 10);
        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
    }

}
