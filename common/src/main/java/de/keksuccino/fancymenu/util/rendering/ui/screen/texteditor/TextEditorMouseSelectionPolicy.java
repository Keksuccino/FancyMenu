package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import java.util.List;
import java.util.function.ToIntFunction;

final class TextEditorMouseSelectionPolicy {

    private TextEditorMouseSelectionPolicy() {
    }

    static boolean isCaptureValid(boolean windowOwnsCapture, boolean leftMouseDown) {
        return windowOwnsCapture && leftMouseDown;
    }

    static <T> int findClosestVisibleLineIndex(double mouseY, int editorTop, int editorBottom, List<T> lines, ToIntFunction<T> lineTop, ToIntFunction<T> lineHeight) {
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int index = 0; index < lines.size(); index++) {
            T line = lines.get(index);
            int top = lineTop.applyAsInt(line);
            int visibleTop = Math.max(top, editorTop);
            int visibleBottom = Math.min(top + lineHeight.applyAsInt(line), editorBottom);
            if (visibleBottom <= visibleTop) {
                continue;
            }
            // Widget hit boxes use a half-open vertical range. Resolve containment first so a shared boundary belongs
            // to the following line instead of tying at distance zero and incorrectly retaining the previous line.
            if ((mouseY >= visibleTop) && (mouseY < visibleBottom)) {
                return index;
            }
            double distance = mouseY < visibleTop ? visibleTop - mouseY : mouseY - visibleBottom;
            if (distance < closestDistance) {
                closestIndex = index;
                closestDistance = distance;
            }
        }
        return closestIndex;
    }

}
