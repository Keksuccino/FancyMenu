package de.keksuccino.fancymenu.util.rendering.ui.screen.snapshot;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DrawableScreenSnapshot {

    protected Screen screen;
    protected double scale;
    protected Minecraft mc = Minecraft.getInstance();
    protected Window window = Minecraft.getInstance().getWindow();
    protected DynamicTexture currentSnapshot = null;
    protected ResourceLocation currentSnapshotLocation = null;

    @Nullable
    public static DrawableScreenSnapshot of(Screen screen) {
        if (screen == null) return null;
        return new DrawableScreenSnapshot(screen, Minecraft.getInstance().getWindow().getGuiScale());
    }

    public DrawableScreenSnapshot(@NotNull Screen screen, double scale) {
        this.screen = Objects.requireNonNull(screen);
        this.scale = scale;
        this.resize();
    }

    public void resize() {
        try {
            double currentScale = this.window.getGuiScale();
            this.window.setGuiScale(this.scale);
            this.screen.resize(mc, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            this.window.setGuiScale(currentScale);
            this.updateSnapshot();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void updateSnapshot() {
        this.closeCurrentSnapshot();
        try {
            RenderTarget renderTarget = new TextureTarget(this.window.getWidth(), this.window.getHeight(), true, Minecraft.ON_OSX);
            renderTarget.bindWrite(true);
            this.screen.render(new PoseStack(), -10000, -10000, RenderUtils.getPartialTick());
            try { Thread.sleep(10L); } catch (InterruptedException ignored) {}
            NativeImage image = Screenshot.takeScreenshot(renderTarget);
            renderTarget.destroyBuffers();
            this.mc.getMainRenderTarget().bindWrite(true);
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

    @NotNull
    public Screen getScreen() {
        return this.screen;
    }

    @Nullable
    public DynamicTexture getCurrentSnapshot() {
        return this.currentSnapshot;
    }

    @Nullable
    public ResourceLocation getCurrentSnapshotLocation() {
        return this.currentSnapshotLocation;
    }

}
