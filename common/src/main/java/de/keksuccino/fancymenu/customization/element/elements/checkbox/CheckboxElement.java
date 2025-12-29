package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.properties.Property;
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
import java.util.Objects;

public class CheckboxElement extends AbstractElement implements ExecutableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected CheckboxButton checkbox;

    public final Property<String> tooltip = putProperty(Property.stringProperty("description", null, true, true, "fancymenu.elements.button.tooltip")
            .setUserInputTextValidator(TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR)
            .setValueGetProcessor(value -> value.replace("%n%", "\n"))
            .setValueSetProcessor(value -> (value != null) ? value.replace("\n", "%n%") : null));
    public final Property<ResourceSupplier<ITexture>> checkmarkTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "checkmark_texture", null, "fancymenu.elements.checkbox.checkmark_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> backgroundTextureNormal = putProperty(Property.resourceSupplierProperty(ITexture.class, "background_texture_normal", null, "fancymenu.elements.checkbox.background_texture_normal", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> backgroundTextureHover = putProperty(Property.resourceSupplierProperty(ITexture.class, "background_texture_hover", null, "fancymenu.elements.checkbox.background_texture_hover", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> backgroundTextureInactive = putProperty(Property.resourceSupplierProperty(ITexture.class, "background_texture_inactive", null, "fancymenu.elements.checkbox.background_texture_inactive", true, true, true, null));
    public final Property<Boolean> variableMode = putProperty(Property.booleanProperty("variable_mode", false, "fancymenu.elements.checkbox.variable_mode"));
    public final Property<String> linkedVariable = putProperty(Property.stringProperty("linked_variable", null, false, false, "fancymenu.elements.checkbox.editor.set_variable").setUserInputTextValidator(TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR));
    public final Property<ResourceSupplier<IAudio>> hoverSound = putProperty(Property.resourceSupplierProperty(IAudio.class, "hoversound", null, "fancymenu.elements.button.hoversound", true, true, true, null));
    public final Property<ResourceSupplier<IAudio>> clickSound = putProperty(Property.resourceSupplierProperty(IAudio.class, "clicksound", null, "fancymenu.elements.button.clicksound", true, true, true, null));
    public final Property<Boolean> navigatable = putProperty(Property.booleanProperty("navigatable", true, "fancymenu.elements.widgets.generic.navigatable"));
    public final Property<RequirementContainer> activeStateSupplier = putProperty(Property.requirementContainerProperty("widget_active_state_requirement_container_identifier", new RequirementContainer(), "fancymenu.elements.button.active_state_controller"));

    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();

    public CheckboxElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.prepareExecutableBlock();
        this.allowDepthTestManipulation = true;
    }

    /**
     * Call this after setting a new block instance.
     */
    public void prepareExecutableBlock() {
        this.actionExecutor.addValuePlaceholder("value", () -> "" + this.getCheckboxStateForPlaceholders());
    }

    /**
     * Call this after setting a new container instance.
     */
    public void prepareLoadingRequirementContainer() {
        this.requirementContainer.addValuePlaceholder("value", () -> "" + this.getCheckboxStateForPlaceholders());
    }

    @Override
    public void afterConstruction() {
        this.checkbox = new CheckboxButton(0, 0, 20, 20, (checkbox, state) -> {
            if (this.variableMode.tryGetNonNull()) {
                String variable = this.linkedVariable.get();
                if (variable != null) {
                    if (!isEditor()) {
                        VariableHandler.setVariable(variable, Boolean.toString(state));
                    }
                }
            } else {
                CheckboxStatesHandler.setForCheckboxElement(this, state);
            }
            this.actionExecutor.execute();
        });
        this.syncCheckboxStateOnLoad();
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

        this.syncCheckboxStateFromVariable();
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

    protected void syncCheckboxStateOnLoad() {
        if (this.checkbox == null) return;
        boolean state = CheckboxStatesHandler.getForCheckboxElement(this);
        if (this.variableMode.tryGetNonNull()) {
            String variable = this.linkedVariable.get();
            if (variable != null) {
                if (VariableHandler.variableExists(variable)) {
                    String value = Objects.requireNonNull(VariableHandler.getVariable(variable)).getValue();
                    state = this.parseBooleanVariableValue(value);
                } else if (!isEditor()) {
                    VariableHandler.setVariable(variable, Boolean.toString(state));
                }
            }
        }
        this.checkbox.setCheckboxState(state, false);
    }

    protected void syncCheckboxStateFromVariable() {
        if (this.checkbox == null) return;
        if (!this.variableMode.tryGetNonNull()) return;
        String variable = this.linkedVariable.get();
        if (variable == null) return;
        if (!VariableHandler.variableExists(variable)) return;
        String value = Objects.requireNonNull(VariableHandler.getVariable(variable)).getValue();
        boolean state = this.parseBooleanVariableValue(value);
        if (this.checkbox.getCheckboxState() != state) {
            this.checkbox.setCheckboxState(state, false);
        }
    }

    protected boolean getCheckboxStateForPlaceholders() {
        if (this.variableMode.tryGetNonNull()) {
            String variable = this.linkedVariable.get();
            if ((variable != null) && VariableHandler.variableExists(variable)) {
                String value = Objects.requireNonNull(VariableHandler.getVariable(variable)).getValue();
                return this.parseBooleanVariableValue(value);
            }
        }
        return CheckboxStatesHandler.getForCheckboxElement(this);
    }

    protected boolean parseBooleanVariableValue(@Nullable String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.equalsIgnoreCase("true") || trimmed.equals("1");
    }

    protected void updateWidgetActiveState() {
        if (this.checkbox == null) return;
        this.checkbox.active = this.activeStateSupplier.tryGetNonNull().requirementsMet();
    }

    protected void updateWidgetNavigatable() {
        if (this.checkbox instanceof NavigatableWidget w) {
            w.setNavigatable(this.navigatable.tryGetNonNull());
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
        String tooltip = this.tooltip.get();
        if ((tooltip != null) && (this.checkbox != null) && this.checkbox.isHovered() && this.checkbox.visible && this.shouldRender() && !isEditor()) {
            String converted = tooltip.replace("%n%", "\n");
            TooltipHandler.INSTANCE.addWidgetTooltip(this.checkbox, Tooltip.of(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(converted), "\n")), false, true);
        }
    }

    protected void updateWidgetHoverSound() {
        if (this.checkbox instanceof CustomizableWidget w) {
            ResourceSupplier<IAudio> supplier = this.hoverSound.get();
            w.setHoverSoundFancyMenu((supplier != null) ? supplier.get() : null);
        }
    }

    protected void updateWidgetClickSound() {
        if (this.checkbox instanceof CustomizableWidget w) {
            ResourceSupplier<IAudio> supplier = this.clickSound.get();
            w.setCustomClickSoundFancyMenu((supplier != null) ? supplier.get() : null);
        }
    }

    protected void updateWidgetTextures() {

        if (this.checkbox == null) return;
        
        ResourceSupplier<ITexture> checkmarkSupplier = this.checkmarkTexture.get();
        if (checkmarkSupplier != null) {
            ITexture texture = checkmarkSupplier.get();
            if (texture != null) {
                this.checkbox.setCustomCheckboxCheckmarkTexture(texture);
            }
        } else {
            this.checkbox.setCustomCheckboxCheckmarkTexture(null);
        }

        ResourceSupplier<ITexture> normalSupplier = this.backgroundTextureNormal.get();
        if (normalSupplier != null) {
            ITexture texture = normalSupplier.get();
            if (texture != null) {
                this.checkbox.setCustomBackgroundTextureNormal(texture);
            }
        } else {
            this.checkbox.setCustomBackgroundTextureNormal(null);
        }

        ResourceSupplier<ITexture> hoverSupplier = this.backgroundTextureHover.get();
        if (hoverSupplier != null) {
            ITexture texture = hoverSupplier.get();
            if (texture != null) {
                this.checkbox.setCustomBackgroundTextureHover(texture);
            }
        } else {
            this.checkbox.setCustomBackgroundTextureHover(null);
        }

        ResourceSupplier<ITexture> inactiveSupplier = this.backgroundTextureInactive.get();
        if (inactiveSupplier != null) {
            ITexture texture = inactiveSupplier.get();
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
