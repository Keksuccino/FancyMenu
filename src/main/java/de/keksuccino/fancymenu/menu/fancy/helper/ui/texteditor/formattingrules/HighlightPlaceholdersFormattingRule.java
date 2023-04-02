//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.formattingrules;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.TextStyle;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorLine;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighlightPlaceholdersFormattingRule extends TextEditorFormattingRule {

    protected TextStyle[] colorsByLevelOfNesting = new TextStyle[] {
            new TextStyle().setColorRGB(new Color(235, 127, 127).getRGB()),
            new TextStyle().setColorRGB(new Color(235, 201, 127).getRGB()),
            new TextStyle().setColorRGB(new Color(190, 235, 127).getRGB()),
            new TextStyle().setColorRGB(new Color(127, 235, 230).getRGB()),
            new TextStyle().setColorRGB(new Color(127, 158, 235).getRGB()),
            new TextStyle().setColorRGB(new Color(150, 127, 235).getRGB()),
            new TextStyle().setColorRGB(new Color(212, 127, 235).getRGB()),
            new TextStyle().setColorRGB(new Color(245, 54, 54).getRGB()),
            new TextStyle().setColorRGB(new Color(245, 146, 54).getRGB()),
            new TextStyle().setColorRGB(new Color(245, 229, 54).getRGB()),
            new TextStyle().setColorRGB(new Color(105, 245, 54).getRGB()),
            new TextStyle().setColorRGB(new Color(54, 137, 245).getRGB())
    };

    protected Map<TextEditorLine, List<PlaceholderIndexPair>> placeholderIndexes = new HashMap<>();

    @Override
    public void resetRule(TextEditorScreen editor) {
        placeholderIndexes.clear();
    }

    @Override
    public TextStyle getStyle(char atCharacterInLine, int atCharacterIndexInLine, int cursorPosInLine, TextEditorLine inLine, int atCharacterIndexTotal, TextEditorScreen editor) {
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
