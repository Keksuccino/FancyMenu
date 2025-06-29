package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CheckboxElement extends AbstractElement implements ExecutableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected CheckboxButton checkbox;
    @Nullable
    public String tooltip;
    @Nullable
    public ResourceSupplier<ITexture> checkmarkTexture = null;
    @Nullable
    public ResourceSupplier<ITexture> backgroundTextureNormal = null;
    @Nullable
    public ResourceSupplier<ITexture> backgroundTextureHover = null;
    @Nullable
    public ResourceSupplier<ITexture> backgroundTextureInactive = null;
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
        this.actionExecutor.addValuePlaceholder("value", () -> "" + CheckboxStatesHandler.getForCheckboxElement(this));
    }

    /**
     * Call this after setting a new container instance.
     */
    public void prepareLoadingRequirementContainer() {
        this.loadingRequirementContainer.addValuePlaceholder("value", () -> "" + CheckboxStatesHandler.getForCheckboxElement(this));
    }

    @Override
    public void afterConstruction() {
        this.checkbox = new CheckboxButton(0, 0, 20, 20, (checkbox, state) -> {
            CheckboxStatesHandler.setForCheckboxElement(this, state);
            this.actionExecutor.execute();
        });
        this.checkbox.setCheckboxState(CheckboxStatesHandler.getForCheckboxElement(this), false);
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
        this.updateWidgetTextures();
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

    protected void updateWidgetTextures() {

        if (this.checkbox == null) return;
        
        if (this.checkmarkTexture != null) {
            ITexture texture = this.checkmarkTexture.get();
            if (texture != null) {
                this.checkbox.setCustomCheckboxCheckmarkTexture(texture);
            }
        } else {
            this.checkbox.setCustomCheckboxCheckmarkTexture(null);
        }

        if (this.backgroundTextureNormal != null) {
            ITexture texture = this.backgroundTextureNormal.get();
            if (texture != null) {
                this.checkbox.setCustomBackgroundTextureNormal(texture);
            }
        } else {
            this.checkbox.setCustomBackgroundTextureNormal(null);
        }

        if (this.backgroundTextureHover != null) {
            ITexture texture = this.backgroundTextureHover.get();
            if (texture != null) {
                this.checkbox.setCustomBackgroundTextureHover(texture);
            }
        } else {
            this.checkbox.setCustomBackgroundTextureHover(null);
        }

        if (this.backgroundTextureInactive != null) {
            ITexture texture = this.backgroundTextureInactive.get();
            if (texture != null) {
                this.checkbox.setCustomBackgroundTextureInactive(texture);
            }
        } else {
            this.checkbox.setCustomBackgroundTextureInactive(null);
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
