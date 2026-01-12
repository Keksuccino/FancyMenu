package de.keksuccino.fancymenu.customization.background;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementMemories;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorHistory;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import de.keksuccino.fancymenu.customization.layout.Layout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class MenuBackground<B extends MenuBackground<?>> implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget, ContextMenuBuilder<B>, PropertyHolder {

    public final MenuBackgroundBuilder<?> builder;
    private final Map<String, Property<?>> propertyMap = new LinkedHashMap<>();
    private final List<ContextMenuBuilder.ContextMenuScreenOpenProcessor> contextMenuScreenOpenProcessorList = new ArrayList<>();
    @NotNull
    protected String instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
    @Nullable
    protected RuntimePropertyContainer cachedMemory;

    /** This gets set by the {@link ScreenCustomizationLayer} when screens fade in or out and should only get used as getter. **/
    public float opacity = 1.0F;
    /** This gets set by the {@link ScreenCustomizationLayer} and should only be used as getter. **/
    public boolean keepBackgroundAspectRatio = false;

    public final Property<Boolean> showBackground = putProperty(Property.booleanProperty("show_background", false, "fancymenu.backgrounds.general.show_background"));

    public MenuBackground(MenuBackgroundBuilder<?> builder) {
        this.builder = builder;
    }

    @Override
    public @NotNull Map<String, Property<?>> getPropertyMap() {
        return this.propertyMap;
    }

    @Override
    public @NotNull B self() {
        return (B)this;
    }

    @Override
    public @NotNull List<ContextMenuBuilder.ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList() {
        return this.contextMenuScreenOpenProcessorList;
    }

    @Override
    public @Nullable Screen getContextMenuCallbackScreen() {
        return LayoutEditorScreen.getCurrentInstance();
    }

    @Override
    public @NotNull List<B> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<B, Boolean> filter) {
        return List.of((B)this);
    }

    @Override
    public void saveSnapshot() {
        var e = LayoutEditorScreen.getCurrentInstance();
        if (e != null) e.history.saveSnapshot();
    }

    @Override
    public void saveSnapshot(@NotNull Object snapshot) {
        Objects.requireNonNull(snapshot);
        var e = LayoutEditorScreen.getCurrentInstance();
        if (e != null) e.history.saveSnapshot((LayoutEditorHistory.Snapshot) snapshot);
    }

    @Override
    public @Nullable Object createSnapshot() {
        var e = LayoutEditorScreen.getCurrentInstance();
        if (e != null) return e.history.createSnapshot();
        return null;
    }

    protected abstract void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor);

    @NotNull
    public ContextMenu _initConfigMenu(@NotNull LayoutEditorScreen editor) {

        var menu = new ContextMenu();

        this.showBackground.buildContextMenuEntryAndAddTo(menu, this);

        menu.addSeparatorEntry("separator_after_show_background_toggle");

        menu.addClickableEntry("copy_background_identifier", Component.translatable("fancymenu.editor.background_options.copy_background_identifier"), (menu1, entry) -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.getInstanceIdentifier());
                }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.background_options.copy_background_identifier.desc")))
                .addIsActiveSupplier((menu1, entry) -> !editor.layout.menuBackgrounds.isEmpty());

        menu.addSeparatorEntry("separator_after_copy_identifier");

        this.initConfigMenu(menu, editor);

        return menu;

    }

    @Override
    public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    @ApiStatus.Internal
    public void _render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.showBackground.tryGetNonNull()) return;

        this.render(graphics, mouseX, mouseY, partial);

    }

    /** Gets called every {@link Screen} tick, after {@link Screen#tick()} got called. **/
    public void tick() {
    }

    /**
     * Gets called before a {@link Screen} gets closed.<br>
     * A screen gets closed when a new active {@link Screen} gets set via {@link Minecraft#setScreen(Screen)}.<br><br>
     */
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
    }

    /**
     * Gets called before a {@link Screen} gets closed.<br>
     * A screen gets closed when a new {@link Screen} gets set via {@link Minecraft#setScreen(Screen)}.
     */
    @Deprecated
    public void onCloseScreen() {
    }

    /**
     * Gets called after a {@link Screen} got opened via {@link Minecraft#setScreen(Screen)}.<br>
     * The {@link Screen} is already initialized at the time this method gets called.
     */
    public void onOpenScreen() {
    }

    /**
     * Gets called before the current {@link Screen} gets resized.<br>
     * Does NOT get called on initial resize (when opening the screen). Use {@link MenuBackground#onOpenScreen()} for that instead.
     */
    public void onBeforeResizeScreen() {
    }

    /**
     * Gets called after the current {@link Screen} got resized.<br>
     * Does NOT get called on initial resize (when opening the screen). Use {@link MenuBackground#onOpenScreen()} for that instead.
     */
    public void onAfterResizeScreen() {
    }

    /**
     * Gets called right after the background got enabled in a layout.<br>
     * This gets called whenever the background gets enabled, which means it will also trigger on every (re-)init and resize of the target {@link Screen}.
     */
    public void onAfterEnable() {
    }

    /**
     * Gets called before the background gets disabled or removed. This is the case when, for example, customizations get disabled
     * for the target {@link Screen}, when the parent {@link Layout} gets disabled or when the background gets removed/replaced in the editor.
     */
    public void onDisableOrRemove() {
    }

    /**
     * The memory of a {@link MenuBackground} remembers variables across instance rebuilding.<br>
     * It can be used if a background needs to access data of its ancestors.<br><br>
     *
     * Every background (based on its instance identifier) has its own memory.
     */
    @NotNull
    public RuntimePropertyContainer getMemory() {
        if (this.cachedMemory == null) {
            this.cachedMemory = ElementMemories.getMemory(this.getInstanceIdentifier());
        }
        return this.cachedMemory;
    }

    @NotNull
    public String getInstanceIdentifier() {
        return this.instanceIdentifier;
    }

    @Nullable
    public Layout getParentLayout() {
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
        if (layer != null) {
            for (Layout layout : layer.activeLayouts) {
                if (layout.menuBackgrounds.contains(this)) return layout;
            }
        }
        return null;
    }

    @Nullable
    public MenuBackground copy() {
        try {
            return this.builder._deserializeBackground(this.builder._serializeBackground(this));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean isEditor() {
        return (Minecraft.getInstance().screen instanceof LayoutEditorScreen);
    }

    public static int getScreenWidth() {
        return AbstractElement.getScreenWidth();
    }

    public static int getScreenHeight() {
        return AbstractElement.getScreenHeight();
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("MenuBackgrounds are not focusable!");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("MenuBackgrounds are not navigatable!");
    }

}
