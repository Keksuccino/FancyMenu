package de.keksuccino.fancymenu.events.widget;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class RenderTabNavigationBarHeaderBackgroundEvent extends EventBase {

    private final TabNavigationBar tabNavigationBar;
    private final GuiGraphics graphics;
    private final int headerWidth;
    private final int headerHeight;

    protected RenderTabNavigationBarHeaderBackgroundEvent(@NotNull TabNavigationBar tabNavigationBar, @NotNull GuiGraphics graphics, int headerWidth, int headerHeight) {
        this.tabNavigationBar = Objects.requireNonNull(tabNavigationBar);
        this.graphics = Objects.requireNonNull(graphics);
        this.headerWidth = headerWidth;
        this.headerHeight = headerHeight;
    }

    public TabNavigationBar getTabNavigationBar() {
        return tabNavigationBar;
    }

    public GuiGraphics getGraphics() {
        return graphics;
    }

    public int getHeaderWidth() {
        return headerWidth;
    }

    public int getHeaderHeight() {
        return headerHeight;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class Pre extends RenderTabNavigationBarHeaderBackgroundEvent {

        public Pre(@NotNull TabNavigationBar tabNavigationBar, @NotNull GuiGraphics graphics, int headerWidth, int headerHeight) {
            super(tabNavigationBar, graphics, headerWidth, headerHeight);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

    }

    public static class Post extends RenderTabNavigationBarHeaderBackgroundEvent {

        public Post(@NotNull TabNavigationBar tabNavigationBar, @NotNull GuiGraphics graphics, int headerWidth, int headerHeight) {
            super(tabNavigationBar, graphics, headerWidth, headerHeight);
        }

    }

}
