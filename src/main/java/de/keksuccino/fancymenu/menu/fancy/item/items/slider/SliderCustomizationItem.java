package de.keksuccino.fancymenu.menu.fancy.item.items.slider;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.slider.AdvancedSliderButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.slider.ListSliderButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.slider.RangeSliderButton;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SliderCustomizationItem extends CustomizationItem {

    public String linkedVariable;
    public SliderType type = SliderType.RANGE;
    public List<String> listValues = new ArrayList<>();
    public int minRangeValue = 1;
    public int maxRangeValue = 10;
    public String labelPrefix;
    public String labelSuffix;

    public AdvancedSliderButton slider;

    public SliderCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        super(parentContainer, item);

        this.linkedVariable = item.getEntryValue("linked_variable");

        String sliderTypeString = item.getEntryValue("slider_type");
        if (sliderTypeString != null) {
            SliderType t = SliderType.getByName(sliderTypeString);
            if (t != null) {
                this.type = t;
            }
        }

        this.labelPrefix = item.getEntryValue("label_prefix");

        this.labelSuffix = item.getEntryValue("label_suffix");

        if (this.type == SliderType.RANGE) {
            String minRangeString = item.getEntryValue("min_range_value");
            if (minRangeString != null) {
                if (MathUtils.isInteger(minRangeString)) {
                    this.minRangeValue = Integer.parseInt(minRangeString);
                }
            }
            String maxRangeString = item.getEntryValue("max_range_value");
            if (maxRangeString != null) {
                if (MathUtils.isInteger(maxRangeString)) {
                    this.maxRangeValue = Integer.parseInt(maxRangeString);
                }
            }
        }
        if (this.type == SliderType.LIST) {
            String listValueString = item.getEntryValue("list_values");
            if (listValueString != null) {
                this.listValues = deserializeValuesList(listValueString);
            }
        }
        if (this.listValues.isEmpty()) {
            this.listValues.add("some_value");
            this.listValues.add("another_value");
            this.listValues.add("yet_another_value");
        }
        if (this.listValues.size() == 1) {
            this.listValues.add("dummy_value");
        }

        this.initializeSlider();

    }

    public void initializeSlider() {
        String valString = null;
        if (linkedVariable != null) {
            valString = VariableHandler.getVariable(linkedVariable);
        }
        Screen current = Minecraft.getInstance().screen;
        if (this.type == SliderType.RANGE) {
            int selectedRangeValue = this.minRangeValue;
            if ((valString != null) && MathUtils.isInteger(valString)) {
                selectedRangeValue = Integer.parseInt(valString);
            }
            this.slider = new RangeSliderButton(this.getPosX(current), this.getPosY(current), this.width, this.height, true, this.minRangeValue, this.maxRangeValue, selectedRangeValue, (apply) -> {
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
            this.slider = new ListSliderButton(this.getPosX(current), this.getPosY(current), this.width, this.height, true, this.listValues, selectedIndex, (apply) -> {
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
    public void render(PoseStack matrix, Screen menu) throws IOException {

        if (this.shouldRender()) {

            RenderSystem.enableBlend();

            //Handle editor mode for text field
            if (isEditorActive()) {
                this.slider.active = false;
            }

            this.slider.x = this.getPosX(menu);
            this.slider.y = this.getPosY(menu);
            this.slider.setWidth(this.width);
            this.slider.setHeight(this.height);
            this.slider.render(matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime());

            //Update variable value on change
            if (this.linkedVariable != null) {
                String valString = VariableHandler.getVariable(this.linkedVariable);
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

            if (this.type == SliderType.RANGE) {
                ((RangeSliderButton)this.slider).maxValue = this.maxRangeValue;
                ((RangeSliderButton)this.slider).minValue = this.minRangeValue;
            }

        }

    }

    public static String serializeValuesList(List<String> list) {
        String s = "";
        for (String s2 : list) {
            s += s2 + ";";
        }
        return s;
    }

    public static List<String> deserializeValuesList(String list) {
        List<String> l = new ArrayList<>();
        if (list.contains(";")) {
            for (String s : list.split("[;]")) {
                if (!s.replace(" ", "").equals("")) {
                    l.add(s);
                }
            }
        }
        return l;
    }

    public static enum SliderType {

        LIST("list"),
        RANGE("range");

        String name;

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
