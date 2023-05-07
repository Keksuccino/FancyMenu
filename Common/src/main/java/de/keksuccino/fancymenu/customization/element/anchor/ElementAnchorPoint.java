package de.keksuccino.fancymenu.customization.element.anchor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
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
        return element.baseX;
    }

    public int getElementPositionY(@NotNull AbstractElement element) {
        return element.baseY;
    }

    public int getDefaultElementPositionX(@NotNull AbstractElement element) {
        return element.baseX;
    }

    public int getDefaultElementPositionY(@NotNull AbstractElement element) {
        return element.baseY;
    }

    public int getOriginPositionX() {
        return this.getElementPositionX(AbstractElement.EMPTY_ELEMENT);
    }

    public int getOriginPositionY() {
        return this.getElementPositionY(AbstractElement.EMPTY_ELEMENT);
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

    // ---------------------------------

    public static class AnchorTopLeft extends ElementAnchorPoint {

        AnchorTopLeft() {
            super("top-left");
        }

    }

    //TODO resize offset für alle anchor points adden

    public static class AnchorMidLeft extends ElementAnchorPoint {

        AnchorMidLeft() {
            super("mid-left");
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            return (AbstractElement.getScreenHeight() / 2) + element.baseY;
        }

        @Override
        public int getDefaultElementPositionY(@NotNull AbstractElement element) {
            return -(element.getHeight() / 2);
        }

    }

    public static class AnchorBottomLeft extends ElementAnchorPoint {

        AnchorBottomLeft() {
            super("bottom-left");
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            return AbstractElement.getScreenHeight() + element.baseY;
        }

        @Override
        public int getDefaultElementPositionY(@NotNull AbstractElement element) {
            return -element.getHeight();
        }

    }

    public static class AnchorTopCenter extends ElementAnchorPoint {

        AnchorTopCenter() {
            super("top-centered");
        }

        @Override
        public int getElementPositionX(@NotNull AbstractElement element) {
            return (AbstractElement.getScreenWidth() / 2) + element.baseX;
        }

        @Override
        public int getDefaultElementPositionX(@NotNull AbstractElement element) {
            return -(element.getWidth() / 2);
        }

    }

    public static class AnchorMidCenter extends ElementAnchorPoint {

        AnchorMidCenter() {
            super("mid-centered");
        }

        @Override
        public int getElementPositionX(@NotNull AbstractElement element) {
            return (AbstractElement.getScreenWidth() / 2) + element.baseX;
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            return (AbstractElement.getScreenHeight() / 2) + element.baseY;
        }

        @Override
        public int getDefaultElementPositionX(@NotNull AbstractElement element) {
            return -(element.getWidth() / 2);
        }

        @Override
        public int getDefaultElementPositionY(@NotNull AbstractElement element) {
            return -(element.getHeight() / 2);
        }

    }

    public static class AnchorBottomCenter extends ElementAnchorPoint {

        AnchorBottomCenter() {
            super("bottom-centered");
        }

        @Override
        public int getElementPositionX(@NotNull AbstractElement element) {
            return (AbstractElement.getScreenWidth() / 2) + element.baseX;
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            return AbstractElement.getScreenHeight() + element.baseY;
        }

        @Override
        public int getDefaultElementPositionX(@NotNull AbstractElement element) {
            return -(element.getWidth() / 2);
        }

        @Override
        public int getDefaultElementPositionY(@NotNull AbstractElement element) {
            return -element.getHeight();
        }

    }

    public static class AnchorTopRight extends ElementAnchorPoint {

        AnchorTopRight() {
            super("top-right");
        }

        @Override
        public int getElementPositionX(@NotNull AbstractElement element) {
            return AbstractElement.getScreenWidth() + element.baseX;
        }

        @Override
        public int getDefaultElementPositionX(@NotNull AbstractElement element) {
            return -element.getWidth();
        }

    }

    public static class AnchorMidRight extends ElementAnchorPoint {

        AnchorMidRight() {
            super("mid-right");
        }

        @Override
        public int getElementPositionX(@NotNull AbstractElement element) {
            return AbstractElement.getScreenWidth() + element.baseX;
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            return (AbstractElement.getScreenHeight() / 2) + element.baseY;
        }

        @Override
        public int getDefaultElementPositionX(@NotNull AbstractElement element) {
            return -element.getWidth();
        }

        @Override
        public int getDefaultElementPositionY(@NotNull AbstractElement element) {
            return -(element.getHeight() / 2);
        }

    }

    public static class AnchorBottomRight extends ElementAnchorPoint {

        AnchorBottomRight() {
            super("bottom-right");
        }

        @Override
        public int getElementPositionX(@NotNull AbstractElement element) {
            return AbstractElement.getScreenWidth() + element.baseX;
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            return AbstractElement.getScreenHeight() + element.baseY;
        }

        @Override
        public int getDefaultElementPositionX(@NotNull AbstractElement element) {
            return -element.getWidth();
        }

        @Override
        public int getDefaultElementPositionY(@NotNull AbstractElement element) {
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

    }

}
