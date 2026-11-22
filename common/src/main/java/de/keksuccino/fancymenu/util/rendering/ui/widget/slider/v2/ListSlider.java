package de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("all")
public class ListSlider<T> extends AbstractExtendedSlider {

    @NotNull
    protected List<T> listValues;
    @NotNull
    protected ConsumingSupplier<T, String> listValueStringSupplier = consumes -> (consumes != null) ? consumes.toString() : "";

    public ListSlider(int x, int y, int width, int height, Component label, @NotNull List<T> listValues, int preSelectedIndex) {
        super(x, y, width, height, label, 0);
        this.listValues = new ArrayList<>(listValues);
        if (this.listValues.size() < 2) {
            throw new RuntimeException("Not enough list values! At least 2 list values needed!");
        }
        this.setSelectedIndex(preSelectedIndex);
    }

    @Override
    public @NotNull String getValueDisplayText() {
        return this.listValueStringSupplier.get(this.getSelectedListValue());
    }

    @NotNull
    public T getSelectedListValue() {
        return this.listValues.get(Math.min(this.listValues.size()-1, Math.max(0, this.getSelectedIndex())));
    }

    public int getSelectedIndex() {
        if (!this.listValues.isEmpty()) {
            double maxValue = this.listValues.size()-1;
            double value = Mth.clamp(this.value, 0.0D, 1.0D);
            int index = (int) Mth.lerp(value, 0, maxValue);
            // The value of index i is i / max, which times max can come out a tiny bit below i (like 15 / 22 * 22 = 14.999...),
            // so the index after the truncated one wins when the value reaches its exact value
            if ((index < maxValue) && (value >= ((index + 1) / maxValue))) index++;
            return index;
        }
        return 0;
    }

    public ListSlider<T> setSelectedIndex(double index) {
        if (!this.listValues.isEmpty()) {
            double minValue = 0;
            double maxValue = this.listValues.size()-1;
            this.setValue(((Mth.clamp(index, minValue, maxValue) - minValue) / (maxValue - minValue)));
        }
        return this;
    }

    /**
     * Replaces the values without notifying the value update listener, because the user didn't move the slider. The selected value
     * stays selected if the new values have it too, otherwise the index stays selected, as far as the new values reach.
     */
    public void replaceListValues(@NotNull List<T> listValues) {
        if (listValues.size() < 2) {
            throw new RuntimeException("Not enough list values! At least 2 list values needed!");
        }
        int oldIndex = this.getSelectedIndex();
        T selected = this.getSelectedListValue();
        this.listValues = new ArrayList<>(listValues);
        int newIndex = oldIndex;
        // Checking the old index first keeps the right one of equal values
        if ((oldIndex >= this.listValues.size()) || !Objects.equals(this.listValues.get(oldIndex), selected)) {
            int found = this.listValues.indexOf(selected);
            newIndex = (found >= 0) ? found : Math.min(oldIndex, this.listValues.size()-1);
        }
        this.value = (double) newIndex / (this.listValues.size()-1);
        this.updateMessage();
    }

    public void setListValueStringSupplier(@NotNull ConsumingSupplier<T, String> supplier) {
        this.listValueStringSupplier = supplier;
    }

}
