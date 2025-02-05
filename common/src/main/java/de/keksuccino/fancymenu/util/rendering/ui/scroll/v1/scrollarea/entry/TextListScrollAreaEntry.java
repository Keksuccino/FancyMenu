package de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.awt.*;
import java.util.function.Consumer;

public class TextListScrollAreaEntry extends ScrollAreaEntry {

    public Color listDotColor;
    protected Component text;
    protected int textWidth;
    public Font font = Minecraft.getInstance().font;
    protected Consumer<TextListScrollAreaEntry> onClickCallback;

    public TextListScrollAreaEntry(ScrollArea parent, @NotNull Component text, @NotNull Color listDotColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
        super(parent, 0, 16);
        this.listDotColor = listDotColor;
        this.onClickCallback = onClick;
        this.setText(text);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        super.render(graphics, mouseX, mouseY, partial);

        int centerY = this.getY() + (this.getHeight() / 2);

        renderListingDot(graphics, this.getX() + 5, centerY - 2, this.listDotColor);

        graphics.drawString(this.font, this.text, (this.getX() + 5 + 4 + 3), (centerY - (this.font.lineHeight / 2)), -1, false);

    }

    @Override
    public void onClick(ScrollAreaEntry entry) {
        this.onClickCallback.accept((TextListScrollAreaEntry) entry);
    }

    public void setText(@NotNull Component text) {
        this.text = text;
        this.textWidth = this.font.width(this.text);
        this.setWidth(5 + 4 + 3 + this.textWidth + 5);
    }

    public Component getText() {
        return this.text;
    }

    public int getTextWidth() {
        return this.textWidth;
    }

}
