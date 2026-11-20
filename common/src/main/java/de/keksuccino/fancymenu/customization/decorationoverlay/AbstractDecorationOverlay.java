package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.aiexport.AiExportMenus;
import de.keksuccino.fancymenu.customization.aiexport.exporters.LayoutAiExporter;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorHistory;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * Base of all decoration overlays.<br><br>
 *
 * <b>Render passes and parts</b><br>
 * Overlays render in two passes per frame (see {@link DecorationOverlayPass}): the background pass draws behind all elements,
 * the foreground pass in front of them. An overlay draws its content as parts ({@link DecorationOverlayPart}):
 * {@link #renderBackPart}, {@link #renderElementPart} and {@link #renderFrontPart}. The {@link #layers Layers} setting of every
 * overlay decides which pass draws which part ({@link DecorationOverlayLayers}). Each part gets drawn once per frame, by exactly one pass.<br>
 * Overlays that only override {@link #render} draw front content: {@link #renderFrontPart} calls it by default.<br><br>
 *
 * <b>Simulation</b><br>
 * {@link #updateFrame} runs exactly once per frame, at the start of the first pass that draws the overlay in that frame,
 * however many passes run. Overlays that move things should advance them there and only draw in the part methods.<br><br>
 *
 * <b>Input</b><br>
 * Overlays are the first children of their screen, so they see clicks before any widget. {@link #onPointerClicked} lets an overlay
 * react to clicks without ever taking them away from widgets. {@link #isMouseOver} must stay false: the vanilla click logic gives
 * a click only to the first child the mouse is over and then stops, so an overlay that claims the mouse would swallow every click.
 */
public abstract class AbstractDecorationOverlay<O extends AbstractDecorationOverlay<?>> implements Renderable, ContainerEventHandler, NarratableEntry, PropertyHolder, ContextMenuBuilder<O>, FancyMenuUiComponent {

    private final Map<String, Property<?>> propertyMap = new LinkedHashMap<>();
    private final List<ContextMenuBuilder.ContextMenuScreenOpenProcessor> contextMenuScreenOpenProcessorList = new ArrayList<>();

    @NotNull
    private String instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
    @ApiStatus.Internal
    public final Property<Boolean> showOverlay = putProperty(Property.booleanProperty("show_decoration_overlay", false, "fancymenu.decoration_overlay.show_overlay"));
    /** Which render pass draws which part of the overlay, see {@link DecorationOverlayLayers}. */
    public final Property<DecorationOverlayLayers> layers = putProperty(Property.enumProperty("decoration_overlay_layers", DecorationOverlayLayers.DEFAULT, "fancymenu.decoration_overlay.layers", DecorationOverlayLayers::getLocalizationKey));
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<Renderable> renderables = new ArrayList<>();
    private final DecorationOverlayFrameTracker frameTracker = new DecorationOverlayFrameTracker();

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

        this.showOverlay.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.VISIBILITY);

        this.layers.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.LAYERS)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlay.layers.desc")));

        AiExportMenus.addExportSubMenu(menu, "export_for_ai", Component.translatable("fancymenu.ai_export"), () -> LayoutAiExporter.exportDecorationOverlay(editor, this));

        menu.addSeparatorEntry("separator_after_show_overlay_toggle");

        this.initConfigMenu(menu, editor);

        return menu;

    }

    /**
     * Draws the front content of overlays that don't use the part methods. Gets called by {@link #renderFrontPart} by default.
     */
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    /**
     * Advances the overlay by one frame. Runs exactly once per frame, before the first part of that frame gets drawn.
     *
     * @param deltaSeconds time since the last update, from 0 (first update) to 0.1 seconds (longer gaps get shortened)
     * @param mouseX pointer position in GUI pixels
     * @param mouseY pointer position in GUI pixels
     */
    protected void updateFrame(float deltaSeconds, int mouseX, int mouseY) {
    }

    /**
     * Draws the screen-wide content meant to be behind the elements ({@link DecorationOverlayPart#BACK}).
     */
    protected void renderBackPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    /**
     * Draws the decorations attached to elements ({@link DecorationOverlayPart#ELEMENT}). Always part of the foreground pass.
     */
    protected void renderElementPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    /**
     * Draws the screen-wide content meant to be in front of the elements ({@link DecorationOverlayPart#FRONT}).
     * Calls {@link #render} by default, so overlays that only implement that one draw front content.
     */
    protected void renderFrontPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.render(graphics, mouseX, mouseY, partial);
    }

    /**
     * Gets called when the user clicks anywhere on the screen while the overlay is shown, before any widget sees the click.
     * The click always goes on to the widgets, whatever the overlay does here. Not called in the layout editor.
     *
     * @param button the mouse button, compare it with {@code InputConstants.MOUSE_BUTTON_LEFT} and the like
     */
    protected void onPointerClicked(double mouseX, double mouseY, int button) {
    }

    @NotNull
    public DecorationOverlayLayers getLayers() {
        return this.layers.tryGetNonNullElse(DecorationOverlayLayers.DEFAULT);
    }

    /**
     * Runs one render pass of the overlay: the frame's update if this is its first pass, then the parts the Layers setting gives this pass.
     * Does nothing if the overlay is hidden or the pass already ran in this frame.
     */
    @ApiStatus.Internal
    public final void _renderPass(@NotNull DecorationOverlayPass pass, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (!this.showOverlay.tryGetNonNullElse(false)) return;
        if (!this.frameTracker.beginPass(pass)) return;
        if (this.frameTracker.isUpdatePending()) {
            this.updateFrame(this.frameTracker.consumeUpdate(), mouseX, mouseY);
        }
        List<DecorationOverlayPart> parts = this.getLayers().getParts(pass);
        for (int i = 0; i < parts.size(); i++) {
            this.renderPart(parts.get(i), graphics, mouseX, mouseY, partial);
        }
    }

    private void renderPart(@NotNull DecorationOverlayPart part, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        switch (part) {
            case BACK -> this.renderBackPart(graphics, mouseX, mouseY, partial);
            case ELEMENT -> this.renderElementPart(graphics, mouseX, mouseY, partial);
            case FRONT -> {
                this.renderFrontPart(graphics, mouseX, mouseY, partial);
                for (int i = 0; i < this.renderables.size(); i++) {
                    this.renderables.get(i).render(graphics, mouseX, mouseY, partial);
                }
            }
        }
    }

    /**
     * Runs one render pass of all overlays in the list, in list order.
     */
    @ApiStatus.Internal
    public static void _renderPassOfAll(@NotNull List<Pair<AbstractDecorationOverlayBuilder<?>, AbstractDecorationOverlay<?>>> overlays, @NotNull DecorationOverlayPass pass, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        for (int i = 0; i < overlays.size(); i++) {
            overlays.get(i).getSecond()._renderPass(pass, graphics, mouseX, mouseY, partial);
        }
    }

    /**
     * How often the overlay advanced its simulation so far. For checks of the dev-only overlay preview.
     */
    @ApiStatus.Internal
    public long _getUpdateCount() {
        return this.frameTracker.getUpdateCount();
    }

    /**
     * How often the pass drew the overlay so far. For checks of the dev-only overlay preview.
     */
    @ApiStatus.Internal
    public long _getPassCount(@NotNull DecorationOverlayPass pass) {
        return this.frameTracker.getPassCount(pass);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        if (this.showOverlay.tryGetNonNullElse(false) && !isEditor()) this.onPointerClicked(event.x(), event.y(), event.button());
        return ContainerEventHandler.super.mouseClicked(event, isDoubleClick);
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

    /**
     * Gets called every {@link Screen} tick after all {@link AbstractElement}s got ticked.
     */
    public void tick(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
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
    public void discardSnapshot(@NotNull Object snapshot) {
        var e = LayoutEditorScreen.getCurrentInstance();
        if (e != null) e.history.discardSnapshot((LayoutEditorHistory.Snapshot) snapshot);
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

    /** Only for the render thread, which is the one that collects collision boxes. */
    private static final int[] SHOWN_AREA = new int[4];

    @Nullable
    protected static CollisionAreaBounds getAsCollisionBox(@NotNull Object o) {
        if (o instanceof AbstractElement e) {
            if (!e.shouldRender()) return null;
            if ((e instanceof HideableElement h) && h.isHidden()) return null;
            if (!e.shouldBeAffectedByDecorationOverlays.tryGetNonNull()) return null;
            // What a container cuts off of its children is not there for anything to collide with
            int[] shown = SHOWN_AREA;
            if (!e.getShownScreenArea(shown)) return null;
            return new CollisionAreaBounds(shown[0], shown[1], shown[2] - shown[0], shown[3] - shown[1]);
        }
        if (o instanceof PlainTextButton) return null;
        if (o instanceof AbstractButton b) return new CollisionAreaBounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        if (o instanceof EditBox b) return new CollisionAreaBounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        if (o instanceof AbstractSliderButton s) return new CollisionAreaBounds(s.getX(), s.getY(), s.getWidth(), s.getHeight());
        return null;
    }

    protected static @NotNull List<CollisionAreaBounds> collectCollisionBoxes(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
        if (isEditor()) {
            return List.of();
        }
        List<CollisionAreaBounds> collisionBoxes = new ArrayList<>();
        elements.forEach(element -> {
            var c = getAsCollisionBox(element);
            if (c != null) {
                collisionBoxes.add(c);
            }
        });
        return collisionBoxes;
    }

}
