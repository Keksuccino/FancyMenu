package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AfmaMultiCopy {

    @Nullable
    protected List<AfmaCopyRect> copy_rects;

    public AfmaMultiCopy() {
    }

    public AfmaMultiCopy(@NotNull List<AfmaCopyRect> copyRects) {
        Objects.requireNonNull(copyRects);
        if (copyRects.size() < 2) {
            throw new IllegalArgumentException("AFMA multi-copy operations require at least 2 copy rectangles");
        }
        ArrayList<AfmaCopyRect> copiedRects = new ArrayList<>(copyRects.size());
        for (AfmaCopyRect copyRect : copyRects) {
            copiedRects.add(Objects.requireNonNull(copyRect, "AFMA multi-copy rectangles cannot contain NULL entries"));
        }
        this.copy_rects = Collections.unmodifiableList(copiedRects);
    }

    @NotNull
    public List<AfmaCopyRect> getCopyRects() {
        return (this.copy_rects != null) ? this.copy_rects : List.of();
    }

    public int getCopyRectCount() {
        return this.getCopyRects().size();
    }

    public long getTotalArea() {
        long totalArea = 0L;
        for (AfmaCopyRect copyRect : this.getCopyRects()) {
            totalArea += copyRect.getArea();
        }
        return totalArea;
    }

    public void validate(@NotNull String context, int canvasWidth, int canvasHeight) {
        List<AfmaCopyRect> copyRects = this.getCopyRects();
        if (copyRects.size() < 2) {
            throw new IllegalArgumentException(context + " requires at least 2 copy rectangles");
        }
        for (int copyIndex = 0; copyIndex < copyRects.size(); copyIndex++) {
            Objects.requireNonNull(copyRects.get(copyIndex), context + " contains a NULL copy rectangle")
                    .validate(context + " copy rectangle " + copyIndex, canvasWidth, canvasHeight);
        }
    }

}
