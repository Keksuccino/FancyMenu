package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

/**
 * When the items of a list that start with "-" get the marker for removed things instead of the marker of their list level.<br>
 * Only the items of the first list level can get it. Items of lists inside of list items always have the marker of their level.
 */
public enum MarkdownRemovedMarkerMode {

    /**
     * When the list of the item has an item that starts with "+" as well, which makes it read like patch notes:
     * "+" for what got added and "-" for what got removed. A plain list of "-" items stays a normal list,
     * also when another list of the same text has "+" items.
     */
    AUTOMATIC,
    NEVER,
    ALWAYS

}
