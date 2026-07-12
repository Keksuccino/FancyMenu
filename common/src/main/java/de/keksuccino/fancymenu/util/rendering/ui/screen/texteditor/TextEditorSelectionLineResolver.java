package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.ToIntFunction;

final class TextEditorSelectionLineResolver {

    private TextEditorSelectionLineResolver() {}

    @Nullable
    static <T> T findClosestVisibleLineAtY(List<T> lines, double mouseY, int editorTop, int editorBottom, ToIntFunction<T> lineY, ToIntFunction<T> lineHeight) {
        T closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (T line : lines) {
            int lineTop = lineY.applyAsInt(line);
            int visibleTop = Math.max(lineTop, editorTop);
            int visibleBottom = Math.min(lineTop + lineHeight.applyAsInt(line), editorBottom);
            if (visibleBottom <= visibleTop) {
                continue;
            }
            // Widget hit boxes use a half-open vertical range. Resolve containment first so a shared boundary belongs
            // to the following line instead of tying at distance zero and incorrectly retaining the previous line.
            if ((mouseY >= visibleTop) && (mouseY < visibleBottom)) {
                return line;
            }
            double distance = mouseY < visibleTop ? visibleTop - mouseY : mouseY - visibleBottom;
            if (distance < closestDistance) {
                closest = line;
                closestDistance = distance;
            }
        }
        return closest;
    }

}
