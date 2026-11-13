package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link ContextMenu} that adds its entries right before they are needed for the first time, instead of holding them from the start.<br>
 * Big menus that exist once per object, like the ones of editor elements, should be one of these,
 * so only the menus somebody actually looks at pay for building and holding their entries.<br><br>
 *
 * The entries get added when the menu opens, when it gets stacked with other menus, and when something asks for its entries or changes them.
 * Every way to reach the entries of a menu from outside has to go through one of the methods this class overrides.
 */
public class LazyContextMenu extends ContextMenu {

    @Nullable
    protected Consumer<ContextMenu> contentBuilder;

    /**
     * @param contentBuilder Adds the entries to the menu it gets. It runs once at most.
     */
    public LazyContextMenu(@NotNull Consumer<ContextMenu> contentBuilder) {
        this.contentBuilder = contentBuilder;
    }

    /**
     * If the menu still waits for something to need its entries.
     */
    public boolean isWaitingForContent() {
        return this.contentBuilder != null;
    }

    protected void buildContent() {
        Consumer<ContextMenu> builder = this.contentBuilder;
        if (builder == null) return;
        // The builder is gone before it runs, because it adds its entries through the methods that call this
        this.contentBuilder = null;
        builder.accept(this);
    }

    @Override
    public ContextMenu openMenuAt(float x, float y, @Nullable List<String> entryPath) {
        this.buildContent();
        return super.openMenuAt(x, y, entryPath);
    }

    @NotNull
    @Override
    public <T extends ContextMenuEntry<?>> T addEntry(@NotNull T entry) {
        this.buildContent();
        return super.addEntry(entry);
    }

    @NotNull
    @Override
    public <T extends ContextMenuEntry<?>> T addEntryAt(int index, @NotNull T entry) {
        this.buildContent();
        return super.addEntryAt(index, entry);
    }

    @Override
    public ContextMenu clearEntries() {
        // Entries nobody needed until now would get removed right away
        this.contentBuilder = null;
        return super.clearEntries();
    }

    @Override
    public ContextMenu clearEntriesKeepOpen() {
        this.contentBuilder = null;
        return super.clearEntriesKeepOpen();
    }

    @Nullable
    @Override
    public ContextMenuEntry<?> getEntry(String identifier) {
        this.buildContent();
        return super.getEntry(identifier);
    }

    @NotNull
    @Override
    public List<ContextMenuEntry<?>> getEntries() {
        this.buildContent();
        return super.getEntries();
    }

    @NotNull
    @Override
    protected List<ContextMenuEntry<?>> getStackableEntries() {
        this.buildContent();
        return super.getStackableEntries();
    }

}
