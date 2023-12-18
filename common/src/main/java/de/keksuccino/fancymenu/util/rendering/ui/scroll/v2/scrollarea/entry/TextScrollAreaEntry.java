package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry;

import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TextScrollAreaEntry extends ScrollAreaEntry {

    protected Component text;
    protected int textWidth;
    public Font font = Minecraft.getInstance().font;
    protected Consumer<TextScrollAreaEntry> onClickCallback;

    public TextScrollAreaEntry(ScrollArea parent, @NotNull Component text, @NotNull Consumer<TextScrollAreaEntry> onClick) {
        super(parent, 0, 14);
        this.onClickCallback = onClick;
        this.setText(text);
    }

    @Override
    public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        float centerY = this.getY() + (this.getHeight() / 2f);
        graphics.drawString(this.font, this.text, (int)(this.getX() + 5f), (int)(centerY - (this.font.lineHeight / 2f)), -1, false);
    }

    @Override
    public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        this.onClickCallback.accept((TextScrollAreaEntry) entry);
    }

    public void setText(@NotNull Component text) {
        this.text = text;
        this.textWidth = this.font.width(this.text);
        this.setWidth(5 + this.textWidth + 5);
    }

    public Component getText() {
        return this.text;
    }

    public int getTextWidth() {
        return this.textWidth;
    }

}
