package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.Renderable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    /** The X offset the layout gave the line. {@link #offsetX} can differ from it, because centered lines get snapped to the pixel grid when they render. */
    protected float layoutOffsetX;
    protected boolean containsImages = false;
    /** The kind of block the line is a part of. Set when the line gets prepared. */
    @NotNull
    protected MarkdownBlockType blockType = MarkdownBlockType.PARAGRAPH;
    /** TRUE for a blank line that takes no space, because the style makes it nothing but the end of the block in front of it. Set by the layout. */
    protected boolean collapsed = false;
    /** The position of the line in the lines of its renderer. Set by the layout. */
    protected int index = 0;
    protected float lineWidth;
    protected float lineHeight;
    /** The space the line keeps free above and below its fragments, like the padding of a panel that starts or ends in the line. Both are part of the line height. */
    protected float paddingTop;
    protected float paddingBottom;

    public MarkdownTextLine(@NotNull MarkdownRenderer parent) {
        this.parent = parent;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.onRender(graphics, mouseX, mouseY, partial, true);
    }

    protected void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partial, boolean shouldRender) {

        // Fragments draw things that reach to fragments after them, like the background of a code span, so all of them get their position before the first one renders
        float textX = this.parent.x + this.offsetX;
        float textY = this.getRenderY();
        for (MarkdownTextFragment f : this.fragments) {
            f.x = textX;
            f.y = textY;
            textX += f.getRenderWidth();
        }
        if (shouldRender) {
            for (int i = 0; i < this.fragments.size(); i++) {
                MarkdownTextFragment f = this.fragments.get(i);
                f.render(graphics, mouseX, mouseY, partial);
                // What decorates the line as a whole comes right behind the text of its first fragment, so the text of all others ends up on top of it
                if (i == 0) this.renderDecorations(graphics, f);
            }
        }

    }

    private void renderDecorations(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment first) {
        if ((first.bulletListLevel > 0) && first.bulletListItemStart) MarkdownListMarkerPainter.render(graphics, this);
        if (first.headlineType != MarkdownTextFragment.HeadlineType.NONE) MarkdownHeadlinePainter.render(graphics, this);
    }

    /**
     * Returns the Y position of the line for where the renderer is right now.<br>
     * The fragments of a line only get this position when the line renders, so this is the position to use for lines that can be outside the visible range.
     */
    protected float getRenderY() {
        return this.parent.scaling.snapY(this.parent.y + this.offsetY);
    }

    public void prepareLine() {
        this.prepareFragments();
        this.updateSize();
    }

    public void prepareFragments() {
        this.containsMultilineCodeBlockFragments = false;
        this.containsImages = false;
        this.singleLineCodeBlockStartEndPairs.clear();
        this.alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
        this.bulletListItemStartLine = false;
        for (MarkdownTextFragment f : this.fragments) {
            f.parentLine = this;
            f.startOfRenderLine = false;
            f.autoLineBreakAfter = false;
            if (f.imageSupplier != null) {
                this.containsImages = true;
            }
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
        this.blockType = MarkdownBlockType.of(this);
    }

    /**
     * Measures the line again.<br>
     * The size of a line gets used for the real size of the text, the position of every following line and the lookup of visible lines,
     * and measuring it walks all of its fragments, so the line keeps its size until this gets called.
     * It has to get called after anything changed that the render size of one of its fragments depends on.
     */
    public void updateSize() {
        float width = 0;
        float height = 0;
        MarkdownTextFragment last = null;
        for (MarkdownTextFragment frag : this.fragments) {
            width += frag.getRenderWidth();
            float fragmentHeight = frag.getRenderHeight();
            if (fragmentHeight > height) {
                height = fragmentHeight;
            }
            last = frag;
        }
        if (last != null) {
            if (last.text.endsWith(" ")) {
                width -= (this.parent.getUnscaledSpaceWidth() * last.getScale());
            }
        }
        if (width < 0) {
            width = 0;
        }
        this.paddingTop = 0;
        this.paddingBottom = 0;
        if ((last != null) && (this.fragments.get(0).headlineType != MarkdownTextFragment.HeadlineType.NONE)) {
            this.paddingBottom += MarkdownHeadlinePainter.getLinePaddingBottom(this);
        }
        this.lineWidth = width;
        this.lineHeight = this.paddingTop + height + this.paddingBottom;
    }

    public float getLineWidth() {
        return this.lineWidth;
    }

    public float getLineHeight() {
        return this.lineHeight;
    }

    @Nullable
    protected MarkdownTextLine getPreviousLine() {
        return (this.index > 0) ? this.parent.lines.get(this.index - 1) : null;
    }

    @Nullable
    protected MarkdownTextLine getNextLine() {
        return ((this.index + 1) < this.parent.lines.size()) ? this.parent.lines.get(this.index + 1) : null;
    }

    /**
     * Returns if the line is inside the visible vertical range of its renderer, which means that it renders.
     */
    protected boolean isVisible() {
        return this.parent.layout.isLineVisible(this);
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
