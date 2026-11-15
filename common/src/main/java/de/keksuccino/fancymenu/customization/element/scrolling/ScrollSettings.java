package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.customization.element.style.PixelScrollGrabber;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import org.jetbrains.annotations.NotNull;
import java.util.Set;

/**
 * How a scrollable element scrolls its content: what the user can do, what the content does on its own, and what its scroll bars look like.<br>
 * The names of the settings are the same for every element. Only their descriptions say what gets scrolled, so every kind of element has its own.
 */
public final class ScrollSettings {

    public final Property<ScrollDirection> scrollDirection;
    /** If the user can scroll the content. Without it, the content only moves on its own and when something tells it to. */
    public final Property.BooleanProperty userScrolling;
    public final Property.FloatProperty scrollSpeed;
    public final Property.BooleanProperty smoothScrolling;
    public final Property.BooleanProperty dragScrolling;
    public final Property.BooleanProperty keyboardScrolling;
    public final Property<ScrollSnapMode> snapMode;
    public final Property<ScrollStartPosition> startPosition;
    /** If the content goes back to its start position every time the screen gets opened. Without it, it stays where it was until the game closes. */
    public final Property.BooleanProperty resetScrollOnOpen;
    public final Property.BooleanProperty autoScroll;
    /** How many screen pixels per second the content scrolls on its own. */
    public final Property.FloatProperty autoScrollSpeed;
    public final Property.BooleanProperty autoScrollLoop;
    public final Property.BooleanProperty autoScrollPauseOnHover;
    public final Property<ScrollBarVisibility> scrollBarVisibility;
    /** How thick the scroll bars are. Their grabbers are twice as long as they are thick, like the pixel art handle they are made for. */
    public final Property.IntegerProperty scrollBarThickness;
    public final Property.BooleanProperty verticalScrollBarLeft;
    public final Property.BooleanProperty horizontalScrollBarTop;

    private final String descriptionKeyBase;

    /**
     * @param holder The element that scrolls.
     * @param descriptionKeyBase The key the descriptions start with that say what the element scrolls, without a dot at its end.
     * @param defaultDirection The direction of an element that got saved without one.
     * @param snapModes The places the content of the element can come to rest at.
     */
    public ScrollSettings(@NotNull PropertyHolder holder, @NotNull String descriptionKeyBase, @NotNull ScrollDirection defaultDirection, @NotNull Set<ScrollSnapMode> snapModes) {
        this.descriptionKeyBase = descriptionKeyBase;
        this.scrollDirection = holder.putProperty(ScrollDirection.buildProperty(defaultDirection));
        this.userScrolling = holder.putProperty(Property.booleanProperty("user_scrolling", true, "fancymenu.elements.scroll_area.scrolling.user"));
        this.scrollSpeed = holder.putProperty(Property.floatProperty("scroll_speed", 1.0F, "fancymenu.elements.scroll_area.scroll_speed", Property.NumericInputBehavior.<Float>builder().rangeInput(0.1F, 5.0F).build()));
        this.smoothScrolling = holder.putProperty(Property.booleanProperty("smooth_scrolling", true, "fancymenu.elements.scroll_area.scrolling.smooth"));
        this.dragScrolling = holder.putProperty(Property.booleanProperty("drag_scrolling", false, "fancymenu.elements.scroll_area.scrolling.drag"));
        this.keyboardScrolling = holder.putProperty(Property.booleanProperty("keyboard_scrolling", true, "fancymenu.elements.scroll_area.scrolling.keyboard"));
        this.snapMode = holder.putProperty(ScrollSnapMode.buildProperty(snapModes));
        this.startPosition = holder.putProperty(ScrollStartPosition.buildProperty());
        this.resetScrollOnOpen = holder.putProperty(Property.booleanProperty("reset_scroll_on_open", true, "fancymenu.elements.scroll_area.scrolling.reset_on_open"));
        this.autoScroll = holder.putProperty(Property.booleanProperty("auto_scroll", false, "fancymenu.elements.scroll_area.auto_scroll.enabled"));
        this.autoScrollSpeed = holder.putProperty(Property.floatProperty("auto_scroll_speed", 20.0F, "fancymenu.elements.scroll_area.auto_scroll.speed", Property.NumericInputBehavior.<Float>builder().rangeInput(2.0F, 200.0F).build()));
        this.autoScrollLoop = holder.putProperty(Property.booleanProperty("auto_scroll_loop", true, "fancymenu.elements.scroll_area.auto_scroll.loop"));
        this.autoScrollPauseOnHover = holder.putProperty(Property.booleanProperty("auto_scroll_pause_on_hover", true, "fancymenu.elements.scroll_area.auto_scroll.pause_on_hover"));
        this.scrollBarVisibility = holder.putProperty(ScrollBarVisibility.buildProperty());
        this.scrollBarThickness = holder.putProperty(Property.integerProperty("scroll_bar_thickness", PixelScrollGrabber.THICKNESS, "fancymenu.elements.scroll_area.scroll_bars.thickness", Property.NumericInputBehavior.<Integer>builder().rangeInput(4, 32).build()));
        this.verticalScrollBarLeft = holder.putProperty(Property.booleanProperty("vertical_scroll_bar_left", false, "fancymenu.elements.scroll_area.scroll_bars.vertical_left"));
        this.horizontalScrollBarTop = holder.putProperty(Property.booleanProperty("horizontal_scroll_bar_top", false, "fancymenu.elements.scroll_area.scroll_bars.horizontal_top"));
    }

    /**
     * Returns the key the descriptions start with that say what the element scrolls, without a dot at its end.
     */
    @NotNull
    public String getDescriptionKeyBase() {
        return this.descriptionKeyBase;
    }

    /**
     * Returns where the content comes to rest, which always is one of the places the content of the element can come to rest at.
     */
    @NotNull
    public ScrollSnapMode getSnapMode() {
        return this.snapMode.tryGetNonNullElse(ScrollSnapMode.NONE);
    }

}
