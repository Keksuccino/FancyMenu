package de.keksuccino.fancymenu.customization.element.anchor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ElementAnchorPoint {

    private final String name;

    public ElementAnchorPoint(@NotNull String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Component getDisplayName() {
        return Component.translatable("fancymenu.element.anchor_point." + this.getName().replace("-", "_"));
    }

    public int getElementPositionX(@NotNull AbstractElement element) {
        return this.getOriginX(element) + element.baseX;
    }

    public int getElementPositionY(@NotNull AbstractElement element) {
        return this.getOriginY(element) + element.baseY;
    }

    public int getOriginX(@NotNull AbstractElement element) {
        return 0;
    }

    public int getOriginY(@NotNull AbstractElement element) {
        return 0;
    }

    public int getDefaultElementBaseX(@NotNull AbstractElement element) {
        return 0;
    }

    public int getDefaultElementBaseY(@NotNull AbstractElement element) {
        return 0;
    }

    public int getResizePositionOffsetX(@NotNull AbstractElement element, int mouseTravelX, @NotNull AbstractEditorElement.ResizeGrabberType resizeGrabberType) {
        if (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.RIGHT) {
            return 0;
        }
        if (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.LEFT) {
            return mouseTravelX;
        }
        return 0;
    }

    public int getResizePositionOffsetY(@NotNull AbstractElement element, int mouseTravelY, @NotNull AbstractEditorElement.ResizeGrabberType resizeGrabberType) {
        if (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP) {
            return mouseTravelY;
        }
        if (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM) {
            return 0;
        }
        return 0;
    }

    protected static int getScreenWidth() {
        return AbstractElement.getScreenWidth();
    }

    protected static int getScreenHeight() {
        return AbstractElement.getScreenHeight();
    }

    protected static boolean isEditor() {
        return Minecraft.getInstance().screen instanceof LayoutEditorScreen;
    }

    // ---------------------------------

    public static class AnchorTopLeft extends ElementAnchorPoint {

        AnchorTopLeft() {
            super("top-left");
        }

    }

    public static class AnchorMidLeft extends ElementAnchorPoint {

        AnchorMidLeft() {
            super("mid-left");
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            return getScreenHeight() / 2;
        }

        @Override
        public int getDefaultElementBaseY(@NotNull AbstractElement element) {
            return -(element.getHeight() / 2);
        }

    }

    public static class AnchorBottomLeft extends ElementAnchorPoint {

        AnchorBottomLeft() {
            super("bottom-left");
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            return getScreenHeight();
        }

        @Override
        public int getDefaultElementBaseY(@NotNull AbstractElement element) {
            return -element.getHeight();
        }

    }

    public static class AnchorTopCenter extends ElementAnchorPoint {

        AnchorTopCenter() {
            super("top-centered");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            return getScreenWidth() / 2;
        }

        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            return -(element.getWidth() / 2);
        }

    }

    public static class AnchorMidCenter extends ElementAnchorPoint {

        AnchorMidCenter() {
            super("mid-centered");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            return getScreenWidth() / 2;
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            return getScreenHeight() / 2;
        }

        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            return -(element.getWidth() / 2);
        }

        @Override
        public int getDefaultElementBaseY(@NotNull AbstractElement element) {
            return -(element.getHeight() / 2);
        }

    }

    public static class AnchorBottomCenter extends ElementAnchorPoint {

        AnchorBottomCenter() {
            super("bottom-centered");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            return getScreenWidth() / 2;
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            return getScreenHeight();
        }

        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            return -(element.getWidth() / 2);
        }

        @Override
        public int getDefaultElementBaseY(@NotNull AbstractElement element) {
            return -element.getHeight();
        }

    }

    public static class AnchorTopRight extends ElementAnchorPoint {

        AnchorTopRight() {
            super("top-right");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            return getScreenWidth();
        }

        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            return -element.getWidth();
        }

    }

    public static class AnchorMidRight extends ElementAnchorPoint {

        AnchorMidRight() {
            super("mid-right");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            return getScreenWidth();
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            return getScreenHeight() / 2;
        }

        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            return -element.getWidth();
        }

        @Override
        public int getDefaultElementBaseY(@NotNull AbstractElement element) {
            return -(element.getHeight() / 2);
        }

    }

    public static class AnchorBottomRight extends ElementAnchorPoint {

        AnchorBottomRight() {
            super("bottom-right");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            return getScreenWidth();
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            return getScreenHeight();
        }

        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            return -element.getWidth();
        }

        @Override
        public int getDefaultElementBaseY(@NotNull AbstractElement element) {
            return -element.getHeight();
        }

    }

    public static class AnchorVanilla extends ElementAnchorPoint {

        AnchorVanilla() {
            super("vanilla");
        }

    }

    public static class AnchorElement extends ElementAnchorPoint {

        AnchorElement() {
            super("element");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            AbstractElement anchor = element.getElementAnchorPointElement();
            if (anchor != null) {
                return anchor.getX();
            }
            return super.getOriginX(element);
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            AbstractElement anchor = element.getElementAnchorPointElement();
            if (anchor != null) {
                return anchor.getY();
            }
            return super.getOriginY(element);
        }

        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            AbstractElement anchor = element.getElementAnchorPointElement();
            if (anchor != null) {
                return anchor.getHeight();
            }
            return super.getDefaultElementBaseX(element);
        }

    }

    public static class AnchorAuto extends ElementAnchorPoint {

        public static final int EDGE_ZONE_SIZE = 5;

        AnchorAuto() {
            super("auto");
        }

        @Override
        public int getOriginX(@NotNull AbstractElement element) {
            return getScreenWidth() / 2;
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            return getScreenHeight() / 2;
        }

        //Used when changing the anchor point
        @Override
        public int getDefaultElementBaseX(@NotNull AbstractElement element) {
            return -(element.getWidth() / 2);
        }

        //Used when changing the anchor point
        @Override
        public int getDefaultElementBaseY(@NotNull AbstractElement element) {
            return -(element.getHeight() / 2);
        }

        @Override
        public int getElementPositionX(@NotNull AbstractElement element) {
            int x = super.getElementPositionX(element);
            if (x < EDGE_ZONE_SIZE) {
                x = EDGE_ZONE_SIZE;
            }
            if (x > (getScreenWidth() - EDGE_ZONE_SIZE - element.getWidth())) {
                x = getScreenWidth() - EDGE_ZONE_SIZE - element.getWidth();
            }
            return x;
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            int y = super.getElementPositionY(element);
            if (y < EDGE_ZONE_SIZE) {
                y = EDGE_ZONE_SIZE;
            }
            if (y > (getScreenHeight() - EDGE_ZONE_SIZE - element.getHeight())) {
                y = getScreenHeight() - EDGE_ZONE_SIZE - element.getHeight();
            }
            return y;
        }

    }

}
