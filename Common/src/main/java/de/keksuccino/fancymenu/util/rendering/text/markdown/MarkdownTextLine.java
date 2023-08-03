package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class MarkdownTextLine implements Renderable {

    public MarkdownRenderer parent;
    public float offsetX;
    public float offsetY;
    public boolean containsMultilineCodeBlockFragments = false;
    @NotNull
    public MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
    public boolean bulletListItemStartLine = false;
    public final List<MarkdownTextFragment> fragments = new ArrayList<>();

    public MarkdownTextLine(@NotNull MarkdownRenderer parent) {
        this.parent = parent;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.onRender(pose, mouseX, mouseY, partial, true);
    }

    protected void onRender(PoseStack pose, int mouseX, int mouseY, float partial, boolean shouldRender) {

        float textX = this.parent.x + this.offsetX;
        for (MarkdownTextFragment f : this.fragments) {
            f.x = textX;
            f.y = this.parent.y + this.offsetY;
            if (shouldRender) {
                f.render(pose, mouseX, mouseY, partial);
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
        for (MarkdownTextFragment f : this.fragments) {
            f.parentLine = this;
            f.startOfRenderLine = false;
            f.autoLineBreakAfter = false;
            if ((f.codeBlockContext != null) && !f.codeBlockContext.singleLine) {
                this.containsMultilineCodeBlockFragments = true;
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
                f -= (this.parent.font.width(" ") * last.getScale());
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

}
