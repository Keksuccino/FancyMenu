package de.keksuccino.fancymenu.customization.layout.editor.widget;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class LayoutEditorWidgetRenderContext {

    private static final ThreadLocal<Deque<RenderState>> RENDER_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private LayoutEditorWidgetRenderContext() {
    }

    public static void beginBodyRender(int offsetX, int offsetY, double scale) {
        RenderState state = new RenderState(offsetX, offsetY, scale);
        RENDER_STACK.get().addLast(state);
    }

    public static void endBodyRender() {
        Deque<RenderState> stack = RENDER_STACK.get();
        if (!stack.isEmpty()) {
            stack.removeLast();
        }
    }

    public static boolean isBodyRenderActive() {
        return getActiveState() != null;
    }

    public static int getActiveBodyRenderOffsetX() {
        RenderState state = getActiveState();
        return state != null ? state.offsetX : 0;
    }

    public static int getActiveBodyRenderOffsetY() {
        RenderState state = getActiveState();
        return state != null ? state.offsetY : 0;
    }

    public static double getActiveBodyRenderScaleFactor() {
        RenderState state = getActiveState();
        return state != null ? state.scale : 1.0;
    }

    private static @Nullable RenderState getActiveState() {
        Deque<RenderState> stack = RENDER_STACK.get();
        return stack.isEmpty() ? null : stack.peekLast();
    }

    private static final class RenderState {
        private final int offsetX;
        private final int offsetY;
        private final double scale;

        private RenderState(int offsetX, int offsetY, double scale) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scale = scale;
        }
    }
}
