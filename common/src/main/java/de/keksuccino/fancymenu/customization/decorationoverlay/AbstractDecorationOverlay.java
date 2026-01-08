package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorHistory;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractDecorationOverlay<O extends AbstractDecorationOverlay<?>> implements Renderable, ContainerEventHandler, NarratableEntry, PropertyHolder, ContextMenuBuilder<O>, FancyMenuUiComponent {

    private final Map<String, Property<?>> propertyMap = new LinkedHashMap<>();
    private final List<ContextMenuBuilder.ContextMenuScreenOpenProcessor> contextMenuScreenOpenProcessorList = new ArrayList<>();

    @NotNull
    private String instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
    @ApiStatus.Internal
    public final Property<Boolean> showOverlay = putProperty(Property.booleanProperty("show_decoration_overlay", false, "fancymenu.decoration_overlay.show_overlay"));
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<Renderable> renderables = new ArrayList<>();

    @NotNull
    public String getInstanceIdentifier() {
        return this.instanceIdentifier;
    }

    public void setInstanceIdentifier(@NotNull String instanceIdentifier) {
        this.instanceIdentifier = instanceIdentifier;
    }

    @Override
    public @NotNull Map<String, Property<?>> getPropertyMap() {
        return this.propertyMap;
    }

    @Override
    public @NotNull List<ContextMenuBuilder.ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList() {
        return this.contextMenuScreenOpenProcessorList;
    }

    protected abstract void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor);

    @ApiStatus.Internal
    @NotNull
    public ContextMenu _initConfigMenu(@NotNull LayoutEditorScreen editor) {

        ContextMenu menu = new ContextMenu();

        this.showOverlay.buildContextMenuEntryAndAddTo(menu, this);

        menu.addSeparatorEntry("separator_after_show_overlay_toggle");

        this.initConfigMenu(menu, editor);

        return menu;

    }

    @Override
    public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    public final void _render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.render(graphics, mouseX, mouseY, partial);

        this.renderables.forEach(renderable -> renderable.render(graphics, mouseX, mouseY, partial));

    }

    /**
     * Gets called after the current {@link Screen} got initialized or resized.<br>
     * At this point all its widgets should be available.
     */
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
    }

    /**
     * Gets called when the current {@link Screen} is about to close.
     */
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    protected void addChild(@NotNull GuiEventListener child) {
        this.children.add(Objects.requireNonNull(child));
    }

    protected <C extends Renderable & GuiEventListener> void addRenderableChild(@NotNull C child) {
        this.children.add(Objects.requireNonNull(child));
        this.renderables.add(child);
    }

    protected void removeChild(@NotNull GuiEventListener child) {
        this.children.remove(child);
        if (child instanceof Renderable r) this.renderables.remove(r);
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public void setDragging(boolean isDragging) {
        // do nothing
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return null;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        // do nothing
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        // do nothing
    }

    @Override
    public @NotNull O self() {
        return (O) this;
    }

    @Override
    public @Nullable Screen getContextMenuCallbackScreen() {
        return LayoutEditorScreen.getCurrentInstance();
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

    @Override
    public @NotNull List<O> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<O, Boolean> filter) {
        return List.of((O)this);
    }

    @Nullable
    protected static Screen getScreen() {
        return Minecraft.getInstance().screen;
    }

    protected static int getScreenWidth() {
        var s = getScreen();
        return (s != null) ? s.width : 1;
    }

    protected static int getScreenHeight() {
        var s = getScreen();
        return (s != null) ? s.height : 1;
    }

    protected static boolean isEditor() {
        return (getScreen() instanceof LayoutEditorScreen);
    }

    @Nullable
    protected static CollisionBox getAsCollisionBox(@NotNull Object o) {
        if (o instanceof AbstractElement e) {
            if (!e.shouldRender()) return null;
            if ((e instanceof HideableElement h) && h.isHidden()) return null;
            if (!e.shouldBeAffectedByDecorationOverlays.tryGetNonNull()) return null;
            return new CollisionBox(e.getAbsoluteX(), e.getAbsoluteY(), e.getAbsoluteWidth(), e.getAbsoluteHeight());
        }
        if (o instanceof PlainTextButton) return null;
        if (o instanceof AbstractButton b) return new CollisionBox(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        if (o instanceof EditBox b) return new CollisionBox(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        if (o instanceof AbstractSliderButton s) return new CollisionBox(s.getX(), s.getY(), s.getWidth(), s.getHeight());
        return null;
    }

    protected static void visitCollisionBoxes(@NotNull Screen screen, @NotNull List<AbstractElement> elements, @NotNull Consumer<CollisionBox> consumer) {
        if (isEditor()) return;
        elements.forEach(element -> {
            var c = getAsCollisionBox(element);
            if (c != null) {
                consumer.accept(c);
            }
        });
    }

    public record CollisionBox(int x, int y, int width, int height) {
    }

}
