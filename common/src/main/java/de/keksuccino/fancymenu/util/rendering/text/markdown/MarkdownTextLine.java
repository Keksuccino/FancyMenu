package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.Renderable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkdownTextLine implements Renderable {

    public MarkdownRenderer parent;
    public float offsetX;
    public float offsetY;
    public boolean containsMultilineCodeBlockFragments = false;
    @NotNull
    public MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
    public boolean bulletListItemStartLine = false;
    public final Map<MarkdownTextFragment.CodeBlockContext, SingleLineCodeBlockPart> singleLineCodeBlockStartEndPairs = new HashMap<>();
    public final List<MarkdownTextFragment> fragments = new ArrayList<>();

    public MarkdownTextLine(@NotNull MarkdownRenderer parent) {
        this.parent = parent;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.onRender(graphics, mouseX, mouseY, partial, true);
    }

    protected void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partial, boolean shouldRender) {

        float textX = this.parent.x + this.offsetX;
        for (MarkdownTextFragment f : this.fragments) {
            f.x = textX;
            f.y = this.parent.y + this.offsetY;
            if (shouldRender) {
                f.render(graphics, mouseX, mouseY, partial);
            }
            textX += f.getRenderWidth();
        }

    }

    public void prepareLine() {
        this.prepareFragments();
        this.onRender(null, 0, 0, 0, false);
    }

    public void prepareFragments() {
        this.containsMultilineCodeBlockFragments = false;
        this.singleLineCodeBlockStartEndPairs.clear();
        this.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
        this.bulletListItemStartLine = false;
        for (MarkdownTextFragment f : this.fragments) {
            f.parentLine = this;
            f.startOfRenderLine = false;
            f.autoLineBreakAfter = false;
            if (f.codeBlockContext != null) {
                if (!f.codeBlockContext.singleLine) {
                    //Check if line is Multi-Line Code Block
                    this.containsMultilineCodeBlockFragments = true;
                } else {
                    //Discover Single-Line Code Block parts of the line. The fragments of a code block follow each other, so the last one found in this line ends the block's part of the line.
                    SingleLineCodeBlockPart part = this.singleLineCodeBlockStartEndPairs.get(f.codeBlockContext);
                    if (part == null) {
                        part = new SingleLineCodeBlockPart();
                        part.start = f;
                        this.singleLineCodeBlockStartEndPairs.put(f.codeBlockContext, part);
                    }
                    part.end = f;
                }
            }
        }
        if (!this.fragments.isEmpty()) {
            MarkdownTextFragment first = this.fragments.get(0);
            MarkdownTextFragment last = this.fragments.get(this.fragments.size()-1);
            //Set line alignment by first fragment
            this.alignment = first.alignment;
            //Set bullet list start by first fragment
            this.bulletListItemStartLine = first.bulletListItemStart;
            first.startOfRenderLine = true;
            last.autoLineBreakAfter = !last.naturalLineBreakAfter;
            // Don't set autoLineBreakAfter for tables
            if (last.isTable()) {
                last.autoLineBreakAfter = false;
            }
        }
    }

    public float getLineWidth() {
        float f = 0;
        MarkdownTextFragment last = null;
        for (MarkdownTextFragment frag : this.fragments) {
            f += frag.getRenderWidth();
            last = frag;
        }
        if (last != null) {
            if (last.text.endsWith(" ")) {
                f -= (this.parent.getUnscaledTextWidth(" ") * last.getScale());
            }
        }
        if (f < 0) {
            f = 0;
        }
        return f;
    }

    public float getLineHeight() {
        float f = 0;
        for (MarkdownTextFragment frag : this.fragments) {
            if (frag.getRenderHeight() > f) {
                f = frag.getRenderHeight();
            }
        }
        return f;
    }

    public boolean isAlignmentAllowed(@NotNull MarkdownRenderer.MarkdownLineAlignment alignment) {
        if (alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) return true;
        return !this.containsMultilineCodeBlockFragments;
    }

    public static class SingleLineCodeBlockPart {
        MarkdownTextFragment start;
        MarkdownTextFragment end;
    }

}
