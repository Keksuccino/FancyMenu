package de.keksuccino.fancymenu.rendering.ui.contextmenu;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.widget.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class AdvancedContextMenu implements Renderable {

    private static final Logger LOGGER = LogManager.getLogger();

    protected ContextMenu contextMenu;
    protected List<MenuEntry> entries = new ArrayList<>();

    public AdvancedContextMenu() {
        this.rebuildMenu();
    }

    public void rebuildMenu() {
        if (this.contextMenu != null) {
            this.contextMenu.closeMenu();
        }
        this.contextMenu = new ContextMenu();
        this.contextMenu.setAlwaysOnTop(true);
        for (MenuEntry e : this.entries) {
            if (e instanceof SeparatorMenuEntry) {
                this.contextMenu.addSeparator();
            }
            if (e instanceof ClickableMenuEntry) {
                this.contextMenu.addContent(((ClickableMenuEntry<?>)e).button);
                if (((ClickableMenuEntry<?>)e).childContextMenu != null) {
                    this.contextMenu.addChild(((ClickableMenuEntry<?>)e).childContextMenu.getContextMenu());
                }
            }
        }
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        UIBase.renderScaledContextMenu(pose, this.contextMenu);
    }

    public <R> void addClickableEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, ClickAction<R> clickAction) {
        int index = this.getIndexOfEntry(addAfterIdentifier);
        if (index == -1) {
            index = this.entries.size();
            LOGGER.error("[FANCYMENU] Failed to add entry '" + identifier + "' to context menu, because tried to add after invalid entry '" + addAfterIdentifier + "'. Entry will get added to the end instead.");
        } else {
            index++;
        }
        this.addClickableEntry(index, identifier, stackable, label, childContextMenu, clickAction);
    }

    public <R> void addClickableEntry(@NotNull String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, ClickAction<R> clickAction) {
        this.addClickableEntry(this.entries.size(), identifier, stackable, label, childContextMenu, clickAction);
    }

    public <R> void addClickableEntry(int index, @NotNull String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, ClickAction<R> clickAction) {
        this.addEntry(index, new ClickableMenuEntry<R>(identifier, stackable, label, childContextMenu, clickAction));
    }

    public void addSeparatorEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, boolean stackable) {
        int index = this.getIndexOfEntry(addAfterIdentifier);
        if (index == -1) {
            index = this.entries.size();
            LOGGER.error("[FANCYMENU] Failed to add entry '" + identifier + "' to context menu, because tried to add after invalid entry '" + addAfterIdentifier + "'. Entry will get added to the end instead.");
        } else {
            index++;
        }
        this.addSeparatorEntry(index, identifier, stackable);
    }

    public void addSeparatorEntry(@NotNull String identifier, boolean stackable) {
        this.addSeparatorEntry(this.entries.size(), identifier, stackable);
    }

    public void addSeparatorEntry(int index, @NotNull String identifier, boolean stackable) {
        this.addEntry(index, new SeparatorMenuEntry(identifier, stackable));
    }

    protected void addEntry(int index, @NotNull MenuEntry entry) {
        index = Math.max(0, Math.min(this.entries.size(), index));
        if (!this.identifierTaken(entry.identifier)) {
            this.entries.add(index, entry);
        } else {
            LOGGER.error("[FANCYMENU] Failed to add entry to context menu! Identifier already in use: " + entry.identifier);
        }
        this.rebuildMenu();
    }

    public void removeEntry(String identifier) {
        int index = this.getIndexOfEntry(identifier);
        this.removeEntry(index);
    }

    public void removeEntry(int index) {
        if ((index >= 0) && (index < this.entries.size())) {
            this.entries.remove(index);
        }
        this.rebuildMenu();
    }

    public void clearEntries() {
        this.entries.clear();
        this.rebuildMenu();
    }

    @Nullable
    public MenuEntry getEntry(String identifier) {
        for (MenuEntry e : this.entries) {
            if (e.identifier.equals(identifier)) return e;
        }
        return null;
    }

    public int getIndexOfEntry(String identifier) {
        MenuEntry e = this.getEntry(identifier);
        if (e != null) {
            return this.entries.indexOf(e);
        }
        return -1;
    }

    public boolean identifierTaken(String identifier) {
        return this.getEntry(identifier) != null;
    }

    @NotNull
    public ContextMenu getContextMenu() {
        return this.contextMenu;
    }

    public void closeMenu() {
        this.contextMenu.closeMenu();
    }

    public void openMenuAtMouse() {
        UIBase.openScaledContextMenuAtMouse(this.contextMenu);
    }

    public void openMenu(int posX, int posY) {
        UIBase.openScaledContextMenuAt(this.contextMenu, posX, posY);
    }

    public boolean isOpen() {
        return this.contextMenu.isOpen();
    }

    public boolean isHovered() {
        return this.contextMenu.isHovered();
    }

    public boolean isUserNavigatingInMenu() {
        return this.contextMenu.isOpen() && this.contextMenu.isHovered();
    }

    @Nullable
    public static AdvancedContextMenu buildStackedContextMenu(@NotNull AdvancedContextMenu... contextMenusToStack) {

        if (contextMenusToStack.length == 0) {
            return null;
        }
        if (contextMenusToStack.length == 1) {
            return contextMenusToStack[0];
        }

        AdvancedContextMenu stacked = new AdvancedContextMenu();

        try {

            AdvancedContextMenu first = contextMenusToStack[0];
            List<AdvancedContextMenu> others = Arrays.asList(contextMenusToStack);
            others.remove(0);
            Map<String, List<MenuEntry>> stackableEntries = new LinkedHashMap<>();
            for (MenuEntry e : first.entries) {
                if (e.stackable) {
                    if (!stackableEntries.containsKey(e.identifier)) {
                        List<MenuEntry> newL = new ArrayList<>();
                        newL.add(e);
                        stackableEntries.put(e.identifier, newL);
                    }
                    List<MenuEntry> l = stackableEntries.get(e.identifier);
                    for (AdvancedContextMenu m : others) {
                        MenuEntry e2 = m.getEntry(e.identifier);
                        if ((e2 != null) && e2.stackable) {
                            l.add(e2);
                        }
                    }
                }
            }
            for (List<MenuEntry> l : stackableEntries.values()) {
                if (!allMenuEntriesCompatible(l)) {
                    continue;
                }
                MenuEntry topEntry = l.get(0).copy();
                //Stack possible child context menus of entry stack
                if ((topEntry instanceof ClickableMenuEntry) && (((ClickableMenuEntry<?>)topEntry).childContextMenu != null)) {
                    List<AdvancedContextMenu> children = new ArrayList<>();
                    children.add(((ClickableMenuEntry<?>)topEntry).childContextMenu);
                    for (MenuEntry m : l) {
                        if ((m instanceof ClickableMenuEntry) && (((ClickableMenuEntry<?>)m).childContextMenu != null)) {
                            children.add(((ClickableMenuEntry<?>)m).childContextMenu);
                        }
                    }
                    ((ClickableMenuEntry<?>)topEntry).childContextMenu = buildStackedContextMenu(children.toArray(new AdvancedContextMenu[]{}));
                }
                if (l.size() > 1) {
                    MenuEntry prev = topEntry;
                    for (MenuEntry e : l.subList(1, l.size())) {
                        MenuEntry copy = e.copy();
                        prev.nextInStack = copy;
                        prev = copy;
                    }
                }
                stacked.addEntry(stacked.entries.size(), topEntry);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (stacked.entries.size() == 0) {
            return null;
        }

        return stacked;

    }

    protected static boolean allMenuEntriesCompatible(List<MenuEntry> entries) {
        if (entries.size() > 1) {
            MenuEntry prev = entries.get(0);
            for (MenuEntry e : entries.subList(1, entries.size())) {
                if (!prev.isCompatibleWith(e)) {
                    return false;
                }
            }
        }
        return true;
    }

    public abstract static class MenuEntry {

        public String identifier;
        public boolean stackable;
        public MenuEntry nextInStack = null;

        protected MenuEntry(@NotNull String identifier, boolean stackable) {
            this.identifier = identifier;
            this.stackable = stackable;
        }

        protected abstract boolean isCompatibleWith(MenuEntry entry);

        protected abstract MenuEntry copy();

    }

    public static class ClickableMenuEntry<R> extends MenuEntry {

        public ClickAction<R> clickAction;
        public AdvancedContextMenu childContextMenu;
        public Component label;
        public Button button;

        protected ClickableMenuEntry(String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, @NotNull ClickAction<R> clickAction) {
            super(identifier, stackable);
            this.clickAction = clickAction;
            this.label = label;
            this.childContextMenu = childContextMenu;
            this.button = new Button(0, 0, 0, 0, label, true, (b) -> this.onClick(null));
        }

        @SuppressWarnings("all")
        protected void onClick(@Nullable R inheritedResult) {
            this.clickAction.onClick(inheritedResult, (passedResult) -> {
                if ((this.nextInStack != null) && this.isCompatibleWith(this.nextInStack)) {
                    ((ClickableMenuEntry<R>)this.nextInStack).onClick(passedResult);
                }
            });
        }

        @SuppressWarnings("all")
        @Override
        protected boolean isCompatibleWith(MenuEntry entry) {
            if (entry == null) {
                return false;
            }
            try {
                ClickableMenuEntry<R> cme = (ClickableMenuEntry<R>) entry;
                return true;
            } catch (Exception ignored) {}
            return false;
        }

        @Override
        protected ClickableMenuEntry<R> copy() {
            return new ClickableMenuEntry<>(this.identifier, this.stackable, this.label, this.childContextMenu, this.clickAction);
        }

    }

    public static class SeparatorMenuEntry extends MenuEntry {

        protected SeparatorMenuEntry(String identifier, boolean stackable) {
            super(identifier, stackable);
        }

        @Override
        protected boolean isCompatibleWith(MenuEntry entry) {
            return (entry instanceof SeparatorMenuEntry);
        }

        @Override
        protected SeparatorMenuEntry copy() {
            return new SeparatorMenuEntry(this.identifier, this.stackable);
        }

    }

    @FunctionalInterface
    public interface ClickAction<R> {

        void onClick(@Nullable R inheritedResult, Consumer<R> passResultToNextInStack);

    }

}
