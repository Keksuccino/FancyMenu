package de.keksuccino.fancymenu.events.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractSelectionList;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class RenderGuiListHeaderFooterEvent extends EventBase {

    protected AbstractSelectionList<?> list;
    protected PoseStack pose;

    protected RenderGuiListHeaderFooterEvent(@NotNull PoseStack pose, @NotNull AbstractSelectionList<?> list) {
        this.list = Objects.requireNonNull(list);
        this.pose = Objects.requireNonNull(pose);
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
    public PoseStack getPoseStack() {
        return this.pose;
    }

    public static class Pre extends RenderGuiListHeaderFooterEvent {

        public Pre(PoseStack pose, AbstractSelectionList<?> list) {
            super(pose, list);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

    }

    public static class Post extends RenderGuiListHeaderFooterEvent {

        public Post(PoseStack pose, AbstractSelectionList<?> list) {
            super(pose, list);
        }

    }

}
