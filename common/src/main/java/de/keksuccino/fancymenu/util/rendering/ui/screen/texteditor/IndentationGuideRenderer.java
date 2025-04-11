package de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import java.awt.Color;
import java.util.*;

/**
 * Helper class to render indentation guides for the text editor.
 */
public class IndentationGuideRenderer {

    private final TextEditorScreen editor;
    private final Color guideColor;
    private int indentSize = 2; // Default indent size (2 spaces)
    private final Map<Integer, List<IndentGuide>> lineGuides = new HashMap<>(); // lineIndex -> list of guides to render
    private boolean needsUpdate = true;

    public IndentationGuideRenderer(TextEditorScreen editor) {
        this.editor = editor;
        this.guideColor = new Color(100, 100, 100, 60); // Subtle gray with transparency
    }

    /**
     * Mark that guides need to be recalculated.
     */
    public void markDirty() {
        this.needsUpdate = true;
    }

    /**
     * Renders the indentation guides.
     */
    public void render(GuiGraphics graphics) {
        if (needsUpdate) {
            detectIndentationSize();
            calculateIndentGuidePositions();
            needsUpdate = false;
        }

        // Get the character width for spacing
        int charWidth = Minecraft.getInstance().font.width(" ");

        // Render guides for visible lines
        for (TextEditorLine line : editor.getLines()) {
            if (!line.isInEditorArea()) {
                continue;
            }

            int lineIndex = line.lineIndex;
            List<IndentGuide> guides = lineGuides.getOrDefault(lineIndex, Collections.emptyList());

            int lineY = line.getY();
            int lineHeight = line.getHeight();

            for (IndentGuide guide : guides) {
                int xPos = line.getX() + (guide.indentPosition * charWidth);

                // Calculate the vertical position and height based on guide type
                int yPos = lineY;
                int height = lineHeight;

                // Adjust rendering for start/end of guides to create visual spacing
                if (guide.type == GuideType.START) {
                    // Start drawing guide from middle of the line
                    yPos = lineY + (lineHeight / 2);
                    height = lineHeight / 2;
                } else if (guide.type == GuideType.END) {
                    // End drawing guide at middle of the line
                    height = lineHeight / 2;
                }

                // Draw the guide line
                graphics.fill(xPos, yPos, xPos + 1, yPos + height, guideColor.getRGB());
            }
        }
    }

    /**
     * Try to detect the indentation size by analyzing the document.
     */
    private void detectIndentationSize() {
        Map<Integer, Integer> indentSizeCounts = new HashMap<>();
        int maxCount = 0;
        int detectedSize = indentSize; // Default

        for (int i = 1; i < editor.getLineCount(); i++) {
            TextEditorLine prevLine = editor.getLine(i-1);
            TextEditorLine line = editor.getLine(i);

            if (prevLine == null || line == null) continue;

            int prevSpaces = getLeadingSpaces(prevLine.getValue());
            int spaces = getLeadingSpaces(line.getValue());

            // If this line has more indent than previous
            if (spaces > prevSpaces && !line.getValue().trim().isEmpty() && !prevLine.getValue().trim().isEmpty()) {
                int diff = spaces - prevSpaces;
                if (diff > 0 && diff <= 8) { // Reasonable indent sizes
                    indentSizeCounts.put(diff, indentSizeCounts.getOrDefault(diff, 0) + 1);

                    if (indentSizeCounts.get(diff) > maxCount) {
                        maxCount = indentSizeCounts.get(diff);
                        detectedSize = diff;
                    }
                }
            }
        }

        // Only update if we have a confident detection
        if (maxCount >= 2) {
            indentSize = detectedSize;
        }
    }

    private int getLeadingSpaces(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Calculate positions where indent guides should be drawn.
     */
    private void calculateIndentGuidePositions() {
        lineGuides.clear();

        // Process all lines and create a map of indentation levels
        Map<Integer, Set<Integer>> activeIndentLevels = new HashMap<>(); // Line index -> active indent levels

        // First pass: build a raw indentation level map, ignoring empty lines for now
        analyzeLinesForIndentation(activeIndentLevels);

        // Second pass: process empty lines and ensure continuity
        processEmptyLines(activeIndentLevels);

        // Third pass: convert indentation levels to guides
        generateGuidesFromIndentLevels(activeIndentLevels);
    }

    private void analyzeLinesForIndentation(Map<Integer, Set<Integer>> activeIndentLevels) {
        // Track blocks of the same indentation level
        Map<Integer, IndentBlock> openBlocks = new HashMap<>(); // Indent level -> open block

        for (int i = 0; i < editor.getLineCount(); i++) {
            TextEditorLine line = editor.getLine(i);
            if (line == null) continue;

            String content = line.getValue();
            String trimmed = content.trim();

            // If this is a non-empty line
            if (!trimmed.isEmpty()) {
                int spaces = getLeadingSpaces(content);
                int level = spaces / indentSize;

                // Add all indent levels for this line
                Set<Integer> levels = new HashSet<>();
                for (int l = 1; l <= level; l++) {
                    levels.add(l);
                }

                activeIndentLevels.put(i, levels);

                // Track indentation blocks for later use
                if (level > 0) {
                    // Start a new block at this level if there isn't one already
                    if (!openBlocks.containsKey(level)) {
                        openBlocks.put(level, new IndentBlock(level, i));
                    }

                    // Check if this level is ending
                    boolean isBlockEnd = isBracketClosingLine(trimmed) ||
                            (i + 1 < editor.getLineCount() &&
                                    getNextNonEmptyLine(i) >= 0 &&
                                    getIndentLevel(editor.getLine(getNextNonEmptyLine(i))) < level);

                    if (isBlockEnd) {
                        // Close this block
                        openBlocks.remove(level);

                        // Close any deeper blocks
                        for (int l = level + 1; l < 20; l++) {
                            openBlocks.remove(l);
                        }
                    }
                } else {
                    // Level 0 - close all blocks
                    openBlocks.clear();
                }
            }
        }
    }

    private void processEmptyLines(Map<Integer, Set<Integer>> activeIndentLevels) {
        // Process empty lines to maintain continuity
        for (int i = 0; i < editor.getLineCount(); i++) {
            TextEditorLine line = editor.getLine(i);
            if (line == null) continue;

            String content = line.getValue();
            String trimmed = content.trim();

            // If this is an empty line
            if (trimmed.isEmpty()) {
                // Find previous and next non-empty lines
                int prevNonEmptyIndex = getPrevNonEmptyLine(i);
                int nextNonEmptyIndex = getNextNonEmptyLine(i);

                if (prevNonEmptyIndex >= 0 && nextNonEmptyIndex >= 0) {
                    // Get the common indent levels between prev and next
                    Set<Integer> prevLevels = activeIndentLevels.getOrDefault(prevNonEmptyIndex, Collections.emptySet());
                    Set<Integer> nextLevels = activeIndentLevels.getOrDefault(nextNonEmptyIndex, Collections.emptySet());

                    // Find common levels
                    Set<Integer> commonLevels = new HashSet<>(prevLevels);
                    commonLevels.retainAll(nextLevels);

                    // If there are common levels, pass them through this empty line
                    if (!commonLevels.isEmpty()) {
                        activeIndentLevels.put(i, commonLevels);
                    }
                }
            }
        }
    }

    private void generateGuidesFromIndentLevels(Map<Integer, Set<Integer>> activeIndentLevels) {
        // Convert indentation levels to guide rendering instructions
        for (int i = 0; i < editor.getLineCount(); i++) {
            Set<Integer> levels = activeIndentLevels.getOrDefault(i, Collections.emptySet());

            if (levels.isEmpty()) continue;

            for (int level : levels) {
                // Convert level to position
                int indentPos = level * indentSize;

                // Determine guide type
                GuideType type = GuideType.MIDDLE;

                // Check if this is the start of a guide
                if (!activeIndentLevels.getOrDefault(i - 1, Collections.emptySet()).contains(level)) {
                    type = GuideType.START;
                }

                // Check if this is the end of a guide
                if (!activeIndentLevels.getOrDefault(i + 1, Collections.emptySet()).contains(level)) {
                    // If it's already a START, make it a MIDDLE (very short guide)
                    if (type == GuideType.START) {
                        type = GuideType.MIDDLE;
                    } else {
                        type = GuideType.END;
                    }
                }

                // Add the guide for this line
                addGuide(i, indentPos, type);
            }
        }
    }

    private int getPrevNonEmptyLine(int currentLine) {
        for (int i = currentLine - 1; i >= 0; i--) {
            TextEditorLine line = editor.getLine(i);
            if (line != null && !line.getValue().trim().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getNextNonEmptyLine(int currentLine) {
        for (int i = currentLine + 1; i < editor.getLineCount(); i++) {
            TextEditorLine line = editor.getLine(i);
            if (line != null && !line.getValue().trim().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBracketOpeningLine(String trimmed) {
        return trimmed.endsWith("{") || trimmed.endsWith("[") || trimmed.endsWith("(");
    }

    private boolean isBracketClosingLine(String trimmed) {
        return trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")");
    }

    private int getIndentLevel(TextEditorLine line) {
        if (line == null) return 0;
        return getLeadingSpaces(line.getValue()) / indentSize;
    }

    private void addGuide(int lineIndex, int indentPosition, GuideType type) {
        lineGuides.computeIfAbsent(lineIndex, k -> new ArrayList<>())
                .add(new IndentGuide(indentPosition, type));
    }

    // Type of guide to render
    private enum GuideType {
        START,   // Guide starts here (render from middle of line to bottom)
        MIDDLE,  // Guide passes through here (render full line)
        END      // Guide ends here (render from top to middle of line)
    }

    // Helper class for representing an indentation block
    private static class IndentBlock {
        final int level;
        final int startLine;

        IndentBlock(int level, int startLine) {
            this.level = level;
            this.startLine = startLine;
        }
    }

    // Helper class for representing a guide to render
    private static class IndentGuide {
        final int indentPosition;
        GuideType type;

        IndentGuide(int indentPosition, GuideType type) {
            this.indentPosition = indentPosition;
            this.type = type;
        }
    }
}