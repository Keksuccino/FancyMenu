package de.keksuccino.fancymenu.customization.element.elements.musiccontroller;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

//TODO übernehmen
public class MusicControllerElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.of(new Color(124, 217, 2));

    public boolean playMenuMusic = true;
    public boolean playWorldMusic = true;

    public MusicControllerElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void tick() {

        super.tick();

        if (!this.shouldRender()) return;

        if (!isEditor()) MusicControllerHandler.notify(this);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        if (isEditor()) {
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();
            RenderSystem.enableBlend();
            graphics.fill(x, y, x + w, y + h, BACKGROUND_COLOR.getColorInt());
            graphics.enableScissor(x, y, x + w, y + h);
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getDisplayName(), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
            graphics.disableScissor();
            RenderingUtils.resetShaderColor(graphics);
        }

    }

}
