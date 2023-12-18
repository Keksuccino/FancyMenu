package de.keksuccino.fancymenu.events.widget;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractSelectionList;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class RenderGuiListHeaderFooterEvent extends EventBase {

    protected AbstractSelectionList<?> list;
    protected GuiGraphics graphics;

    protected RenderGuiListHeaderFooterEvent(@NotNull GuiGraphics graphics, @NotNull AbstractSelectionList<?> list) {
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
    public IMixinAbstractSelectionList getAccessor() {
        return (IMixinAbstractSelectionList) this.list;
    }

    @NotNull
    public GuiGraphics getGraphics() {
        return graphics;
    }

    public static class Pre extends RenderGuiListHeaderFooterEvent {

        public Pre(GuiGraphics graphics, AbstractSelectionList<?> list) {
            super(graphics, list);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

    }

    public static class Post extends RenderGuiListHeaderFooterEvent {

        public Post(GuiGraphics graphics, AbstractSelectionList<?> list) {
            super(graphics, list);
        }

    }

}
