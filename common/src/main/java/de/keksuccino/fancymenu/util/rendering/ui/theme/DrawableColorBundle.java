package de.keksuccino.fancymenu.util.rendering.ui.theme;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import org.jetbrains.annotations.NotNull;

public class DrawableColorBundle {

    protected DrawableColor normal;
    protected DrawableColor blur;

    @NotNull
    public static DrawableColorBundle of(@NotNull DrawableColor normal, @NotNull DrawableColor blur) {
        return new DrawableColorBundle(normal, blur);
    }

    protected DrawableColorBundle() {
    }

    protected DrawableColorBundle(@NotNull DrawableColor normal, @NotNull DrawableColor blur) {
        this.normal = normal;
        this.blur = blur;
    }

    public DrawableColorBundle set(@NotNull DrawableColor normal, @NotNull DrawableColor blur) {
        this.normal = normal;
        this.blur = blur;
        return this;
    }

    public DrawableColor get() {
        return this.get(UIBase.shouldBlur());
    }

    public DrawableColor get(boolean blur) {
        return blur ? this.blur() : this.normal();
    }

    public DrawableColor normal() {
        return this.normal;
    }

    public DrawableColor blur() {
        return this.blur;
    }

}
