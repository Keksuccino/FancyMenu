package de.keksuccino.fancymenu.customization.element.anchor;

import com.mojang.logging.LogUtils;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class ElementAnchorPoint {

    private static final Logger LOGGER = LogUtils.getLogger();

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
        return this.getOriginX(element) + element.posOffsetX;
    }

    public int getElementPositionY(@NotNull AbstractElement element) {
        return this.getOriginY(element) + element.posOffsetY;
    }

    public int getOriginX(@NotNull AbstractElement element) {
        return 0;
    }

    public int getOriginY(@NotNull AbstractElement element) {
        return 0;
    }

    public int getResizePositionOffsetX(@NotNull AbstractElement element, int mouseTravelX, @NotNull AbstractEditorElement.ResizeGrabberType resizeGrabberType) {
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.RIGHT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_RIGHT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_RIGHT)) {
            return 0;
        }
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_LEFT)) {
            return mouseTravelX;
        }
        return 0;
    }

    public int getResizePositionOffsetY(@NotNull AbstractElement element, int mouseTravelY, @NotNull AbstractEditorElement.ResizeGrabberType resizeGrabberType) {
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_RIGHT)) {
            return mouseTravelY;
        }
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_RIGHT)) {
            return 0;
        }
        return 0;
    }

    public int getStickyResizePositionCorrectionX(@NotNull AbstractElement element, int mouseTravelX, int oldOffsetX, int newOffsetX, int oldPosX, int newPosX, int oldWidth, int newWidth, AbstractEditorElement.@NotNull ResizeGrabberType resizeGrabberType) {
        // When using LEFT grabber: Keep right edge in place
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_LEFT)) {
            int newPosXRight = newPosX + newWidth;
            int oldPosXRight = oldPosX + oldWidth;
            return oldPosXRight - newPosXRight;
        }
        // When using RIGHT grabber: Keep left edge in place
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.RIGHT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_RIGHT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_RIGHT)) {
            return oldPosX - newPosX;
        }
        return 0;
    }

    public int getStickyResizePositionCorrectionY(@NotNull AbstractElement element, int mouseTravelY, int oldOffsetY, int newOffsetY, int oldPosY, int newPosY, int oldHeight, int newHeight, AbstractEditorElement.@NotNull ResizeGrabberType resizeGrabberType) {
        // When using TOP grabber: Keep bottom edge in place
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.TOP_RIGHT)) {
            int newPosYBottom = newPosY + newHeight;
            int oldPosYBottom = oldPosY + oldHeight;
            return oldPosYBottom - newPosYBottom;
        }
        // When using BOTTOM grabber: Keep top edge in place
        if ((resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_LEFT)
                || (resizeGrabberType == AbstractEditorElement.ResizeGrabberType.BOTTOM_RIGHT)) {
            return oldPosY - newPosY;
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
        public int getElementPositionY(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginY(element) - (element.getAbsoluteHeight() / 2)) + element.posOffsetY;
            }
            return super.getElementPositionY(element);
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
        public int getElementPositionY(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginY(element) - element.getAbsoluteHeight()) + element.posOffsetY;
            }
            return super.getElementPositionY(element);
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
        public int getElementPositionX(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginX(element) - (element.getAbsoluteWidth() / 2)) + element.posOffsetX;
            }
            return super.getElementPositionX(element);
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
        public int getElementPositionX(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginX(element) - (element.getAbsoluteWidth() / 2)) + element.posOffsetX;
            }
            return super.getElementPositionX(element);
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginY(element) - (element.getAbsoluteHeight() / 2)) + element.posOffsetY;
            }
            return super.getElementPositionY(element);
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
        public int getElementPositionX(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginX(element) - (element.getAbsoluteWidth() / 2)) + element.posOffsetX;
            }
            return super.getElementPositionX(element);
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginY(element) - element.getAbsoluteHeight()) + element.posOffsetY;
            }
            return super.getElementPositionY(element);
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
        public int getElementPositionX(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginX(element) - element.getAbsoluteWidth()) + element.posOffsetX;
            }
            return super.getElementPositionX(element);
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
        public int getElementPositionX(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginX(element) - element.getAbsoluteWidth()) + element.posOffsetX;
            }
            return super.getElementPositionX(element);
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginY(element) - (element.getAbsoluteHeight() / 2)) + element.posOffsetY;
            }
            return super.getElementPositionY(element);
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
        public int getElementPositionX(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginX(element) - element.getAbsoluteWidth()) + element.posOffsetX;
            }
            return super.getElementPositionX(element);
        }

        @Override
        public int getElementPositionY(@NotNull AbstractElement element) {
            if (element.stickyAnchor) {
                return (this.getOriginY(element) - element.getAbsoluteHeight()) + element.posOffsetY;
            }
            return super.getElementPositionY(element);
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
            AbstractElement anchor = element.getElementAnchorPointParent();
            if (anchor != null) {
                if (anchor == element) return super.getOriginX(element);
                if ((anchor.getElementAnchorPointParent() != null) && (anchor.getElementAnchorPointParent() == element)) return super.getOriginX(element);
                if (element.getElementAnchorPointParent() == element) return super.getOriginX(element);
                if (anchor.getElementAnchorPointParent() == anchor) return super.getOriginX(element);
                return anchor.getChildElementAnchorPointX();
            }
            return super.getOriginX(element);
        }

        @Override
        public int getOriginY(@NotNull AbstractElement element) {
            AbstractElement anchor = element.getElementAnchorPointParent();
            if (anchor != null) {
                if (anchor == element) return super.getOriginY(element);
                if ((anchor.getElementAnchorPointParent() != null) && (anchor.getElementAnchorPointParent() == element)) return super.getOriginY(element);
                if (element.getElementAnchorPointParent() == element) return super.getOriginY(element);
                if (anchor.getElementAnchorPointParent() == anchor) return super.getOriginY(element);
                return anchor.getChildElementAnchorPointY();
            }
            return super.getOriginY(element);
        }

    }

}
