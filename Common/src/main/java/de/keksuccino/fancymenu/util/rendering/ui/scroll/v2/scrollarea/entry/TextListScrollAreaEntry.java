package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TextListScrollAreaEntry extends ScrollAreaEntry {

    public DrawableColor listDotColor;
    protected Component text;
    protected int textWidth;
    public Font font = Minecraft.getInstance().font;
    protected Consumer<TextListScrollAreaEntry> onClickCallback;

    public TextListScrollAreaEntry(ScrollArea parent, @NotNull Component text, @NotNull DrawableColor listDotColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
        super(parent, 0, 16);
        this.listDotColor = listDotColor;
        this.onClickCallback = onClick;
        this.setText(text);
    }

    @Override
    public void renderEntry(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        float centerY = this.getY() + (this.getHeight() / 2f);
        renderListingDot(pose, (this.getX() + 5f), (centerY - 2f), this.listDotColor.getColorInt());
        this.font.draw(pose, this.text, (int)(this.getX() + 5f + 4f + 3f), (int)(centerY - (this.font.lineHeight / 2f)), -1);
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