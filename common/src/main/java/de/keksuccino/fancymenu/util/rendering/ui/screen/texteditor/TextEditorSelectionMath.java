package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import java.util.List;
import java.util.function.ToIntFunction;

final class TextEditorSelectionMath {

    private TextEditorSelectionMath() {
    }

    /**
     * Resolves a line from its clipped vertical interval. Containment is checked before distance so an exact shared
     * boundary belongs to the following line, matching the half-open hit boxes used by vanilla widgets.
     */
    static <T> int findClosestVisibleIndex(double mouseY, int editorTop, int editorBottom, List<T> lines, ToIntFunction<T> yGetter, ToIntFunction<T> heightGetter) {
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int index = 0; index < lines.size(); index++) {
            T line = lines.get(index);
            int visibleTop = Math.max(yGetter.applyAsInt(line), editorTop);
            int visibleBottom = Math.min(yGetter.applyAsInt(line) + heightGetter.applyAsInt(line), editorBottom);
            if (visibleBottom <= visibleTop) {
                continue;
            }
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
