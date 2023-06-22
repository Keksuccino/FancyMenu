package de.keksuccino.fancymenu.customization.element.elements.slider;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.slider.AdvancedSliderButton;
import de.keksuccino.fancymenu.util.rendering.ui.slider.ListSliderButton;
import de.keksuccino.fancymenu.util.rendering.ui.slider.RangeSliderButton;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SliderElement extends AbstractElement {

    public String linkedVariable;
    public SliderType type = SliderType.RANGE;
    public List<String> listValues = new ArrayList<>();
    public int minRangeValue = 1;
    public int maxRangeValue = 10;
    public String labelPrefix;
    public String labelSuffix;

    public AdvancedSliderButton slider;

    public SliderElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    public void initializeSlider() {
        String valString = null;
        if (linkedVariable != null) {
            if (VariableHandler.variableExists(linkedVariable)) {
                valString = Objects.requireNonNull(VariableHandler.getVariable(linkedVariable)).value;
            }
        }
        if (this.type == SliderType.RANGE) {
            int selectedRangeValue = this.minRangeValue;
            if ((valString != null) && MathUtils.isInteger(valString)) {
                selectedRangeValue = Integer.parseInt(valString);
            }
            this.slider = new RangeSliderButton(this.getX(), this.getY(), this.getWidth(), this.getHeight(), true, this.minRangeValue, this.maxRangeValue, selectedRangeValue, (apply) -> {
                if (linkedVariable != null) {
                    VariableHandler.setVariable(linkedVariable, "" + ((RangeSliderButton)apply).getSelectedRangeValue());
                }
            });
        }
        if (this.type == SliderType.LIST) {
            int selectedIndex = 0;
            if (valString != null) {
                int i = 0;
                for (String s : this.listValues) {
                    if (s.equals(valString)) {
                        selectedIndex = i;
                        break;
                    }
                    i++;
                }
            }
            this.slider = new ListSliderButton(this.getX(), this.getY(), this.getWidth(), this.getHeight(), true, this.listValues, selectedIndex, (apply) -> {
                if (linkedVariable != null) {
                    VariableHandler.setVariable(linkedVariable, ((ListSliderButton)apply).getSelectedListValue());
                }
            });
        }
        if (this.slider != null) {
            this.slider.setLabelPrefix(this.labelPrefix);
            this.slider.setLabelSuffix(this.labelSuffix);
        }
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            RenderSystem.enableBlend();

            //Handle editor mode for text field
            if (isEditor()) {
                this.slider.active = false;
            }

            this.slider.x = this.getX();
            this.slider.y = this.getY();
            this.slider.setWidth(this.getWidth());
            ((IMixinAbstractWidget)this.slider).setHeightFancyMenu(this.getHeight());
            this.slider.render(pose, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime());

            //Update variable value on change
            if (this.linkedVariable != null) {
                if (VariableHandler.variableExists(this.linkedVariable)) {
                    String valString = Objects.requireNonNull(VariableHandler.getVariable(this.linkedVariable)).value;
                    if (valString != null) {
                        if (this.type == SliderType.RANGE) {
                            if (MathUtils.isInteger(valString)) {
                                int val = Integer.parseInt(valString);
                                if (((RangeSliderButton)this.slider).getSelectedRangeValue() != val) {
                                    ((RangeSliderButton)this.slider).setSelectedRangeValue(val);
                                }
                            }
                        }
                        if (this.type == SliderType.LIST) {
                            if (!((ListSliderButton)this.slider).getSelectedListValue().equals(valString)) {
                                int newIndex = 0;
                                int i = 0;
                                for (String s : this.listValues) {
                                    if (s.equals(valString)) {
                                        newIndex = i;
                                        break;
                                    }
                                    i++;
                                }
                                ((ListSliderButton)this.slider).setSelectedIndex(newIndex);
                            }
                        }
                    }
                }
            }

            if (this.type == SliderType.RANGE) {
                ((RangeSliderButton)this.slider).maxValue = this.maxRangeValue;
                ((RangeSliderButton)this.slider).minValue = this.minRangeValue;
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        }

    }

    public enum SliderType {

        LIST("list"),
        RANGE("range");

        final String name;

        SliderType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SliderType getByName(String name) {
            for (SliderType i : SliderType.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }

}
