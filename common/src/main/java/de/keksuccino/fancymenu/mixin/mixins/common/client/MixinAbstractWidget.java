package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.math.Axis;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.keksuccino.fancymenu.events.widget.RenderWidgetEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(value = AbstractWidget.class)
public abstract class MixinAbstractWidget implements CustomizableWidget, UniqueWidget {

	@Shadow protected float alpha;
	@Shadow public boolean visible;
	@Shadow public boolean active;
	@Shadow protected boolean isHovered;
	@Shadow protected int height;
	@Shadow protected int width;

	@Unique @Nullable
	private String widgetIdentifierFancyMenu;
	@Unique @Nullable
	private Component customLabelFancyMenu;
	@Unique @Nullable
	private Component hoverLabelFancyMenu;
	@Unique @Nullable
	private IAudio customClickSoundFancyMenu;
	@Unique @Nullable
	private IAudio hoverSoundFancyMenu;
	@Unique @Nullable
	private IAudio unhoverSoundFancyMenu;
	@Unique
	private boolean hiddenFancyMenu = false;
	@Unique
	private boolean underlineLabelOnHoverFancyMenu = false;
	@Unique @Nullable
	private DrawableColor labelHoverColorFancyMenu;
	@Unique @Nullable
	private DrawableColor labelBaseColorFancyMenu;
	@Unique
	private float labelScaleFancyMenu = 1.0F;
	@Unique
	private boolean labelShadowFancyMenu = true;
	@Unique @Nullable
	private RenderableResource customBackgroundNormalFancyMenu;
	@Unique @Nullable
	private RenderableResource customBackgroundHoverFancyMenu;
	@Unique @Nullable
	private RenderableResource customBackgroundInactiveFancyMenu;
	@Unique @NotNull
	private CustomBackgroundResetBehavior customBackgroundResetBehaviorFancyMenu = CustomBackgroundResetBehavior.RESET_NEVER;
	@Unique
	private boolean nineSliceCustomBackgroundTexture_FancyMenu = false;
	@Unique
	private int nineSliceCustomBackgroundBorderX_FancyMenu = 5;
	@Unique
	private int nineSliceCustomBackgroundBorderY_FancyMenu = 5;
	@Unique @Nullable
	private Integer customWidthFancyMenu;
	@Unique @Nullable
	private Integer customHeightFancyMenu;
	@Unique @Nullable
	private Integer customXFancyMenu;
	@Unique @Nullable
	private Integer customYFancyMenu;
	@Unique
	private float hitboxRotationDegrees_FancyMenu = 0.0F;
	@Unique
	private float hitboxVerticalTiltDegrees_FancyMenu = 0.0F;
	@Unique
	private float hitboxHorizontalTiltDegrees_FancyMenu = 0.0F;
	@Unique
	private boolean hitboxRotationActive_FancyMenu = false;
	@Unique
	private float hitboxInverseRotation00_FancyMenu = 1.0F;
	@Unique
	private float hitboxInverseRotation01_FancyMenu = 0.0F;
	@Unique
	private float hitboxInverseRotation10_FancyMenu = 0.0F;
	@Unique
	private float hitboxInverseRotation11_FancyMenu = 1.0F;
	@Unique @Nullable
	private Integer cachedOriginalWidthFancyMenu;
	@Unique @Nullable
	private Integer cachedOriginalHeightFancyMenu;
	@Unique
	private final List<Consumer<Boolean>> hoverStateListenersFancyMenu = new ArrayList<>();
	@Unique
	private final List<Consumer<Boolean>> focusStateListenersFancyMenu = new ArrayList<>();
	@Unique
	private final List<Consumer<Boolean>> hoverOrFocusStateListenersFancyMenu = new ArrayList<>();
	@Unique
	private boolean lastHoverStateFancyMenu = false;
	@Unique
	private boolean lastFocusStateFancyMenu = false;
	@Unique
	private boolean lastHoverOrFocusStateFancyMenu = false;
	@Unique
	private boolean widgetInitializedFancyMenu = false;
	@Unique
	private final List<Runnable> resetCustomizationsListenersFancyMenu = new ArrayList<>();
	
	@Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
	private void beforeRenderFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {

		if (!this.widgetInitializedFancyMenu) this.initWidgetFancyMenu();
		this.widgetInitializedFancyMenu = true;

		//Manually update isHovered before AbstractWidget, to correctly notify hover listeners
		this.isHovered = graphics.containsPointInScissor(mouseX, mouseY) && this.isMouseOverFancyMenu_FancyMenu(mouseX, mouseY);

		if ((this.customWidthFancyMenu != null) && (this.customWidthFancyMenu > 0)) {
			if (this.cachedOriginalWidthFancyMenu == null) this.cachedOriginalWidthFancyMenu = this.width;
			this.width = this.customWidthFancyMenu;
		}
		if ((this.customHeightFancyMenu != null) && (this.customHeightFancyMenu > 0)) {
			if (this.cachedOriginalHeightFancyMenu == null) this.cachedOriginalHeightFancyMenu = this.height;
			this.height = this.customHeightFancyMenu;
		}

		//Handle Hidden State
		if (this.hiddenFancyMenu) {
			this.isHovered = false;
			this.setFocused(false);
			info.cancel();
		}

		this.tickHoverStateListenersFancyMenu(this.isHovered);
		this.tickFocusStateListenersFancyMenu(this.isFocused());
		this.tickHoverOrFocusStateListenersFancyMenu(this.isHoveredOrFocused());

		if (this.hiddenFancyMenu) return;

		//Fire RenderWidgetEvent.Pre
		try {
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(graphics, this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.BEFORE))
	private void before_renderWidget_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		this.isHovered = graphics.containsPointInScissor(mouseX, mouseY) && this.isMouseOverFancyMenu_FancyMenu(mouseX, mouseY);
	}

	@Inject(method = "render", at = @At(value = "RETURN"))
	private void afterRenderFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {

		if (this.hiddenFancyMenu) return;

		try {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(graphics, this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Inject(method = "playDownSound", at = @At(value = "HEAD"), cancellable = true)
	private void before_playDownSound_FancyMenu(SoundManager manager, CallbackInfo info) {
		if (this.customClickSoundFancyMenu != null) {
			this.customClickSoundFancyMenu.stop();
			this.customClickSoundFancyMenu.play();
			info.cancel();
		}
	}

	@Inject(method = "getMessage", at = @At("RETURN"), cancellable = true)
	private void onGetMessageFancyMenu(CallbackInfoReturnable<Component> info) {
		AbstractWidget w = this.getWidgetFancyMenu();
		Component result = info.getReturnValue();
		boolean hovered = w.isHoveredOrFocused() && w.visible && w.active;
		if (hovered && (this.hoverLabelFancyMenu != null)) {
			result = this.hoverLabelFancyMenu;
		} else if (this.customLabelFancyMenu != null) {
			result = this.customLabelFancyMenu;
		}
		if ((result != null) && hovered) {
			boolean underline = this.underlineLabelOnHoverFancyMenu;
			DrawableColor hoverColor = this.labelHoverColorFancyMenu;
			DrawableColor baseColor = this.labelBaseColorFancyMenu;
			DrawableColor appliedColor = (hoverColor != null) ? hoverColor : baseColor;
			if (underline || (appliedColor != null)) {
				int appliedColorRgb = (appliedColor != null) ? (appliedColor.getColorInt() & 0xFFFFFF) : 0;
				result = result.copy().withStyle(style -> {
					var updated = style;
					if (underline) updated = updated.withUnderlined(true);
					if (appliedColor != null) updated = updated.withColor(appliedColorRgb);
					return updated;
				});
			}
		} else if (result != null) {
			DrawableColor baseColor = this.labelBaseColorFancyMenu;
			if (baseColor != null) {
				int baseColorRgb = baseColor.getColorInt() & 0xFFFFFF;
				result = result.copy().withStyle(style -> style.withColor(baseColorRgb));
			}
		}
		info.setReturnValue(result);
	}

	@Inject(method = "renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;II)V", at = @At("HEAD"), cancellable = true)
	private void before_renderScrollingString_FancyMenu(GuiGraphics graphics, Font font, int width, int color, CallbackInfo info) {
		float scale = this.resolveLabelScaleFancyMenu();
		boolean labelShadow = this.labelShadowFancyMenu;
		if (scale == 1.0F && labelShadow) return;
		if (scale == 0.0F) {
			info.cancel();
			return;
		}
		AbstractWidget w = this.getWidgetFancyMenu();
		Component text = w.getMessage();
		int xMin = w.getX() + width;
		int xMax = w.getX() + w.getWidth() - width;
		int yMin = w.getY();
		int yMax = w.getY() + w.getHeight();
		if (scale == 1.0F) {
			int textWidth = font.width(text);
			int textPosY = (yMin + yMax - 9) / 2 + 1;
			int maxTextWidth = xMax - xMin;
			if (textWidth > maxTextWidth) {
				int diffTextWidth = textWidth - maxTextWidth;
				double scrollTime = (double) Util.getMillis() / 1000.0D;
				double scrollDuration = Math.max((double) diffTextWidth * 0.5D, 3.0D);
				double scrollAlpha = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / scrollDuration)) / 2.0D + 0.5D;
				double textOffset = Mth.lerp(scrollAlpha, 0.0D, (double) diffTextWidth);
				graphics.enableScissor(xMin, yMin, xMax, yMax);
				graphics.drawString(font, text, xMin - (int) textOffset, textPosY, color, labelShadow);
				graphics.disableScissor();
			} else {
				int textPosX = Mth.clamp((xMin + xMax) / 2, xMin + textWidth / 2, xMax - textWidth / 2);
				graphics.drawString(font, text, textPosX - (textWidth / 2), textPosY, color, labelShadow);
			}
			info.cancel();
			return;
		}
		float invScale = 1.0F / scale;
		float scaledMinX = xMin * invScale;
		float scaledMaxX = xMax * invScale;
		float scaledMinY = yMin * invScale;
		float scaledMaxY = yMax * invScale;
		int textWidth = font.width(text);
		float textPosY = (scaledMinY + scaledMaxY - font.lineHeight) / 2F + 1F;
		float maxTextWidth = scaledMaxX - scaledMinX;

		graphics.pose().pushPose();
		graphics.pose().scale(scale, scale, 1.0F);
		if (textWidth > maxTextWidth) {
			float diffTextWidth = textWidth - maxTextWidth;
			double scrollTime = (double) Util.getMillis() / 1000.0D;
			double scrollDuration = Math.max(diffTextWidth * 0.5D, 3.0D);
			double scrollAlpha = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / scrollDuration)) / 2.0D + 0.5D;
			double textOffset = Mth.lerp(scrollAlpha, 0.0D, diffTextWidth);
			graphics.enableScissor(xMin, yMin, xMax, yMax);
			graphics.drawString(font, text, (int)(scaledMinX - (float)textOffset), (int)textPosY, color, labelShadow);
			graphics.disableScissor();
		} else {
			float textPosX = ((scaledMinX + scaledMaxX) / 2F) - (textWidth / 2F);
			graphics.drawString(font, text, (int)textPosX, (int)textPosY, color, labelShadow);
		}
		graphics.pose().popPose();
		info.cancel();
	}

	@Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
	private void beforeIsMouseOverFancyMenu(double $$0, double $$1, CallbackInfoReturnable<Boolean> info) {
		if (this.hiddenFancyMenu) {
			info.setReturnValue(false);
			return;
		}
		if (this.hitboxRotationActive_FancyMenu) {
			info.setReturnValue(this.isMouseOverRotated_FancyMenu($$0, $$1));
		}
	}

	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void before_clicked_FancyMenu(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> info) {
		if (this.hiddenFancyMenu) {
			info.setReturnValue(false);
			return;
		}
		if (this.hitboxRotationActive_FancyMenu) {
			info.setReturnValue(this.active && this.visible && this.isMouseOverRotated_FancyMenu(mouseX, mouseY));
		}
	}

	@Inject(method = "isValidClickButton", at = @At("HEAD"), cancellable = true)
	private void beforeIsValidClickButtonFancyMenu(int $$0, CallbackInfoReturnable<Boolean> info) {
		if (this.hiddenFancyMenu) info.setReturnValue(false);
	}

	@Inject(method = "nextFocusPath", at = @At("HEAD"), cancellable = true)
	private void beforeNextFocusPathFancyMenu(FocusNavigationEvent $$0, CallbackInfoReturnable<ComponentPath> info) {
		if (this.hiddenFancyMenu) info.setReturnValue(null);
	}

	@Inject(method = "getX", at = @At("RETURN"), cancellable = true)
	private void atReturnGetXFancyMenu(CallbackInfoReturnable<Integer> info) {
		if (this.customXFancyMenu != null) {
			info.setReturnValue(this.customXFancyMenu);
		}
	}

	@Inject(method = "getY", at = @At("RETURN"), cancellable = true)
	private void atReturnGetYFancyMenu(CallbackInfoReturnable<Integer> info) {
		if (this.customYFancyMenu != null) {
			info.setReturnValue(this.customYFancyMenu);
		}
	}

	@Inject(method = "getWidth", at = @At("RETURN"), cancellable = true)
	private void atReturnGetWidthFancyMenu(CallbackInfoReturnable<Integer> info) {
		if (this.customWidthFancyMenu != null) {
			if (this.customWidthFancyMenu > 0) info.setReturnValue(this.customWidthFancyMenu);
		}
	}

	@Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
	private void atReturnGetHeightFancyMenu(CallbackInfoReturnable<Integer> info) {
		if (this.customHeightFancyMenu != null) {
			if (this.customHeightFancyMenu > 0) info.setReturnValue(this.customHeightFancyMenu);
		}
	}

	@Shadow public abstract void setFocused(boolean bl);

	@Shadow public abstract boolean isFocused();

	@Shadow public abstract boolean isHoveredOrFocused();

	@Shadow public abstract int getX();

	@Shadow public abstract int getY();

	@Shadow public abstract int getWidth();

	@Shadow public abstract int getHeight();

	@Unique
	private void initWidgetFancyMenu() {

		this.addHoverOrFocusStateListenerFancyMenu(hoveredOrFocused -> {
			if (hoveredOrFocused && !this.hiddenFancyMenu && this.visible && this.active) {
				this.handleHoverSoundFancyMenu();
			}
		});

		this.addHoverOrFocusStateListenerFancyMenu(hoveredOrFocused -> {
			if (!hoveredOrFocused && !this.hiddenFancyMenu && this.visible && this.active) {
				this.handleUnhoverSoundFancyMenu();
			}
		});

		this.addHoverOrFocusStateListenerFancyMenu(hoveredOrFocused -> {
			CustomBackgroundResetBehavior behavior = this.getCustomBackgroundResetBehaviorFancyMenu();
			if (hoveredOrFocused && ((behavior == CustomBackgroundResetBehavior.RESET_ON_HOVER) || (behavior == CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER))) {
				if (this.getCustomBackgroundNormalFancyMenu() instanceof PlayableResource p) p.stop();
				if (this.getCustomBackgroundHoverFancyMenu() instanceof PlayableResource p) p.stop();
				if (this.getCustomBackgroundInactiveFancyMenu() instanceof PlayableResource p) p.stop();
			}
			if (!hoveredOrFocused && ((behavior == CustomBackgroundResetBehavior.RESET_ON_UNHOVER) || (behavior == CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER))) {
				if (this.getCustomBackgroundNormalFancyMenu() instanceof PlayableResource p) p.stop();
				if (this.getCustomBackgroundHoverFancyMenu() instanceof PlayableResource p) p.stop();
				if (this.getCustomBackgroundInactiveFancyMenu() instanceof PlayableResource p) p.stop();
			}
		});

	}

	@Unique
	private void handleHoverSoundFancyMenu() {
		if (this.hoverSoundFancyMenu != null) {
			this.hoverSoundFancyMenu.stop();
			this.hoverSoundFancyMenu.play();
		}
	}

	@Unique
	private void handleUnhoverSoundFancyMenu() {
		if (this.unhoverSoundFancyMenu != null) {
			this.unhoverSoundFancyMenu.stop();
			this.unhoverSoundFancyMenu.play();
		}
	}

	@Unique
	private boolean isMouseOverFancyMenu_FancyMenu(double mouseX, double mouseY) {
		if (this.hitboxRotationActive_FancyMenu) {
			return this.isMouseOverRotated_FancyMenu(mouseX, mouseY);
		}
		int width = this.getWidth();
		int height = this.getHeight();
		if (width <= 0 || height <= 0) return false;
		int x = this.getX();
		int y = this.getY();
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	@Unique
	private boolean isMouseOverRotated_FancyMenu(double mouseX, double mouseY) {
		int width = this.getWidth();
		int height = this.getHeight();
		if (width <= 0 || height <= 0) return false;
		float centerX = this.getX() + (width / 2.0F);
		float centerY = this.getY() + (height / 2.0F);
		float dx = (float) mouseX - centerX;
		float dy = (float) mouseY - centerY;
		float localX = (this.hitboxInverseRotation00_FancyMenu * dx) + (this.hitboxInverseRotation01_FancyMenu * dy);
		float localY = (this.hitboxInverseRotation10_FancyMenu * dx) + (this.hitboxInverseRotation11_FancyMenu * dy);
		float halfWidth = width / 2.0F;
		float halfHeight = height / 2.0F;
		return localX >= -halfWidth && localX < halfWidth && localY >= -halfHeight && localY < halfHeight;
	}

	@SuppressWarnings("all")
	@Unique
	private AbstractWidget getWidgetFancyMenu() {
		return (AbstractWidget)((Object)this);
	}

	@Override
	public void addResetCustomizationsListenerFancyMenu(@NotNull Runnable listener) {
		this.resetCustomizationsListenersFancyMenu.add(listener);
	}

	@Override
	public @NotNull List<Runnable> getResetCustomizationsListenersFancyMenu() {
		return this.resetCustomizationsListenersFancyMenu;
	}

	@Unique
	@Override
	public void addHoverStateListenerFancyMenu(@NotNull Consumer<Boolean> listener) {
		this.hoverStateListenersFancyMenu.add(listener);
	}

	@Unique
	@Override
	public void addFocusStateListenerFancyMenu(@NotNull Consumer<Boolean> listener) {
		this.focusStateListenersFancyMenu.add(listener);
	}

	@Unique
	@Override
	public void addHoverOrFocusStateListenerFancyMenu(@NotNull Consumer<Boolean> listener) {
		this.hoverOrFocusStateListenersFancyMenu.add(listener);
	}

	@Unique
	@NotNull
	@Override
	public List<Consumer<Boolean>> getHoverStateListenersFancyMenu() {
		return this.hoverStateListenersFancyMenu;
	}

	@Unique
	@NotNull
	@Override
	public List<Consumer<Boolean>> getFocusStateListenersFancyMenu() {
		return this.focusStateListenersFancyMenu;
	}

	@NotNull
	@Override
	public List<Consumer<Boolean>> getHoverOrFocusStateListenersFancyMenu() {
		return this.hoverOrFocusStateListenersFancyMenu;
	}

	@Unique
	@Override
	public boolean getLastHoverStateFancyMenu() {
		return this.lastHoverStateFancyMenu;
	}

	@Unique
	@Override
	public void setLastHoverStateFancyMenu(boolean hovered) {
		this.lastHoverStateFancyMenu = hovered;
	}

	@Unique
	@Override
	public boolean getLastFocusStateFancyMenu() {
		return this.lastFocusStateFancyMenu;
	}

	@Unique
	@Override
	public void setLastFocusStateFancyMenu(boolean focused) {
		this.lastFocusStateFancyMenu = focused;
	}

	@Override
	public boolean getLastHoverOrFocusStateFancyMenu() {
		return this.lastHoverOrFocusStateFancyMenu;
	}

	@Override
	public void setLastHoverOrFocusStateFancyMenu(boolean hoveredOrFocused) {
		this.lastHoverOrFocusStateFancyMenu = hoveredOrFocused;
	}

	@Unique
	@Override
	public void resetWidgetCustomizationsFancyMenu() {
		if (this.getCustomBackgroundNormalFancyMenu() instanceof PlayableResource p) p.stop();
		if (this.getCustomBackgroundHoverFancyMenu() instanceof PlayableResource p) p.stop();
		if (this.getCustomBackgroundInactiveFancyMenu() instanceof PlayableResource p) p.stop();
		this.setCustomBackgroundNormalFancyMenu(null);
		this.setCustomBackgroundHoverFancyMenu(null);
		this.setCustomBackgroundInactiveFancyMenu(null);
		this.setCustomBackgroundResetBehaviorFancyMenu(CustomBackgroundResetBehavior.RESET_NEVER);
		this.setHoverSoundFancyMenu(null);
		this.setUnhoverSoundFancyMenu(null);
		this.setCustomClickSoundFancyMenu(null);
		this.setHiddenFancyMenu(false);
		this.setCustomLabelFancyMenu(null);
		this.setHoverLabelFancyMenu(null);
		this.setUnderlineLabelOnHoverFancyMenu(false);
		this.setLabelShadowFancyMenu(true);
		this.setLabelHoverColorFancyMenu(null);
		this.setLabelBaseColorFancyMenu(null);
		this.setLabelScaleFancyMenu(1.0F);
		this.setCustomWidthFancyMenu(null);
		this.setCustomHeightFancyMenu(null);
		this.setCustomXFancyMenu(null);
		this.setCustomYFancyMenu(null);
		this.setHitboxRotationFancyMenu(0.0F, 0.0F, 0.0F);
		this.tickHoverStateListenersFancyMenu(false);
		this.tickFocusStateListenersFancyMenu(false);
		this.tickHoverOrFocusStateListenersFancyMenu(false);
		for (Runnable listener : this.getResetCustomizationsListenersFancyMenu()) {
			listener.run();
		}
		if (this.cachedOriginalWidthFancyMenu != null) this.width = this.cachedOriginalWidthFancyMenu;
		if (this.cachedOriginalHeightFancyMenu != null) this.height = this.cachedOriginalHeightFancyMenu;
		this.cachedOriginalWidthFancyMenu = null;
		this.cachedOriginalHeightFancyMenu = null;
	}

	@Unique
	@Override
	public void resetWidgetSizeAndPositionFancyMenu() {
		this.setCustomXFancyMenu(null);
		this.setCustomYFancyMenu(null);
		this.setCustomWidthFancyMenu(null);
		this.setCustomHeightFancyMenu(null);
		if (this.cachedOriginalWidthFancyMenu != null) this.width = this.cachedOriginalWidthFancyMenu;
		if (this.cachedOriginalHeightFancyMenu != null) this.height = this.cachedOriginalHeightFancyMenu;
		this.cachedOriginalWidthFancyMenu = null;
		this.cachedOriginalHeightFancyMenu = null;
		this.customWidthFancyMenu = null;
		this.customHeightFancyMenu = null;
		this.customXFancyMenu = null;
		this.customYFancyMenu = null;
	}

	@Unique
	@Override
	public void setCustomLabelFancyMenu(@Nullable Component customLabelFancyMenu) {
		this.customLabelFancyMenu = customLabelFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public Component getCustomLabelFancyMenu() {
		return this.customLabelFancyMenu;
	}

	@Unique
	@Override
	public void setHoverLabelFancyMenu(@Nullable Component hoverLabelFancyMenu) {
		this.hoverLabelFancyMenu = hoverLabelFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public Component getHoverLabelFancyMenu() {
		return this.hoverLabelFancyMenu;
	}

	@Unique
	@Override
	public void setUnderlineLabelOnHoverFancyMenu(boolean underline) {
		this.underlineLabelOnHoverFancyMenu = underline;
	}

	@Unique
	@Override
	public boolean isUnderlineLabelOnHoverFancyMenu() {
		return this.underlineLabelOnHoverFancyMenu;
	}

	@Unique
	@Override
	public void setLabelShadowFancyMenu(boolean shadow) {
		this.labelShadowFancyMenu = shadow;
	}

	@Unique
	@Override
	public boolean isLabelShadowFancyMenu() {
		return this.labelShadowFancyMenu;
	}

	@Unique
	@Override
	public void setLabelHoverColorFancyMenu(@Nullable DrawableColor color) {
		this.labelHoverColorFancyMenu = color;
	}

	@Unique
	@Override
	public @Nullable DrawableColor getLabelHoverColorFancyMenu() {
		return this.labelHoverColorFancyMenu;
	}

	@Unique
	@Override
	public void setLabelBaseColorFancyMenu(@Nullable DrawableColor color) {
		this.labelBaseColorFancyMenu = color;
	}

	@Unique
	@Override
	public @Nullable DrawableColor getLabelBaseColorFancyMenu() {
		return this.labelBaseColorFancyMenu;
	}

	@Unique
	@Override
	public void setLabelScaleFancyMenu(float scale) {
		this.labelScaleFancyMenu = scale;
	}

	@Unique
	@Override
	public float getLabelScaleFancyMenu() {
		return this.labelScaleFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public IAudio getCustomClickSoundFancyMenu() {
		return this.customClickSoundFancyMenu;
	}

	@Unique
	@Override
	public void setCustomClickSoundFancyMenu(@Nullable IAudio sound) {
		this.customClickSoundFancyMenu = sound;
	}

	@Unique
	@Nullable
	@Override
	public IAudio getHoverSoundFancyMenu() {
		return this.hoverSoundFancyMenu;
	}

	@Unique
	@Override
	public void setHoverSoundFancyMenu(@Nullable IAudio sound) {
		this.hoverSoundFancyMenu = sound;
	}

	@Unique
	@Nullable
	@Override
	public IAudio getUnhoverSoundFancyMenu() {
		return this.unhoverSoundFancyMenu;
	}

	@Unique
	@Override
	public void setUnhoverSoundFancyMenu(@Nullable IAudio sound) {
		this.unhoverSoundFancyMenu = sound;
	}

	@Unique
	@Override
	public boolean isHiddenFancyMenu() {
		return this.hiddenFancyMenu;
	}

	@Unique
	@Override
	public void setCustomBackgroundNormalFancyMenu(@Nullable RenderableResource background) {
		this.customBackgroundNormalFancyMenu = background;
	}

	@Unique
	@Override
	public @Nullable RenderableResource getCustomBackgroundNormalFancyMenu() {
		if (this.customBackgroundNormalFancyMenu != null) return this.customBackgroundNormalFancyMenu;
		return this.getGlobalBackgroundNormal_FancyMenu();
	}

	@Unique
	@Override
	public void setCustomBackgroundHoverFancyMenu(@Nullable RenderableResource background) {
		this.customBackgroundHoverFancyMenu = background;
	}

	@Unique
	@Override
	public @Nullable RenderableResource getCustomBackgroundHoverFancyMenu() {
		if (this.customBackgroundHoverFancyMenu != null) return this.customBackgroundHoverFancyMenu;
		return this.getGlobalBackgroundHover_FancyMenu();
	}

	@Unique
	@Override
	public void setNineSliceCustomBackground_FancyMenu(boolean repeat) {
		this.nineSliceCustomBackgroundTexture_FancyMenu = repeat;
	}

	@Unique
	@Override
	public boolean isNineSliceCustomBackgroundTexture_FancyMenu() {
		return nineSliceCustomBackgroundTexture_FancyMenu;
	}

	@Unique
	@Override
	public void setNineSliceBorderX_FancyMenu(int borderX) {
		this.nineSliceCustomBackgroundBorderX_FancyMenu = borderX;
	}

	@Unique
	@Override
	public int getNineSliceCustomBackgroundBorderX_FancyMenu() {
		return nineSliceCustomBackgroundBorderX_FancyMenu;
	}

	@Unique
	@Override
	public void setNineSliceBorderY_FancyMenu(int borderY) {
		this.nineSliceCustomBackgroundBorderY_FancyMenu = borderY;
	}

	@Unique
	@Override
	public int getNineSliceCustomBackgroundBorderY_FancyMenu() {
		return nineSliceCustomBackgroundBorderY_FancyMenu;
	}

	@Unique
	@Override
	public void setCustomBackgroundResetBehaviorFancyMenu(@NotNull CustomBackgroundResetBehavior resetBehavior) {
		this.customBackgroundResetBehaviorFancyMenu = Objects.requireNonNull(resetBehavior);
	}

	@Unique
	@Override
	public @NotNull CustomBackgroundResetBehavior getCustomBackgroundResetBehaviorFancyMenu() {
		return this.customBackgroundResetBehaviorFancyMenu;
	}

	@Unique
	@Override
	public void setCustomBackgroundInactiveFancyMenu(@Nullable RenderableResource background) {
		this.customBackgroundInactiveFancyMenu = background;
	}

	@Unique
	@Override
	public @Nullable RenderableResource getCustomBackgroundInactiveFancyMenu() {
		if (this.customBackgroundInactiveFancyMenu != null) return this.customBackgroundInactiveFancyMenu;
		return this.getGlobalBackgroundInactive_FancyMenu();
	}

	@Unique
	@Nullable
	private RenderableResource getGlobalBackgroundNormal_FancyMenu() {
		if (this.hasFancyMenuColorBackgroundNormal_FancyMenu()) return null;
		if ((Object)this instanceof ImageButton) return null;
		if ((Object)this instanceof AbstractSliderButton) {
			return GlobalCustomizationHandler.getCustomSliderHandleNormal();
		}
		return GlobalCustomizationHandler.getCustomButtonBackgroundNormal();
	}

	@Unique
	@Nullable
	private RenderableResource getGlobalBackgroundHover_FancyMenu() {
		if (this.hasFancyMenuColorBackgroundHover_FancyMenu()) return null;
		if ((Object)this instanceof ImageButton) return null;
		if ((Object)this instanceof AbstractSliderButton) {
			return GlobalCustomizationHandler.getCustomSliderHandleHover();
		}
		return GlobalCustomizationHandler.getCustomButtonBackgroundHover();
	}

	@Unique
	@Nullable
	private RenderableResource getGlobalBackgroundInactive_FancyMenu() {
		if (this.hasFancyMenuColorBackgroundInactive_FancyMenu()) return null;
		if ((Object)this instanceof ImageButton) return null;
		if ((Object)this instanceof AbstractSliderButton) {
			return GlobalCustomizationHandler.getCustomSliderHandleInactive();
		}
		return GlobalCustomizationHandler.getCustomButtonBackgroundInactive();
	}

	@Unique
	private boolean hasFancyMenuColorBackgroundNormal_FancyMenu() {
		if ((Object)this instanceof ExtendedButton button) {
			return button.getBackgroundColorNormal() != null;
		}
		if ((Object)this instanceof AbstractExtendedSlider slider) {
			return slider.getSliderHandleColorNormal() != null;
		}
		return false;
	}

	@Unique
	private boolean hasFancyMenuColorBackgroundHover_FancyMenu() {
		if ((Object)this instanceof ExtendedButton button) {
			return button.getBackgroundColorHover() != null;
		}
		if ((Object)this instanceof AbstractExtendedSlider slider) {
			return slider.getSliderHandleColorHover() != null;
		}
		return false;
	}

	@Unique
	private boolean hasFancyMenuColorBackgroundInactive_FancyMenu() {
		if ((Object)this instanceof ExtendedButton button) {
			return button.getBackgroundColorInactive() != null;
		}
		if ((Object)this instanceof AbstractExtendedSlider slider) {
			return slider.getSliderHandleColorInactive() != null;
		}
		return false;
	}

	@Unique
	@Override
	public void setHiddenFancyMenu(boolean hiddenFancyMenu) {
		this.hiddenFancyMenu = hiddenFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public Integer getCustomWidthFancyMenu() {
		return this.customWidthFancyMenu;
	}

	@Unique
	@Override
	public void setCustomWidthFancyMenu(@Nullable Integer customWidthFancyMenu) {
		this.customWidthFancyMenu = customWidthFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public Integer getCustomHeightFancyMenu() {
		return this.customHeightFancyMenu;
	}

	@Unique
	@Override
	public void setCustomHeightFancyMenu(@Nullable Integer customHeightFancyMenu) {
		this.customHeightFancyMenu = customHeightFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public Integer getCustomXFancyMenu() {
		return this.customXFancyMenu;
	}

	@Unique
	@Override
	public void setCustomXFancyMenu(@Nullable Integer customXFancyMenu) {
		this.customXFancyMenu = customXFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public Integer getCustomYFancyMenu() {
		return this.customYFancyMenu;
	}

	@Unique
	@Override
	public void setCustomYFancyMenu(@Nullable Integer customYFancyMenu) {
		this.customYFancyMenu = customYFancyMenu;
	}

	@Unique
	@Override
	public void setHitboxRotationFancyMenu(float rotationDegrees, float verticalTiltDegrees, float horizontalTiltDegrees) {
		this.hitboxRotationDegrees_FancyMenu = rotationDegrees;
		this.hitboxVerticalTiltDegrees_FancyMenu = verticalTiltDegrees;
		this.hitboxHorizontalTiltDegrees_FancyMenu = horizontalTiltDegrees;
		this.updateHitboxRotationMatrix_FancyMenu();
	}

	@Unique
	@Override
	public float getHitboxRotationDegreesFancyMenu() {
		return this.hitboxRotationDegrees_FancyMenu;
	}

	@Unique
	@Override
	public float getHitboxVerticalTiltDegreesFancyMenu() {
		return this.hitboxVerticalTiltDegrees_FancyMenu;
	}

	@Unique
	@Override
	public float getHitboxHorizontalTiltDegreesFancyMenu() {
		return this.hitboxHorizontalTiltDegrees_FancyMenu;
	}

	@Unique
	private void updateHitboxRotationMatrix_FancyMenu() {
		float rotation = this.hitboxRotationDegrees_FancyMenu;
		float verticalTilt = this.hitboxVerticalTiltDegrees_FancyMenu;
		float horizontalTilt = this.hitboxHorizontalTiltDegrees_FancyMenu;
		if ((rotation == 0.0F) && (verticalTilt == 0.0F) && (horizontalTilt == 0.0F)) {
			this.hitboxRotationActive_FancyMenu = false;
			this.hitboxInverseRotation00_FancyMenu = 1.0F;
			this.hitboxInverseRotation01_FancyMenu = 0.0F;
			this.hitboxInverseRotation10_FancyMenu = 0.0F;
			this.hitboxInverseRotation11_FancyMenu = 1.0F;
			return;
		}
		RenderRotationUtil.RotationState state = new RenderRotationUtil.RotationState();
		if (verticalTilt != 0.0F) state.mul(Axis.XP.rotationDegrees(verticalTilt));
		if (horizontalTilt != 0.0F) state.mul(Axis.YP.rotationDegrees(horizontalTilt));
		if (rotation != 0.0F) state.mul(Axis.ZP.rotationDegrees(rotation));

		float x = state.x;
		float y = state.y;
		float z = state.z;
		float w = state.w;

		float m00 = 1.0F - 2.0F * y * y - 2.0F * z * z;
		float m01 = 2.0F * x * y - 2.0F * z * w;
		float m10 = 2.0F * x * y + 2.0F * z * w;
		float m11 = 1.0F - 2.0F * x * x - 2.0F * z * z;

		float det = m00 * m11 - m01 * m10;
		if (!Float.isFinite(det) || Math.abs(det) < 1.0E-6F) {
			this.hitboxRotationActive_FancyMenu = false;
			this.hitboxInverseRotation00_FancyMenu = 1.0F;
			this.hitboxInverseRotation01_FancyMenu = 0.0F;
			this.hitboxInverseRotation10_FancyMenu = 0.0F;
			this.hitboxInverseRotation11_FancyMenu = 1.0F;
			return;
		}
		float invDet = 1.0F / det;
		this.hitboxRotationActive_FancyMenu = true;
		this.hitboxInverseRotation00_FancyMenu = m11 * invDet;
		this.hitboxInverseRotation01_FancyMenu = -m01 * invDet;
		this.hitboxInverseRotation10_FancyMenu = -m10 * invDet;
		this.hitboxInverseRotation11_FancyMenu = m00 * invDet;
	}

	@SuppressWarnings("all")
	@Unique
	@Override
	public AbstractWidget setWidgetIdentifierFancyMenu(@Nullable String identifier) {
		this.widgetIdentifierFancyMenu = identifier;
		return (AbstractWidget)((Object)this);
	}

	@Unique
	@Override
	public @Nullable String getWidgetIdentifierFancyMenu() {
		return this.widgetIdentifierFancyMenu;
	}

}
