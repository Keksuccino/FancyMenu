package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CheckboxButton;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CheckboxElement extends AbstractElement implements ExecutableElement {

    @Nullable
    protected CheckboxButton checkbox;
    @Nullable
    public String tooltip;
    public boolean persistentState = false;
    @Nullable
    public DrawableColor borderColor = null;
    @Nullable
    public DrawableColor backgroundColor = null;
    @Nullable
    public ResourceSupplier<ITexture> checkmarkTexture = null;
    public ResourceSupplier<IAudio> hoverSound;
    public ResourceSupplier<IAudio> clickSound;
    public boolean navigatable = true;
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
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
        this.actionExecutor.addValuePlaceholder("value", () -> "" + this.persistentState);
    }

    /**
     * Call this after setting a new container instance.
     */
    public void prepareLoadingRequirementContainer() {
        this.loadingRequirementContainer.addValuePlaceholder("value", () -> "" + this.persistentState);
    }

    @Override
    public void afterConstruction() {
        this.checkbox = new CheckboxButton(0, 0, 20, 20, (checkbox, state) -> {
            this.persistentState = state;
            this.actionExecutor.execute();
        });
        this.checkbox.setCheckboxState(this.persistentState, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        
        if (this.checkbox == null) return;
        
        this.updateWidget();
        
        if (!this.shouldRender()) return;

        //Prevents crashes related to dividing by zero
        if (this.checkbox.getHeight() <= 0) return;
        if (this.checkbox.getWidth() <= 0) return;

        this.checkbox.render(graphics, mouseX, mouseY, partial);
        
    }

    protected void updateWidget() {

        if (this.checkbox == null) return;
        
        this.updateWidgetActiveState();
        this.updateWidgetVisibility();
        this.updateWidgetAlpha();
        this.updateWidgetTooltip();
        this.updateWidgetHoverSound();
        this.updateWidgetClickSound();
        this.updateWidgetColors();
        this.updateWidgetSize();
        this.updateWidgetPosition();
        this.updateWidgetNavigatable();

    }

    protected void updateWidgetActiveState() {
        if (this.checkbox == null) return;
        this.checkbox.active = this.activeStateSupplier.requirementsMet();
    }

    protected void updateWidgetNavigatable() {
        if (this.checkbox instanceof NavigatableWidget w) {
            w.setNavigatable(this.navigatable);
        }
    }

    protected void updateWidgetVisibility() {
        if (this.checkbox instanceof CustomizableWidget w) {
            w.setHiddenFancyMenu(!this.shouldRender());
        }
    }

    protected void updateWidgetAlpha() {
        if (this.checkbox == null) return;
        this.checkbox.setAlpha(this.opacity);
    }

    protected void updateWidgetPosition() {
        if (this.checkbox == null) return;
        this.checkbox.setX(this.getAbsoluteX());
        this.checkbox.setY(this.getAbsoluteY());
    }

    protected void updateWidgetSize() {
        if (this.checkbox == null) return;
        this.checkbox.setWidth(this.getAbsoluteWidth());
        this.checkbox.setHeight(this.getAbsoluteHeight());
    }

    protected void updateWidgetTooltip() {
        if ((this.tooltip != null) && (this.checkbox != null) && this.checkbox.isHovered() && this.checkbox.visible && this.shouldRender() && !isEditor()) {
            String tooltip = this.tooltip.replace("%n%", "\n");
            TooltipHandler.INSTANCE.addWidgetTooltip(this.checkbox, Tooltip.of(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(tooltip), "\n")), false, true);
        }
    }

    protected void updateWidgetHoverSound() {
        if (this.checkbox instanceof CustomizableWidget w) {
            w.setHoverSoundFancyMenu((this.hoverSound != null) ? this.hoverSound.get() : null);
        }
    }

    protected void updateWidgetClickSound() {
        if (this.checkbox instanceof CustomizableWidget w) {
            w.setCustomClickSoundFancyMenu((this.clickSound != null) ? this.clickSound.get() : null);
        }
    }

    protected void updateWidgetColors() {
        if (this.checkbox == null) return;
        
        this.checkbox.setCustomCheckboxBorderColor(this.borderColor);
        this.checkbox.setCustomCheckboxBackgroundColor(this.backgroundColor);
        
        if (this.checkmarkTexture != null) {
            ITexture texture = this.checkmarkTexture.get();
            if (texture != null) {
                this.checkbox.setCustomCheckboxCheckmarkTexture(texture);
            }
        } else {
            this.checkbox.setCustomCheckboxCheckmarkTexture(null);
        }
    }

    @Override
    @Nullable
    public List<GuiEventListener> getWidgetsToRegister() {
        if (this.checkbox == null) return null;
        return List.of(this.checkbox);
    }

    @Override
    @NotNull
    public GenericExecutableBlock getExecutableBlock() {
        return this.actionExecutor;
    }

}
