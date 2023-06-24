package de.keksuccino.fancymenu.util.rendering.ui.screen.snapshot;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleDrawableScreenSnapshot {

    protected DynamicTexture currentSnapshot = null;
    protected ResourceLocation currentSnapshotLocation = null;

    @NotNull
    public static SimpleDrawableScreenSnapshot create() {
        return new SimpleDrawableScreenSnapshot();
    }

    public SimpleDrawableScreenSnapshot() {
        this.createSnapshot();
    }

    protected void createSnapshot() {
        try {
            NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
            this.currentSnapshot = new DynamicTexture(image);
            this.currentSnapshotLocation = Minecraft.getInstance().getTextureManager().register("drawable_screen_snapshot", this.currentSnapshot);
        } catch (Exception ex) {
            ex.printStackTrace();
            this.closeCurrentSnapshot();
        }
    }

    protected void closeCurrentSnapshot() {
        try {
            if (this.currentSnapshot != null) this.currentSnapshot.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.currentSnapshot = null;
        this.currentSnapshotLocation = null;
    }

    public void close() {
        this.closeCurrentSnapshot();
    }

    @Nullable
    public DynamicTexture getSnapshot() {
        return this.currentSnapshot;
    }

    @Nullable
    public ResourceLocation getSnapshotLocation() {
        return this.currentSnapshotLocation;
    }

}
