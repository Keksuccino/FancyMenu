package de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TextScrollAreaEntry extends ScrollAreaEntry {

    private static final Logger LOGGER = LogManager.getLogger();

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        super.render(graphics, mouseX, mouseY, partial);

        int centerY = this.getY() + (this.getHeight() / 2);

        UIBase.drawStringWithoutShadow(graphics, font, this.text, (float)(this.getX() + 5), (float)(centerY - (this.font.lineHeight / 2)), -1);

    }

    @Override
    public void onClick(ScrollAreaEntry entry) {
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
