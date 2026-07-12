package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import java.util.List;
import java.util.function.ToIntFunction;

final class TextEditorMouseSelectionController {

    private TextEditorMouseSelectionController() {}

    static double clampEndpoint(double coordinate, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, coordinate));
    }

    static <T> int findClosestVisibleLineIndex(List<T> lines, double mouseY, int editorTop, int editorBottom, ToIntFunction<T> lineY, ToIntFunction<T> lineHeight) {
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int index = 0; index < lines.size(); index++) {
            T line = lines.get(index);
            int rawLineY = lineY.applyAsInt(line);
            int visibleTop = Math.max(rawLineY, editorTop);
            int visibleBottom = Math.min(rawLineY + lineHeight.applyAsInt(line), editorBottom);
            if (visibleBottom <= visibleTop) {
                continue;
            }
            // Hit boxes are half-open so a shared boundary belongs to the following line.
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

    static boolean isCaptureValid(boolean hasWindow, boolean windowVisible, boolean windowInputLocked, boolean windowFocused, boolean leftMouseDown) {
        return leftMouseDown && (!hasWindow || (windowVisible && !windowInputLocked && windowFocused));
    }
}
