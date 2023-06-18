
package de.keksuccino.fancymenu.util.rendering.ui.texteditor.formattingrules;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorScreen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighlightPlaceholdersFormattingRule extends TextEditorFormattingRule {

    protected Style[] colorsByLevelOfNesting = new Style[] {
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor1.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor2.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor3.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor4.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor5.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor6.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor7.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor8.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor9.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor10.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor11.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorScheme().textEditorTextFormattingNestedTextColor12.getColorInt())
    };

    protected Map<TextEditorLine, List<PlaceholderIndexPair>> placeholderIndexes = new HashMap<>();

    @Override
    public void resetRule(TextEditorScreen editor) {
        placeholderIndexes.clear();
    }

    @Override
    public @Nullable Style getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, int atCharacterIndexTotal, TextEditorScreen editor) {
        String s = String.valueOf(atCharacterInLine);
        if (s.equals("{") && inLine.getValue().substring(atCharacterIndexInLine).startsWith("{\"placeholder\":\"")) {
            int endIndex = findPlaceholderEndIndex(inLine.getValue(), atCharacterIndexInLine);
            if (endIndex > -1) {
                if (!this.placeholderIndexes.containsKey(inLine)) {
                    this.placeholderIndexes.put(inLine, new ArrayList<>());
                }
                this.placeholderIndexes.get(inLine).add(new PlaceholderIndexPair(atCharacterIndexInLine, endIndex));
            }
        }
        int depth = this.getDepth(atCharacterIndexInLine, inLine);
        if (depth > -1) {
            if (depth > this.colorsByLevelOfNesting.length-1) {
                depth = this.colorsByLevelOfNesting.length-1;
            }
            return this.colorsByLevelOfNesting[depth];
        }
        return null;
    }

    private int getDepth(int charIndex, TextEditorLine line) {
        if (this.placeholderIndexes.containsKey(line)) {
            int depth = -1;
            for (PlaceholderIndexPair p : this.placeholderIndexes.get(line)) {
                if ((charIndex >= p.start) && (charIndex <= p.end)) {
                    depth++;
                }
            }
            return depth;
        }
        return -1;
    }

    private static int findPlaceholderEndIndex(String in, int startIndex) {
        if (in.substring(startIndex).startsWith("{") && ((startIndex == 0) || !in.substring(startIndex-1).startsWith("\\"))) {
            int currentIndex = startIndex+1;
            int depth = 0;
            for (char c : in.substring(startIndex+1).toCharArray()) {
                if (String.valueOf(c).equals("{") && !in.substring(currentIndex-1).startsWith("\\")) {
                    depth++;
                } else if (String.valueOf(c).equals("}") && !in.substring(currentIndex-1).startsWith("\\")) {
                    if (depth <= 0) {
                        return currentIndex;
                    } else {
                        depth--;
                    }
                }
                currentIndex++;
            }
        }
        return -1;
    }

    public static class PlaceholderIndexPair {
        int start;
        int end;
        public PlaceholderIndexPair(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

}
