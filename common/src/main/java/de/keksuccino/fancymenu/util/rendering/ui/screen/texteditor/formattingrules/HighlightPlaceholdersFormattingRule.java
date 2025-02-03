package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.formattingrules;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighlightPlaceholdersFormattingRule extends TextEditorFormattingRule {

    protected Style[] colorsByLevelOfNesting = new Style[] {
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_1.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_2.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_3.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_4.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_5.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_6.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_7.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_8.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_9.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_10.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_11.getColorInt()),
            Style.EMPTY.withColor(UIBase.getUIColorTheme().text_editor_text_formatting_nested_text_color_12.getColorInt())
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