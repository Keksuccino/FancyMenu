package de.keksuccino.fancymenu.util.rendering.ui.contextmenu;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class AdvancedContextMenu implements Renderable {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Tooltip DEFAULT_TOOLTIP_STYLE = Tooltip.create().setBackgroundColor(Tooltip.DEFAULT_BACKGROUND_COLOR, Tooltip.DEFAULT_BORDER_COLOR);

    protected ContextMenu contextMenu = new ContextMenu();
    protected Tooltip tooltipStyle = DEFAULT_TOOLTIP_STYLE;
    protected List<MenuEntry> entries = new ArrayList<>();
    protected boolean autoAlignment = true;

    public AdvancedContextMenu() {
        this.rebuildMenu();
    }

    public AdvancedContextMenu rebuildMenu() {
        this.contextMenu.closeMenu();
        this.contextMenu.closeChilds();
        this.contextMenu.getContent().clear();
        this.contextMenu.getChildren().clear();
        this.contextMenu.setAlwaysOnTop(true);
        this.contextMenu.setAutoAlignment(this.autoAlignment);
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
        return this;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        if (this.contextMenu.isOpen()) {
            this.tick();
        }
        Screen s = Minecraft.getInstance().screen;
        if (s != null) {
            this.contextMenu.render(pose, mouseX, mouseY, s.width, s.height);
        }
    }

    public void renderScaled(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        if (this.contextMenu.isOpen()) {
            this.tick();
        }
        UIBase.renderScaledContextMenu(pose, this.contextMenu);
    }

    protected void tick() {
        for (MenuEntry e : this.entries) {
            e.tick(this);
        }
    }

    /**
     * This will add a clickable entry with a {@link ClickAction} as last parameter.<br><br>
     *
     * <strong>Click Action:</strong><br>
     * The {@link ClickAction} is stackable and can pass a result to the next in the stack.<br>
     * The <b>first parameter</b> is the {@link ClickableMenuEntry} the {@link ClickAction} is part of.<br>
     * The <b>second parameter</b> of the {@link ClickAction} is the value the previous one passed to it, so if it's the first in the stack, this parameter will be NULL.<br>
     * The <b>third paramter</b> of the {@link ClickAction} is the consumer that is used to pass the result to the next one in the stack. <b>The next in the stack will ony get called if the previus one passes a result to it!</b>
     */
    public <R> ClickableMenuEntry<R> addClickableEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, Class<? extends R> resultType, ClickAction<R> clickAction) {
        int index = this.getIndexOfEntry(addAfterIdentifier);
        if (index == -1) {
            index = this.entries.size();
            LOGGER.error("[FANCYMENU] Failed to add entry '" + identifier + "' to context menu, because tried to add after invalid entry '" + addAfterIdentifier + "'. Entry will get added to the end instead.");
        } else {
            index++;
        }
        return this.addClickableEntry(index, identifier, stackable, label, childContextMenu, resultType, clickAction);
    }

    /**
     * This will add a clickable entry with a {@link ClickAction} as last parameter.<br><br>
     *
     * <strong>Click Action:</strong><br>
     * The {@link ClickAction} is stackable and can pass a result to the next in the stack.<br>
     * The <b>first parameter</b> is the {@link ClickableMenuEntry} the {@link ClickAction} is part of.<br>
     * The <b>second parameter</b> of the {@link ClickAction} is the value the previous one passed to it, so if it's the first in the stack, this parameter will be NULL.<br>
     * The <b>third paramter</b> of the {@link ClickAction} is the consumer that is used to pass the result to the next one in the stack. <b>The next in the stack will ony get called if the previus one passes a result to it!</b>
     */
    public <R> ClickableMenuEntry<R> addClickableEntry(@NotNull String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, Class<? extends R> resultType, ClickAction<R> clickAction) {
        return this.addClickableEntry(this.entries.size(), identifier, stackable, label, childContextMenu, resultType, clickAction);
    }

    /**
     * This will add a clickable entry with a {@link ClickAction} as last parameter.<br><br>
     *
     * <strong>Click Action:</strong><br>
     * The {@link ClickAction} is stackable and can pass a result to the next in the stack.<br>
     * The <b>first parameter</b> is the {@link ClickableMenuEntry} the {@link ClickAction} is part of.<br>
     * The <b>second parameter</b> of the {@link ClickAction} is the value the previous one passed to it, so if it's the first in the stack, this parameter will be NULL.<br>
     * The <b>third paramter</b> of the {@link ClickAction} is the consumer that is used to pass the result to the next one in the stack. <b>The next in the stack will ony get called if the previus one passes a result to it!</b>
     */
    @SuppressWarnings("all")
    public <R> ClickableMenuEntry<R> addClickableEntry(int index, @NotNull String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, Class<? extends R> resultType, ClickAction<R> clickAction) {
        return (ClickableMenuEntry<R>) this.addEntry(index, new ClickableMenuEntry<R>(identifier, stackable, label, childContextMenu, clickAction));
    }

    public SeparatorMenuEntry addSeparatorEntryAfter(@NotNull String addAfterIdentifier, @NotNull String identifier, boolean stackable) {
        int index = this.getIndexOfEntry(addAfterIdentifier);
        if (index == -1) {
            index = this.entries.size();
            LOGGER.error("[FANCYMENU] Failed to add entry '" + identifier + "' to context menu, because tried to add after invalid entry '" + addAfterIdentifier + "'. Entry will get added to the end instead.");
        } else {
            index++;
        }
        return this.addSeparatorEntry(index, identifier, stackable);
    }

    public SeparatorMenuEntry addSeparatorEntry(@NotNull String identifier, boolean stackable) {
        return this.addSeparatorEntry(this.entries.size(), identifier, stackable);
    }

    public SeparatorMenuEntry addSeparatorEntry(int index, @NotNull String identifier, boolean stackable) {
        return (SeparatorMenuEntry) this.addEntry(index, new SeparatorMenuEntry(identifier, stackable));
    }

    protected MenuEntry addEntry(int index, @NotNull MenuEntry entry) {
        index = Math.max(0, Math.min(this.entries.size(), index));
        if (!this.hasEntry(entry.identifier)) {
            entry.parentContextMenu = this;
            this.entries.add(index, entry);
        } else {
            LOGGER.error("[FANCYMENU] Failed to add entry to context menu! Identifier already in use: " + entry.identifier);
        }
        this.rebuildMenu();
        return entry;
    }

    public AdvancedContextMenu removeEntry(String identifier) {
        int index = this.getIndexOfEntry(identifier);
        return this.removeEntry(index);
    }

    public AdvancedContextMenu removeEntry(int index) {
        if ((index >= 0) && (index < this.entries.size())) {
            this.entries.remove(index);
        }
        this.rebuildMenu();
        return this;
    }

    public AdvancedContextMenu clearEntries() {
        this.entries.clear();
        this.rebuildMenu();
        return this;
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

    public boolean hasEntry(String identifier) {
        return this.getEntry(identifier) != null;
    }

    @NotNull
    public ContextMenu getContextMenu() {
        return this.contextMenu;
    }

    public boolean isAutoAlignment() {
        return this.autoAlignment;
    }

    public void setAutoAlignment(boolean autoAlignment) {
        this.autoAlignment = autoAlignment;
        this.rebuildMenu();
    }

    public AdvancedContextMenu closeMenu() {
        this.contextMenu.closeMenu();
        return this;
    }

    protected AdvancedContextMenu openMenuAtMouseInternal(boolean scaleByUI) {
        if (scaleByUI) {
            UIBase.openScaledContextMenuAtMouse(this.contextMenu);
        } else {
            this.contextMenu.openMenuAt(MouseInput.getMouseX(), MouseInput.getMouseY());
        }
        return this;
    }

    public AdvancedContextMenu openMenuAtMouse() {
        return this.openMenuAtMouseInternal(false);
    }

    public AdvancedContextMenu openMenuAtMouseScaled() {
        return this.openMenuAtMouseInternal(true);
    }

    protected AdvancedContextMenu openMenuInternal(int posX, int posY, boolean scaleByUI) {
        if (scaleByUI) {
            UIBase.openScaledContextMenuAt(this.contextMenu, posX, posY);
        } else {
            this.contextMenu.openMenuAt(posX, posY);
        }
        return this;
    }

    public AdvancedContextMenu openMenuScaled(int posX, int posY) {
        return this.openMenuInternal(posX, posY, true);
    }

    public AdvancedContextMenu openMenu(int posX, int posY) {
        return this.openMenuInternal(posX, posY, false);
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
    public Tooltip getTooltipStyle() {
        return tooltipStyle;
    }

    public AdvancedContextMenu setTooltipStyle(@Nullable Tooltip tooltipStyle) {
        this.tooltipStyle = tooltipStyle;
        return this;
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
                RuntimePropertyContainer meta = new RuntimePropertyContainer();
                MenuEntry topEntry = l.get(0).copy();
                topEntry.entryStackMeta = meta;
                topEntry.partOfStack = true;
                topEntry.firstInStack = true;
                topEntry.lastInStack = false;
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
                        copy.partOfStack = true;
                        copy.entryStackMeta = meta;
                        copy.lastInStack = false;
                        copy.firstInStack = false;
                        if (copy instanceof ClickableMenuEntry) {
                            ((ClickableMenuEntry<?>)copy).tooltip = null;
                        }
                        prev.nextInStack = copy;
                        prev = copy;
                    }
                    prev.lastInStack = true;
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

        protected String identifier;
        protected AdvancedContextMenu parentContextMenu = null;
        protected boolean stackable;
        protected RuntimePropertyContainer entryStackMeta = new RuntimePropertyContainer();
        protected boolean partOfStack = false;
        protected boolean firstInStack = true;
        protected boolean lastInStack = true;
        protected MenuEntry nextInStack = null;
        protected Consumer<MenuEntry> ticker = null;

        protected MenuEntry(@NotNull String identifier, boolean stackable) {
            this.identifier = identifier;
            this.stackable = stackable;
        }

        public MenuEntry setTicker(@Nullable Consumer<MenuEntry> ticker) {
            this.ticker = ticker;
            return this;
        }

        protected void tick(AdvancedContextMenu menu) {
            if (this.ticker != null) {
                this.ticker.accept(this);
            }
            if (this.nextInStack != null) {
                this.nextInStack.tick(menu);
            }
        }

        public String getIdentifier() {
            return this.identifier;
        }

        public boolean isStackable() {
            return this.stackable;
        }

        public boolean isPartOfStack() {
            return this.partOfStack;
        }

        public boolean isLastInStack() {
            return this.lastInStack;
        }

        public boolean isFirstInStack() {
            return this.firstInStack;
        }

        @NotNull
        public RuntimePropertyContainer getEntryStackMeta() {
            return this.entryStackMeta;
        }

        protected abstract boolean isCompatibleWith(MenuEntry entry);

        protected abstract MenuEntry copy();

    }

    public static class ClickableMenuEntry<R> extends MenuEntry {

        protected ClickAction<R> clickAction;
        protected AdvancedContextMenu childContextMenu;
        protected Component label;
        protected AdvancedButton button;
        protected Tooltip tooltip = null;

        protected ClickableMenuEntry(String identifier, boolean stackable, @NotNull Component label, @Nullable AdvancedContextMenu childContextMenu, @NotNull ClickAction<R> clickAction) {
            super(identifier, stackable);
            this.clickAction = clickAction;
            this.label = label;
            this.childContextMenu = childContextMenu;
            this.button = new AdvancedButton(0, 0, 0, 0, label.getString(), true, (b) -> this.onClick(null)) {
                @Override
                public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
                    tick(parentContextMenu);
                    super.render(pose, mouseX, mouseY, partial);
                }
            };
        }

        @SuppressWarnings("all")
        @Override
        public ClickableMenuEntry<R> setTicker(@Nullable Consumer<MenuEntry> ticker) {
            return (ClickableMenuEntry<R>) super.setTicker(ticker);
        }

        @Override
        protected void tick(AdvancedContextMenu menu) {
            if (this.childContextMenu != null) {
                this.childContextMenu.contextMenu.setParentButton(this.button);
            }
            if (this.tooltip != null) {
                if (menu.tooltipStyle != null) {
                    this.tooltip.copyStyleOf(menu.tooltipStyle);
                }
                TooltipHandler.INSTANCE.addWidgetTooltip(this.button, this.tooltip, false, true);
            }
            super.tick(menu);
        }

        public ClickableMenuEntry<R> setTooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        @Nullable
        public Tooltip getTooltip() {
            return this.tooltip;
        }

        public ClickAction<R> getClickAction() {
            return this.clickAction;
        }

        public AdvancedContextMenu getChildContextMenu() {
            return this.childContextMenu;
        }

        public Component getLabel() {
            return this.label;
        }

        public void setLabel(Component label) {
            this.label = label;
            this.button.setMessage(label);
        }

        public AdvancedButton getButton() {
            return this.button;
        }

        @SuppressWarnings("all")
        protected void onClick(@Nullable R inheritedResult) {
            this.clickAction.onClick(this, inheritedResult, (passedResult) -> {
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
            ClickableMenuEntry<R> e = new ClickableMenuEntry<>(this.identifier, this.stackable, this.label, this.childContextMenu, this.clickAction);
            e.tooltip = this.tooltip;
            e.ticker = this.ticker;
            return e;
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
            SeparatorMenuEntry e = new SeparatorMenuEntry(this.identifier, this.stackable);
            e.ticker = this.ticker;
            return e;
        }

    }

    @FunctionalInterface
    public interface ClickAction<R> {

        /**
         * @param entry The parent {@link ClickableMenuEntry} this {@link ClickAction} is part of.
         * @param inheritedResult If the parent {@link ClickableMenuEntry} is stackable, this is the result the previous entry in the stack passed to this one. If the parent entry is the first in the stack, this will be NULL!
         * @param passResultToNextInStack If the parent {@link ClickableMenuEntry} is stackable, this is used to pass the result of this entry to the next in the stack. The next entry in the stack will only get called if the previous one passed a result.
         */
        void onClick(@NotNull ClickableMenuEntry<R> entry, @Nullable R inheritedResult, Consumer<R> passResultToNextInStack);

    }

}
