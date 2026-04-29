package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.gui.Axis;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.VanillaTooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.IExtendedWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.WidgetWithVanillaTooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractButton;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
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
public abstract class MixinAbstractWidget implements CustomizableWidget, UniqueWidget, IExtendedWidget, WidgetWithVanillaTooltip {

	@Shadow @Final public static ResourceLocation WIDGETS_LOCATION;
	@Shadow protected float alpha;
	@Shadow public boolean visible;
	@Shadow public boolean active;
	@Shadow protected boolean isHovered;
	@Shadow protected int height;
	@Shadow protected int width;
	@Shadow public int x;
	@Shadow public int y;

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
	@Unique
	private int nineSliceCustomBackgroundBorderTop_FancyMenu = 5;
	@Unique
	private int nineSliceCustomBackgroundBorderRight_FancyMenu = 5;
	@Unique
	private int nineSliceCustomBackgroundBorderBottom_FancyMenu = 5;
	@Unique
	private int nineSliceCustomBackgroundBorderLeft_FancyMenu = 5;
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
	@Unique @Nullable
	private Integer cachedOriginalXFancyMenu;
	@Unique @Nullable
	private Integer cachedOriginalYFancyMenu;
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
	@Unique @Nullable
	private VanillaTooltip vanillaTooltip_FancyMenu;
	
	@Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
	private void beforeRenderFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {

		if (!this.widgetInitializedFancyMenu) this.initWidgetFancyMenu();
		this.widgetInitializedFancyMenu = true;

		if ((this.customWidthFancyMenu != null) && (this.customWidthFancyMenu > 0)) {
			if (this.cachedOriginalWidthFancyMenu == null) this.cachedOriginalWidthFancyMenu = this.width;
			this.width = this.customWidthFancyMenu;
		}
		if ((this.customHeightFancyMenu != null) && (this.customHeightFancyMenu > 0)) {
			if (this.cachedOriginalHeightFancyMenu == null) this.cachedOriginalHeightFancyMenu = this.height;
			this.height = this.customHeightFancyMenu;
		}
		if (this.customXFancyMenu != null) {
			if (this.cachedOriginalXFancyMenu == null) this.cachedOriginalXFancyMenu = this.x;
			this.x = this.customXFancyMenu;
		}
		if (this.customYFancyMenu != null) {
			if (this.cachedOriginalYFancyMenu == null) this.cachedOriginalYFancyMenu = this.y;
			this.y = this.customYFancyMenu;
		}

		//Manually update isHovered before AbstractWidget, to correctly notify hover listeners
		this.isHovered = this.isMouseOverFancyMenu_FancyMenu(mouseX, mouseY);

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
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(GuiGraphics.currentGraphics(), this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @reason Vanilla updates hovered state inside render with its unrotated rectangle.
	 */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;renderButton(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", shift = At.Shift.BEFORE))
	private void before_renderButton_FancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
		this.isHovered = this.isMouseOverFancyMenu_FancyMenu(mouseX, mouseY);
	}

	@Inject(method = "render", at = @At(value = "RETURN"))
	private void afterRenderFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {

		if (this.hiddenFancyMenu) return;

		try {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(GuiGraphics.currentGraphics(), this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Unique @Nullable
	private Boolean cachedRenderCustomBackgroundFancyMenu;

	@WrapWithCondition(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
	private boolean wrapBlitInRenderButtonFancyMenu(AbstractWidget instance, PoseStack pose, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
		if (this.cachedRenderCustomBackgroundFancyMenu != null) {
			this.cachedRenderCustomBackgroundFancyMenu = null;
			return false;
		}

		AbstractWidget widget = this.getWidgetFancyMenu();
		if ((widget instanceof CustomizableSlider slider) && ((Object)this instanceof AbstractSliderButton abstractSliderButton)) {
			this.cachedRenderCustomBackgroundFancyMenu = slider.renderSliderBackgroundFancyMenu(GuiGraphics.currentGraphics(), abstractSliderButton, true);
			RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
		} else {
			this.cachedRenderCustomBackgroundFancyMenu = this.renderCustomBackgroundFancyMenu(widget, GuiGraphics.currentGraphics(), widget.x, widget.y, widget.getWidth(), widget.getHeight());
		}

		if (this.cachedRenderCustomBackgroundFancyMenu) {
			this.render119VanillaBackgroundFancyMenu();
		}

		return false;
	}

	/**
	 * @reason Backport the modern nine-sliced widget background for 1.19.2's older two-blit button renderer.
	 */
	@Unique
	private void render119VanillaBackgroundFancyMenu() {
		GuiGraphics graphics = GuiGraphics.currentGraphics();
		graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		graphics.blitNineSliced(WIDGETS_LOCATION, this.x, this.y, this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureYFancyMenu());
		RenderingUtils.resetShaderColor(graphics);
	}

	@Unique
	private int getTextureYFancyMenu() {
		boolean slider = (Object)this instanceof AbstractSliderButton;
		int state = 1;
		if (!this.active || slider) {
			state = 0;
		} else if (this.isHoveredOrFocused()) {
			state = 2;
		}
		return 46 + state * 20;
	}

	/**
	 * @reason Backport the 1.19.4+ scrolling label renderer while keeping FancyMenu label customizations active on 1.19.2.
	 */
	@WrapWithCondition(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private boolean wrapLabelRenderingFancyMenu(PoseStack pose, Font font, Component component, int x, int y, int color) {
		this.renderScrollingLabel(this.getWidgetFancyMenu(), GuiGraphics.currentGraphics(), font, 2, this.resolveLabelShadow_FancyMenu(), color);
		return false;
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
		boolean applyGlobalLabel = this.isGlobalLabelCustomizationTarget_FancyMenu();
		if (hovered && (this.hoverLabelFancyMenu != null)) {
			result = this.hoverLabelFancyMenu;
		} else if (this.customLabelFancyMenu != null) {
			result = this.customLabelFancyMenu;
		}
		if ((result != null) && hovered) {
			boolean underline = this.underlineLabelOnHoverFancyMenu;
			DrawableColor hoverColor = this.labelHoverColorFancyMenu;
			DrawableColor baseColor = this.labelBaseColorFancyMenu;
			if (applyGlobalLabel) {
				if (!underline) underline = this.isGlobalUnderlineLabelOnHoverEnabled_FancyMenu();
				if (baseColor == null) baseColor = this.getGlobalLabelBaseColor_FancyMenu();
				if ((hoverColor == null) && (this.labelBaseColorFancyMenu == null)) {
					hoverColor = this.getGlobalLabelHoverColor_FancyMenu();
				}
			}
			DrawableColor appliedColor = (hoverColor != null) ? hoverColor : baseColor;
			if (underline || (appliedColor != null)) {
				final boolean underlineFinal = underline;
				final DrawableColor appliedColorFinal = appliedColor;
				final int appliedColorRgb = (appliedColorFinal != null) ? (appliedColorFinal.getColorInt() & 0xFFFFFF) : 0;
				result = result.copy().withStyle(style -> {
					var updated = style;
					if (underlineFinal) updated = updated.withUnderlined(true);
					if (appliedColorFinal != null) updated = updated.withColor(appliedColorRgb);
					return updated;
				});
			}
		} else if (result != null) {
			DrawableColor baseColor = this.labelBaseColorFancyMenu;
			if (applyGlobalLabel && (baseColor == null)) {
				baseColor = this.getGlobalLabelBaseColor_FancyMenu();
			}
			if (baseColor != null) {
				int baseColorRgb = baseColor.getColorInt() & 0xFFFFFF;
				result = result.copy().withStyle(style -> style.withColor(baseColorRgb));
			}
		}
		info.setReturnValue(result);
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
		int width = this.getEffectiveWidth_FancyMenu();
		int height = this.getEffectiveHeight_FancyMenu();
		if (width <= 0 || height <= 0) return false;
		int x = this.getEffectiveX_FancyMenu();
		int y = this.getEffectiveY_FancyMenu();
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	@Unique
	private boolean isMouseOverRotated_FancyMenu(double mouseX, double mouseY) {
		int width = this.getEffectiveWidth_FancyMenu();
		int height = this.getEffectiveHeight_FancyMenu();
		if (width <= 0 || height <= 0) return false;
		float centerX = this.getEffectiveX_FancyMenu() + (width / 2.0F);
		float centerY = this.getEffectiveY_FancyMenu() + (height / 2.0F);
		float dx = (float) mouseX - centerX;
		float dy = (float) mouseY - centerY;
		float localX = (this.hitboxInverseRotation00_FancyMenu * dx) + (this.hitboxInverseRotation01_FancyMenu * dy);
		float localY = (this.hitboxInverseRotation10_FancyMenu * dx) + (this.hitboxInverseRotation11_FancyMenu * dy);
		float halfWidth = width / 2.0F;
		float halfHeight = height / 2.0F;
		return localX >= -halfWidth && localX < halfWidth && localY >= -halfHeight && localY < halfHeight;
	}

	@Unique
	private int getEffectiveX_FancyMenu() {
		return this.customXFancyMenu != null ? this.customXFancyMenu : this.x;
	}

	@Unique
	private int getEffectiveY_FancyMenu() {
		return this.customYFancyMenu != null ? this.customYFancyMenu : this.y;
	}

	@Unique
	private int getEffectiveWidth_FancyMenu() {
		return (this.customWidthFancyMenu != null && this.customWidthFancyMenu > 0) ? this.customWidthFancyMenu : this.width;
	}

	@Unique
	private int getEffectiveHeight_FancyMenu() {
		return (this.customHeightFancyMenu != null && this.customHeightFancyMenu > 0) ? this.customHeightFancyMenu : this.height;
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
		if (this.cachedOriginalXFancyMenu != null) this.x = this.cachedOriginalXFancyMenu;
		if (this.cachedOriginalYFancyMenu != null) this.y = this.cachedOriginalYFancyMenu;
		this.cachedOriginalXFancyMenu = null;
		this.cachedOriginalYFancyMenu = null;
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
		if (this.cachedOriginalXFancyMenu != null) this.x = this.cachedOriginalXFancyMenu;
		if (this.cachedOriginalYFancyMenu != null) this.y = this.cachedOriginalYFancyMenu;
		this.cachedOriginalXFancyMenu = null;
		this.cachedOriginalYFancyMenu = null;
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
	@Override
	public float resolveLabelScaleFancyMenu() {
		float localScale = this.labelScaleFancyMenu;
		if (this.isGlobalLabelCustomizationTarget_FancyMenu()) {
			float globalScale = this.getGlobalLabelScale_FancyMenu();
			if (Float.compare(localScale, 1.0F) == 0 && Float.compare(globalScale, 1.0F) != 0) {
				return globalScale;
			}
		}
		return localScale;
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
		this.nineSliceCustomBackgroundBorderLeft_FancyMenu = borderX;
		this.nineSliceCustomBackgroundBorderRight_FancyMenu = borderX;
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
		this.nineSliceCustomBackgroundBorderTop_FancyMenu = borderY;
		this.nineSliceCustomBackgroundBorderBottom_FancyMenu = borderY;
	}

	@Unique
	@Override
	public int getNineSliceCustomBackgroundBorderY_FancyMenu() {
		return nineSliceCustomBackgroundBorderY_FancyMenu;
	}

	@Unique
	@Override
	public void setNineSliceBorderTop_FancyMenu(int borderTop) {
		this.nineSliceCustomBackgroundBorderTop_FancyMenu = borderTop;
	}

	@Unique
	@Override
	public int getNineSliceCustomBackgroundBorderTop_FancyMenu() {
		return this.nineSliceCustomBackgroundBorderTop_FancyMenu;
	}

	@Unique
	@Override
	public void setNineSliceBorderRight_FancyMenu(int borderRight) {
		this.nineSliceCustomBackgroundBorderRight_FancyMenu = borderRight;
	}

	@Unique
	@Override
	public int getNineSliceCustomBackgroundBorderRight_FancyMenu() {
		return this.nineSliceCustomBackgroundBorderRight_FancyMenu;
	}

	@Unique
	@Override
	public void setNineSliceBorderBottom_FancyMenu(int borderBottom) {
		this.nineSliceCustomBackgroundBorderBottom_FancyMenu = borderBottom;
	}

	@Unique
	@Override
	public int getNineSliceCustomBackgroundBorderBottom_FancyMenu() {
		return this.nineSliceCustomBackgroundBorderBottom_FancyMenu;
	}

	@Unique
	@Override
	public void setNineSliceBorderLeft_FancyMenu(int borderLeft) {
		this.nineSliceCustomBackgroundBorderLeft_FancyMenu = borderLeft;
	}

	@Unique
	@Override
	public int getNineSliceCustomBackgroundBorderLeft_FancyMenu() {
		return this.nineSliceCustomBackgroundBorderLeft_FancyMenu;
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
			RenderableResource resource = GlobalCustomizationHandler.getCustomSliderHandleNormal();
			if (resource != null) {
				this.applyGlobalSliderHandleNineSlice_FancyMenu();
			}
			return resource;
		}
		RenderableResource resource = GlobalCustomizationHandler.getCustomButtonBackgroundNormal();
		if (resource != null) {
			this.applyGlobalButtonBackgroundNineSlice_FancyMenu();
		}
		return resource;
	}

	@Unique
	@Nullable
	private RenderableResource getGlobalBackgroundHover_FancyMenu() {
		if (this.hasFancyMenuColorBackgroundHover_FancyMenu()) return null;
		if ((Object)this instanceof ImageButton) return null;
		if ((Object)this instanceof AbstractSliderButton) {
			RenderableResource resource = GlobalCustomizationHandler.getCustomSliderHandleHover();
			if (resource != null) {
				this.applyGlobalSliderHandleNineSlice_FancyMenu();
			}
			return resource;
		}
		RenderableResource resource = GlobalCustomizationHandler.getCustomButtonBackgroundHover();
		if (resource != null) {
			this.applyGlobalButtonBackgroundNineSlice_FancyMenu();
		}
		return resource;
	}

	@Unique
	@Nullable
	private RenderableResource getGlobalBackgroundInactive_FancyMenu() {
		if (this.hasFancyMenuColorBackgroundInactive_FancyMenu()) return null;
		if ((Object)this instanceof ImageButton) return null;
		if ((Object)this instanceof AbstractSliderButton) {
			RenderableResource resource = GlobalCustomizationHandler.getCustomSliderHandleInactive();
			if (resource != null) {
				this.applyGlobalSliderHandleNineSlice_FancyMenu();
			}
			return resource;
		}
		RenderableResource resource = GlobalCustomizationHandler.getCustomButtonBackgroundInactive();
		if (resource != null) {
			this.applyGlobalButtonBackgroundNineSlice_FancyMenu();
		}
		return resource;
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
	private void applyGlobalButtonBackgroundNineSlice_FancyMenu() {
		this.setNineSliceCustomBackground_FancyMenu(GlobalCustomizationHandler.isGlobalButtonBackgroundNineSliceEnabled());
		this.setNineSliceBorderTop_FancyMenu(GlobalCustomizationHandler.getGlobalButtonBackgroundNineSliceBorderTop());
		this.setNineSliceBorderRight_FancyMenu(GlobalCustomizationHandler.getGlobalButtonBackgroundNineSliceBorderRight());
		this.setNineSliceBorderBottom_FancyMenu(GlobalCustomizationHandler.getGlobalButtonBackgroundNineSliceBorderBottom());
		this.setNineSliceBorderLeft_FancyMenu(GlobalCustomizationHandler.getGlobalButtonBackgroundNineSliceBorderLeft());
	}

	@Unique
	private void applyGlobalSliderHandleNineSlice_FancyMenu() {
		if (!((Object)this instanceof CustomizableSlider slider)) return;
		slider.setNineSliceCustomSliderHandle_FancyMenu(GlobalCustomizationHandler.isGlobalSliderHandleNineSliceEnabled());
		slider.setNineSliceSliderHandleBorderTop_FancyMenu(GlobalCustomizationHandler.getGlobalSliderHandleNineSliceBorderTop());
		slider.setNineSliceSliderHandleBorderRight_FancyMenu(GlobalCustomizationHandler.getGlobalSliderHandleNineSliceBorderRight());
		slider.setNineSliceSliderHandleBorderBottom_FancyMenu(GlobalCustomizationHandler.getGlobalSliderHandleNineSliceBorderBottom());
		slider.setNineSliceSliderHandleBorderLeft_FancyMenu(GlobalCustomizationHandler.getGlobalSliderHandleNineSliceBorderLeft());
	}

	@Unique
	private boolean isButtonWidget_FancyMenu() {
		return (Object)this instanceof AbstractButton;
	}

	@Unique
	private boolean isSliderWidget_FancyMenu() {
		return (Object)this instanceof AbstractSliderButton;
	}

	@Unique
	private boolean isGlobalLabelCustomizationTarget_FancyMenu() {
		return this.isButtonWidget_FancyMenu() || this.isSliderWidget_FancyMenu();
	}

	@Unique
	private boolean isGlobalUnderlineLabelOnHoverEnabled_FancyMenu() {
		if (this.isSliderWidget_FancyMenu()) {
			return GlobalCustomizationHandler.isGlobalSliderLabelUnderlineOnHoverEnabled();
		}
		if (this.isButtonWidget_FancyMenu()) {
			return GlobalCustomizationHandler.isGlobalButtonLabelUnderlineOnHoverEnabled();
		}
		return false;
	}

	@Unique
	@Nullable
	private DrawableColor getGlobalLabelBaseColor_FancyMenu() {
		if (this.isSliderWidget_FancyMenu()) {
			return GlobalCustomizationHandler.getGlobalSliderLabelBaseColor();
		}
		if (this.isButtonWidget_FancyMenu()) {
			return GlobalCustomizationHandler.getGlobalButtonLabelBaseColor();
		}
		return null;
	}

	@Unique
	@Nullable
	private DrawableColor getGlobalLabelHoverColor_FancyMenu() {
		if (this.isSliderWidget_FancyMenu()) {
			return GlobalCustomizationHandler.getGlobalSliderLabelHoverColor();
		}
		if (this.isButtonWidget_FancyMenu()) {
			return GlobalCustomizationHandler.getGlobalButtonLabelHoverColor();
		}
		return null;
	}

	@Unique
	private float getGlobalLabelScale_FancyMenu() {
		if (this.isSliderWidget_FancyMenu()) {
			return GlobalCustomizationHandler.getGlobalSliderLabelScale();
		}
		if (this.isButtonWidget_FancyMenu()) {
			return GlobalCustomizationHandler.getGlobalButtonLabelScale();
		}
		return 1.0F;
	}

	@Unique
	private boolean resolveLabelShadow_FancyMenu() {
		boolean localShadow = this.labelShadowFancyMenu;
		if (this.isSliderWidget_FancyMenu()) {
			return GlobalCustomizationHandler.isGlobalSliderLabelShadowEnabled() && localShadow;
		}
		if (this.isButtonWidget_FancyMenu()) {
			return GlobalCustomizationHandler.isGlobalButtonLabelShadowEnabled() && localShadow;
		}
		return localShadow;
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

	@Unique
	@Override
	public @Nullable VanillaTooltip getVanillaTooltip_FancyMenu() {
		return this.vanillaTooltip_FancyMenu;
	}

	@Unique
	@Override
	public void setVanillaTooltip_FancyMenu(@Nullable VanillaTooltip tooltip) {
		this.vanillaTooltip_FancyMenu = tooltip;
	}

}
