package de.keksuccino.fancymenu.util.rendering.text.markdown;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Knows which parts of a {@link MarkdownRenderer}'s text react to the mouse: the fragments that input events have to reach,
 * and the text hover events that fire when the mouse enters them.<br><br>
 *
 * Both get looked up once per parse by {@link #collect(List)}, because walking all fragments for every input event and every frame is expensive.
 */
final class MarkdownInteractions {

    private final MarkdownRenderer renderer;
    /** Link-like fragments and tables with link-like fragments in their cells, in the order of the text. */
    final List<MarkdownTextFragment> interactiveFragments = new ArrayList<>();
    /** Every text hover event of the text, once. */
    private final List<MarkdownTextFragment.TextHoverEvent> hoverEvents = new ArrayList<>();
    /** The identifiers of the text hover events the mouse was over when {@link #updateHoverEvents()} ran last. */
    private final Set<String> hoveredEventIds = new HashSet<>();

    MarkdownInteractions(@NotNull MarkdownRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Looks up the interactive fragments and text hover events of the given fragments. Has to run after every parse.
     */
    void collect(@NotNull List<MarkdownTextFragment> fragments) {
        this.interactiveFragments.clear();
        this.hoverEvents.clear();
        Set<MarkdownTextFragment.TextHoverEvent> foundHoverEvents = new HashSet<>();
        for (MarkdownTextFragment fragment : fragments) {
            if (this.collectLinkLike(fragment, foundHoverEvents)) {
                this.interactiveFragments.add(fragment);
            }
        }
    }

    /**
     * Returns if the fragment is link-like or a table with link-like fragments, and adds the text hover events it finds on the way.
     */
    private boolean collectLinkLike(@NotNull MarkdownTextFragment fragment, @NotNull Set<MarkdownTextFragment.TextHoverEvent> foundHoverEvents) {
        if ((fragment.hoverEvent != null) && foundHoverEvents.add(fragment.hoverEvent)) {
            this.hoverEvents.add(fragment.hoverEvent);
        }
        boolean linkLike = fragment.isLinkLike();
        if (fragment.isTable()) {
            for (MarkdownTextFragment.TableRow row : fragment.tableContext.rows) {
                for (MarkdownTextFragment.TableCell cell : row.cells) {
                    for (MarkdownTextFragment cellFragment : cell.fragments) {
                        if (this.collectLinkLike(cellFragment, foundHoverEvents)) linkLike = true;
                    }
                }
            }
        }
        return linkLike;
    }

    /**
     * Fires the text hover event of every event the mouse entered since the last call.<br>
     * Events with the same identifier count as one, so moving the mouse between them doesn't fire again.
     */
    void updateHoverEvents() {
        if ((this.renderer.textEventHandler == null) || !this.isAnyHoverEventHovered()) {
            if (!this.hoveredEventIds.isEmpty()) this.hoveredEventIds.clear();
            return;
        }
        Iterator<String> hoveredIds = this.hoveredEventIds.iterator();
        while (hoveredIds.hasNext()) {
            if (!this.isHoverEventHovered(hoveredIds.next())) hoveredIds.remove();
        }
        // Firing an event runs foreign code, which is allowed to make the renderer parse a new text, so the list can change while walking it
        for (int i = 0; i < this.hoverEvents.size(); i++) {
            MarkdownTextFragment.TextHoverEvent event = this.hoverEvents.get(i);
            if (event.isHovered() && this.hoveredEventIds.add(event.identifier)) {
                this.renderer.fireTextHoverEvent(event.identifier);
            }
        }
    }

    private boolean isAnyHoverEventHovered() {
        for (int i = 0; i < this.hoverEvents.size(); i++) {
            if (this.hoverEvents.get(i).isHovered()) return true;
        }
        return false;
    }

    private boolean isHoverEventHovered(@NotNull String identifier) {
        for (int i = 0; i < this.hoverEvents.size(); i++) {
            MarkdownTextFragment.TextHoverEvent event = this.hoverEvents.get(i);
            if (event.isHovered() && event.identifier.equals(identifier)) return true;
        }
        return false;
    }

    /**
     * Makes all given fragments forget that the mouse was over them, including the fragments in table cells.
     */
    void resetHovered(@NotNull List<MarkdownTextFragment> fragments) {
        this.resetHoveredFragments(fragments);
        this.hoveredEventIds.clear();
    }

    private void resetHoveredFragments(@NotNull List<MarkdownTextFragment> fragments) {
        for (MarkdownTextFragment fragment : fragments) {
            fragment.setHovered(false);
            if (fragment.isTable()) {
                for (MarkdownTextFragment.TableRow row : fragment.tableContext.rows) {
                    for (MarkdownTextFragment.TableCell cell : row.cells) {
                        this.resetHoveredFragments(cell.fragments);
                    }
                }
            }
        }
    }

}
