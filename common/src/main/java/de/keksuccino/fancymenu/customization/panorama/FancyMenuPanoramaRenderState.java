package de.keksuccino.fancymenu.customization.panorama;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.Objects;

public record FancyMenuPanoramaRenderState(
	@NotNull LocalTexturePanoramaRenderer panoramaRenderer,
	@NotNull Matrix3x2f pose,
	float pitch,
	float yaw,
	int x0,
	int y0,
	int x1,
	int y1,
	int color,
	@Nullable ScreenRectangle scissorArea,
	@NotNull ScreenRectangle bounds
) implements PictureInPictureRenderState {

	public FancyMenuPanoramaRenderState(
		@NotNull LocalTexturePanoramaRenderer panoramaRenderer,
		@NotNull Matrix3x2f pose,
		float pitch,
		float yaw,
		int x0,
		int y0,
		int x1,
		int y1,
		int color,
		@Nullable ScreenRectangle scissorArea
	) {
		this(
			panoramaRenderer,
			pose,
			pitch,
			yaw,
			x0,
			y0,
			x1,
			y1,
			color,
			scissorArea,
			getBounds(x0, y0, x1, y1, pose, scissorArea)
		);
	}

	@Override
	public float scale() {
		return 1.0F;
	}

	@NotNull
	private static ScreenRectangle getBounds(int x0, int y0, int x1, int y1, @NotNull Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
		ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
		return Objects.requireNonNullElse(scissorArea != null ? scissorArea.intersection(bounds) : bounds, ScreenRectangle.empty());
	}

}
