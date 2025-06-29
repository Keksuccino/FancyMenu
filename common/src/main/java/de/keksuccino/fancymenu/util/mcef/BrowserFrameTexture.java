package de.keksuccino.fancymenu.util.mcef;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.NotNull;

public class BrowserFrameTexture extends AbstractTexture {

    public BrowserFrameTexture(int id) {
        this.id = id;
    }

    @Override
    public void setFilter(boolean $$0, boolean $$1) {
        // do nothing
    }

    @Override
    public void setFilter(@NotNull TriState $$0, boolean $$1) {
        // do nothing
    }

    @Override
    public void setClamp(boolean $$0) {
        // do nothing
    }

    @Override
    public void bind() {
        // do nothing
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void releaseId() {
        // do nothing
    }

}
