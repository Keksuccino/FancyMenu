package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.checkbox.ExtendedCheckbox;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CheckboxElement extends AbstractElement implements ExecutableElement {

    // Default checkbox textures
    public static final ResourceLocation DEFAULT_BACKGROUND_NORMAL = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/gui/checkbox_background_normal.png");
    public static final ResourceLocation DEFAULT_BACKGROUND_HOVER = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/gui/checkbox_background_hover.png");
    public static final ResourceLocation DEFAULT_BACKGROUND_INACTIVE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/gui/checkbox_background_inactive.png");
    public static final ResourceLocation DEFAULT_CHECKMARK = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/gui/checkbox_checkmark.png");

    @Nullable
    private ExtendedCheckbox widget;
    
    // Checkbox properties
    public boolean checked = false;
    @Nullable
    public String label;
    public String tooltip;
    
    // Textures
    public ResourceSupplier<ITexture> backgroundTextureNormal;
    public ResourceSupplier<ITexture> backgroundTextureHover;
    public ResourceSupplier<ITexture> backgroundTextureInactive;
    public ResourceSupplier<ITexture> checkmarkTexture;
    
    // Sound
    public ResourceSupplier<IAudio> clickSound;
    public ResourceSupplier<IAudio> hoverSound;
    
    // Behavior
    public boolean restartBackgroundAnimationsOnHover = true;
    public boolean nineSliceCustomBackground = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;
    public boolean navigatable = true;
    
    // Action execution
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    
    // Active state
    @NotNull
    public LoadingRequirementContainer activeStateSupplier = new LoadingRequirementContainer();

    public CheckboxElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.prepareExecutableBlock();
    }
    
    /**
     * Call this after setting a new block instance.
     */
    public void prepareExecutableBlock() {
        this.actionExecutor.addValuePlaceholder("checked", () -> String.valueOf(this.checked));
        this.actionExecutor.addValuePlaceholder("value", () -> String.valueOf(this.checked));
    }
    
    /**
     * Call this after setting a new container instance.
     */
    public void prepareLoadingRequirementContainer() {
        this.loadingRequirementContainer.addValuePlaceholder("checked", () -> String.valueOf(this.checked));
        this.loadingRequirementContainer.addValuePlaceholder("value", () -> String.valueOf(this.checked));
    }

    @Override
    public void tick() {
        if (this.getWidget() == null) return;
        this.updateWidget();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.getWidget() == null) return;

        this.updateWidget();

        if (!this.shouldRender()) return;

        if (isEditor()) {
            net.minecraft.client.gui.components.Tooltip cachedVanillaTooltip = this.getWidget().getTooltip();
            boolean cachedVisible = this.getWidget().visible;
            boolean cachedActive = this.getWidget().active;
            this.getWidget().visible = true;
            this.getWidget().active = true;
            this.getWidget().setTooltip(null);
            MainThreadTaskExecutor.executeInMainThread(() -> {
                this.getWidget().visible = cachedVisible;
                this.getWidget().active = cachedActive;
                this.getWidget().setTooltip(cachedVanillaTooltip);
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }

        this.getWidget().render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void tickVisibleInvisible() {
        super.tickVisibleInvisible();
        if (this.getWidget() != null) this.updateWidget();
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        if (this.getWidget() == null) return null;
        return List.of(this.getWidget());
    }

    public void updateWidget() {
        this.updateWidgetActiveState();
        this.updateWidgetVisibility();
        this.updateWidgetAlpha();
        this.updateWidgetTooltip();
        this.updateWidgetLabel();
        this.updateWidgetHoverSound();
        this.updateWidgetClickSound();
        this.updateWidgetTexture();
        this.updateWidgetSize();
        this.updateWidgetPosition();
        this.updateWidgetNavigatable();
        this.updateWidgetCheckedState();
    }

    public void updateWidgetActiveState() {
        if (this.getWidget() == null) return;
        this.getWidget().active = this.activeStateSupplier.requirementsMet();
    }

    public void updateWidgetNavigatable() {
        if (this.getWidget() instanceof NavigatableWidget w) {
            w.setNavigatable(this.navigatable);
        }
    }

    public void updateWidgetVisibility() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setHiddenFancyMenu(!this.shouldRender());
        }
    }

    public void updateWidgetAlpha() {
        if (this.getWidget() == null) return;
        this.getWidget().setAlpha(this.opacity);
    }

    public void updateWidgetPosition() {
        if (this.getWidget() == null) return;
        this.getWidget().setX(this.getAbsoluteX());
        this.getWidget().setY(this.getAbsoluteY());
    }

    public void updateWidgetSize() {
        if (this.getWidget() == null) return;
        this.getWidget().setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget) this.getWidget()).setHeightFancyMenu(this.getAbsoluteHeight());
    }

    public void updateWidgetTooltip() {
        if ((this.tooltip != null) && (this.getWidget() != null) && this.getWidget().isHovered() && this.getWidget().visible && this.shouldRender() && !isEditor()) {
            String tooltip = this.tooltip.replace("%n%", "\n");
            TooltipHandler.INSTANCE.addWidgetTooltip(this.getWidget(), Tooltip.of(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(tooltip), "\n")), false, true);
        }
    }

    public void updateWidgetLabel() {
        if (this.getWidget() == null) return;
        if (this.label != null) {
            this.getWidget().setLabel(buildComponent(this.label));
        } else {
            this.getWidget().setLabel(Component.empty());
        }
    }

    public void updateWidgetHoverSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setHoverSoundFancyMenu((this.hoverSound != null) ? this.hoverSound.get() : null);
        }
    }

    public void updateWidgetClickSound() {
        if (this.getWidget() instanceof CustomizableWidget w) {
            w.setCustomClickSoundFancyMenu((this.clickSound != null) ? this.clickSound.get() : null);
        }
    }

    public void updateWidgetTexture() {
        RenderableResource backNormal = null;
        RenderableResource backHover = null;
        RenderableResource backInactive = null;
        RenderableResource checkmark = null;

        // Normal
        if (this.backgroundTextureNormal != null) {
            backNormal = this.backgroundTextureNormal.get();
        }
        // Hover
        if (this.backgroundTextureHover != null) {
            backHover = this.backgroundTextureHover.get();
        }
        // Inactive
        if (this.backgroundTextureInactive != null) {
            backInactive = this.backgroundTextureInactive.get();
        }
        // Checkmark
        if (this.checkmarkTexture != null) {
            checkmark = this.checkmarkTexture.get();
        }

        if (this.getWidget() instanceof ExtendedCheckbox w) {
            w.setNineSliceCustomBackground_FancyMenu(this.nineSliceCustomBackground);
            w.setNineSliceBorderX_FancyMenu(this.nineSliceBorderX);
            w.setNineSliceBorderY_FancyMenu(this.nineSliceBorderY);
            w.setCustomBackgroundNormalFancyMenu(backNormal);
            w.setCustomBackgroundHoverFancyMenu(backHover);
            w.setCustomBackgroundInactiveFancyMenu(backInactive);
            w.setCustomCheckmarkTextureFancyMenu(checkmark);
            w.setCustomBackgroundResetBehaviorFancyMenu(this.restartBackgroundAnimationsOnHover ? CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER : CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER);
        }
    }
    
    public void updateWidgetCheckedState() {
        if (this.getWidget() instanceof ExtendedCheckbox w) {
            w.setChecked(this.checked);
        }
    }

    @Nullable
    public AbstractWidget getWidget() {
        return this.widget;
    }

    public void setWidget(@Nullable ExtendedCheckbox widget) {
        this.widget = widget;
    }
    
    protected void onCheckboxToggled(boolean newValue) {
        this.checked = newValue;
        this.actionExecutor.execute();
    }

    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.actionExecutor;
    }
}
