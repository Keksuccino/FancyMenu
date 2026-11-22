package de.keksuccino.fancymenu.customization.variables;

/**
 * What happened when a list variable got edited.
 */
public enum ListEditResult {

    /** The entries changed, or the variable got created or turned into a list. */
    CHANGED,
    /** The edit had nothing to do, like removing an entry the list doesn't have. */
    UNCHANGED,
    /** The list has no entry at the given index, so nothing changed and no variable got created. */
    INDEX_OUT_OF_RANGE,
    /** The variable is a text variable with a value, so it stayed as it was. List edits never overwrite text. */
    NOT_A_LIST,
    /** The variables are shut down, so nothing changed. */
    UNAVAILABLE;

    public boolean isChanged() {
        return this == CHANGED;
    }

}
