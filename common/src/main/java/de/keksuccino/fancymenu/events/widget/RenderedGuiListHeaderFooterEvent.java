package de.keksuccino.fancymenu.events.widget;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class RenderedGuiListHeaderFooterEvent extends EventBase {

    protected AbstractSelectionList<?> list;
    protected GuiGraphics graphics;

    public RenderedGuiListHeaderFooterEvent(@NotNull GuiGraphics graphics, @NotNull AbstractSelectionList<?> list) {
        this.list = Objects.requireNonNull(list);
        this.graphics = Objects.requireNonNull(graphics);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @NotNull
    public AbstractSelectionList<?> getList() {
        return this.list;
    }

    @NotNull
    public GuiGraphics getGraphics() {
        return graphics;
    }

}
