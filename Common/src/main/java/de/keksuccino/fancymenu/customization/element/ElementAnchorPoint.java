package de.keksuccino.fancymenu.customization.element;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElementAnchorPoint {

    public static final ElementAnchorPoint VANILLA = new ElementAnchorPoint("vanilla");
    public static final ElementAnchorPoint ELEMENT = new ElementAnchorPoint("element") {
        @Override public int getElementPositionX(@NotNull AbstractElement element) { return ((element.anchorPointElement != null) ? element.anchorPointElement.getX() : 0) + element.rawX; }
        @Override public int getElementPositionY(@NotNull AbstractElement element) { return ((element.anchorPointElement != null) ? element.anchorPointElement.getY() : 0) + element.rawY; }
    };
    public static final ElementAnchorPoint TOP_LEFT = new ElementAnchorPoint("top-left");
    public static final ElementAnchorPoint MID_LEFT = new ElementAnchorPoint("mid-left") {
        @Override public int getElementPositionY(@NotNull AbstractElement element) { return (AbstractElement.getScreenHeight() / 2) + element.rawY; }
        @Override public int getDefaultElementPositionY(@NotNull AbstractElement element) { return -(element.getHeight() / 2); }
    };
    public static final ElementAnchorPoint BOTTOM_LEFT = new ElementAnchorPoint("bottom-left") {
        @Override public int getElementPositionY(@NotNull AbstractElement element) { return AbstractElement.getScreenHeight() + element.rawY; }
        @Override public int getDefaultElementPositionY(@NotNull AbstractElement element) { return -element.getHeight(); }
    };
    public static final ElementAnchorPoint TOP_CENTERED = new ElementAnchorPoint("top-centered") {
        @Override public int getElementPositionX(@NotNull AbstractElement element) { return (AbstractElement.getScreenWidth() / 2) + element.rawX; }
        @Override public int getDefaultElementPositionX(@NotNull AbstractElement element) { return -(element.getWidth() / 2); }
    };
    public static final ElementAnchorPoint MID_CENTERED = new ElementAnchorPoint("mid-centered") {
        @Override public int getElementPositionX(@NotNull AbstractElement element) { return (AbstractElement.getScreenWidth() / 2) + element.rawX; }
        @Override public int getElementPositionY(@NotNull AbstractElement element) { return (AbstractElement.getScreenHeight() / 2) + element.rawY; }
        @Override public int getDefaultElementPositionX(@NotNull AbstractElement element) { return -(element.getWidth() / 2); }
        @Override public int getDefaultElementPositionY(@NotNull AbstractElement element) { return -(element.getHeight() / 2); }
    };
    public static final ElementAnchorPoint BOTTOM_CENTERED = new ElementAnchorPoint("bottom-centered") {
        @Override public int getElementPositionX(@NotNull AbstractElement element) { return (AbstractElement.getScreenWidth() / 2) + element.rawX; }
        @Override public int getElementPositionY(@NotNull AbstractElement element) { return AbstractElement.getScreenHeight() + element.rawY; }
        @Override public int getDefaultElementPositionX(@NotNull AbstractElement element) { return -(element.getWidth() / 2); }
        @Override public int getDefaultElementPositionY(@NotNull AbstractElement element) { return -element.getHeight(); }
    };
    public static final ElementAnchorPoint TOP_RIGHT = new ElementAnchorPoint("top-right") {
        @Override public int getElementPositionX(@NotNull AbstractElement element) { return AbstractElement.getScreenWidth() + element.rawX; }
        @Override public int getDefaultElementPositionX(@NotNull AbstractElement element) { return -element.getWidth(); }
    };
    public static final ElementAnchorPoint MID_RIGHT = new ElementAnchorPoint("mid-right") {
        @Override public int getElementPositionX(@NotNull AbstractElement element) { return AbstractElement.getScreenWidth() + element.rawX; }
        @Override public int getElementPositionY(@NotNull AbstractElement element) { return (AbstractElement.getScreenHeight() / 2) + element.rawY; }
        @Override public int getDefaultElementPositionX(@NotNull AbstractElement element) { return -element.getWidth(); }
        @Override public int getDefaultElementPositionY(@NotNull AbstractElement element) { return -(element.getHeight() / 2); }
    };
    public static final ElementAnchorPoint BOTTOM_RIGHT = new ElementAnchorPoint("bottom-right") {
        @Override public int getElementPositionX(@NotNull AbstractElement element) { return AbstractElement.getScreenWidth() + element.rawX; }
        @Override public int getElementPositionY(@NotNull AbstractElement element) { return AbstractElement.getScreenHeight() + element.rawY; }
        @Override public int getDefaultElementPositionX(@NotNull AbstractElement element) { return -element.getWidth(); }
        @Override public int getDefaultElementPositionY(@NotNull AbstractElement element) { return -element.getHeight(); }
    };

    public static final ElementAnchorPoint[] ANCHOR_POINTS = new ElementAnchorPoint[] {VANILLA, ELEMENT, TOP_LEFT, MID_LEFT, BOTTOM_LEFT, TOP_CENTERED, MID_CENTERED, BOTTOM_CENTERED, TOP_RIGHT, MID_RIGHT, BOTTOM_RIGHT};

    private final String name;

    private ElementAnchorPoint(@NotNull String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Component getDisplayName() {
        return Component.translatable("fancymenu.element.anchor_point." + this.getName().replace("-", "_"));
    }

    public int getElementPositionX(@NotNull AbstractElement element) {
        return element.rawX;
    }

    public int getElementPositionY(@NotNull AbstractElement element) {
        return element.rawY;
    }

    public int getDefaultElementPositionX(@NotNull AbstractElement element) {
        return element.rawX;
    }

    public int getDefaultElementPositionY(@NotNull AbstractElement element) {
        return element.rawY;
    }

    public int getOriginPositionX() {
        return this.getElementPositionX(AbstractElement.EMPTY_ELEMENT);
    }

    public int getOriginPositionY() {
        return this.getElementPositionY(AbstractElement.EMPTY_ELEMENT);
    }

    @Nullable
    public static ElementAnchorPoint getByName(@NotNull String name) {
        if (name.equals("original")) {
            return VANILLA;
        }
        for (ElementAnchorPoint p : ANCHOR_POINTS) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

}
