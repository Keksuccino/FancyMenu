package de.keksuccino.fancymenu.rendering.ui.contextmenu;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.widget.Button;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AdvancedContextMenu implements Renderable {

    private static final Logger LOGGER = LogManager.getLogger();

    protected ContextMenu contextMenu;
    protected List<MenuEntry> entries = new ArrayList<>();
    protected List<ContextMenu> childContextMenus = new ArrayList<>();

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
                this.contextMenu.addContent(((ClickableMenuEntry)e).button);
            }
        }
        for (ContextMenu m : this.childContextMenus) {
            this.contextMenu.addChild(m);
        }
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        UIBase.renderScaledContextMenu(pose, this.contextMenu);
    }

    public void addButton(String identifier, boolean stackable, AdvancedButton button) {
        this.addButton(this.entries.size(), identifier, stackable, button);
    }

    public void addButtonAfter(String addAfterIdentifier, String identifier, boolean stackable, AdvancedButton button) {
        int index = this.getIndexOfEntry(addAfterIdentifier);
        if (index == -1) {
            index = this.entries.size();
            LOGGER.error("[FANCYMENU] Failed to add entry '" + identifier + "' to right-click context menu of element, because tried to add after invalid entry '" + addAfterIdentifier + "'. Entry will get added to the end instead.");
        } else {
            index++;
        }
        this.addButton(index, identifier, stackable, button);
    }

    public void addButton(int index, String identifier, boolean stackable, AdvancedButton button) {
        this.addEntry(index, new ClickableMenuEntry(identifier, stackable, button));
    }

    public void addSeparator(String identifier, boolean stackable) {
        this.addSeparator(this.entries.size(), identifier, stackable);
    }

    public void addSeparatorAfter(String addAfterIdentifier, String identifier, boolean stackable) {
        int index = this.getIndexOfEntry(addAfterIdentifier);
        if (index == -1) {
            index = this.entries.size();
            LOGGER.error("[FANCYMENU] Failed to add entry '" + identifier + "' to right-click context menu of element, because tried to add after invalid entry '" + addAfterIdentifier + "'. Entry will get added to the end instead.");
        } else {
            index++;
        }
        this.addSeparator(index, identifier, stackable);
    }

    public void addSeparator(int index, String identifier, boolean stackable) {
        this.addEntry(index, new SeparatorMenuEntry(identifier, stackable));
    }

    protected void addEntry(int index, @NotNull MenuEntry entry) {
        index = Math.max(0, Math.min(this.entries.size(), index));
        if (!this.identifierTaken(entry.identifier)) {
            this.entries.add(index, entry);
        } else {
            LOGGER.error("[FANCYMENU] Failed to add entry to right-click context menu of element! Identifier already in use: " + entry.identifier);
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

    public void addChildContextMenu(ContextMenu menu) {
        this.childContextMenus.add(menu);
        this.rebuildMenu();
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

    public static class MenuEntry {

        public final String identifier;
        public final boolean stackable;
        public MenuEntry nextInStack = null;

        protected MenuEntry(@NotNull String identifier, boolean stackable) {
            this.identifier = identifier;
            this.stackable = stackable;
        }

    }

    public static class ClickableMenuEntry extends MenuEntry {

        public final ClickAction<?> clickAction;
        public final Button button;

        protected <R> ClickableMenuEntry(String identifier, boolean stackable, @NotNull Component label, @NotNull ClickAction<R> clickAction) {
            super(identifier, stackable);
            this.clickAction = clickAction;
            this.button = new Button(0, 0, 0, 0, label, true, (b) -> {
                clickAction.onClick();
            });
        }

        protected void onClick(Object inheritedResult, Consumer<R> passResultToNextInStack) {
            this.clickAction.onClick(inheritedResult, (r) -> {

            });
        }

    }

    public static class SeparatorMenuEntry extends MenuEntry {

        protected SeparatorMenuEntry(String identifier, boolean stackable) {
            super(identifier, stackable);
        }

    }

    @FunctionalInterface
    public interface ClickAction<R> {

        void onClick(@Nullable R inheritedResult, Consumer<R> passResultToNextInStack);

    }

}
