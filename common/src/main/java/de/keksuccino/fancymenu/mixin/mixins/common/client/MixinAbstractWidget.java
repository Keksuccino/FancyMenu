package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.VanillaTooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.*;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
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

@SuppressWarnings("all")
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
	@Unique
	private boolean hiddenFancyMenu = false;
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

		//Manually update isHovered before AbstractWidget, to correctly notify hover listeners
		this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

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
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(pose, this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Inject(method = "render", at = @At(value = "RETURN"))
	private void afterRenderFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {

		if (this.hiddenFancyMenu) return;

		try {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(pose, this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Unique private Boolean cachedRenderCustomBackgroundFancyMenu = null;

	@WrapWithCondition(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
	private boolean wrapBlitInRenderButtonFancyMenu(AbstractWidget instance, PoseStack pose, int i1, int i2, int i3, int i4, int i5, int i6) {

		if (this.cachedRenderCustomBackgroundFancyMenu != null) {
			this.cachedRenderCustomBackgroundFancyMenu = null;
			return false;
		}

		AbstractWidget button = (AbstractWidget)((Object)this);
		if ((button instanceof CustomizableSlider s) && ((Object)this instanceof AbstractSliderButton as)) {
			this.cachedRenderCustomBackgroundFancyMenu = s.renderSliderBackgroundFancyMenu(GuiGraphics.currentGraphics(), as, true);
			//Re-bind default texture after rendering custom
			RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
		} else {
			this.cachedRenderCustomBackgroundFancyMenu = this.renderCustomBackgroundFancyMenu(button, GuiGraphics.currentGraphics(), button.x, button.y, button.getWidth(), button.getHeight());
		}

		if (this.cachedRenderCustomBackgroundFancyMenu) this.render119VanillaBackgroundFancyMenu(pose);

		return false;

	}

	/**
	 * This is to backport the 1.19+ widget background rendering
	 */
	@Unique
	private void render119VanillaBackgroundFancyMenu(PoseStack pose) {
		GuiGraphics graphics = GuiGraphics.currentGraphics();
		graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		graphics.blitNineSliced(WIDGETS_LOCATION, this.x, this.y, this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureYFancyMenu());
		RenderingUtils.resetShaderColor(graphics);
	}

	@Unique
	private int getTextureYFancyMenu() {
		boolean isSlider =((Object)this instanceof AbstractSliderButton);
		int i = 1;
		if (!this.active || isSlider) {
			i = 0;
		} else if (this.isHoveredOrFocused()) {
			i = 2;
		}
		return 46 + i * 20;
	}

	/**
	 * @reason Backporting the 1.19+ label rendering (sliding left to right)
	 */
	@WrapWithCondition(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private boolean wrapLabelRenderingFancyMenu(PoseStack pose, Font font, Component component, int i1, int i2, int i3) {
		this.renderScrollingLabel(this.getWidgetFancyMenu(), GuiGraphics.currentGraphics(), font, 2, true, -1);
		return false;
	}
	
	@Inject(method = "playDownSound", at = @At(value = "HEAD"), cancellable = true)
	private void beforeWidgetClickSoundFancyMenu(SoundManager manager, CallbackInfo info) {
		if (this.customClickSoundFancyMenu != null) {
			this.customClickSoundFancyMenu.stop();
			this.customClickSoundFancyMenu.play();
			info.cancel();
		}
	}

	@Inject(method = "getMessage", at = @At("RETURN"), cancellable = true)
	private void onGetMessageFancyMenu(CallbackInfoReturnable<Component> info) {
		AbstractWidget w = this.getWidgetFancyMenu();
		if (w.isHoveredOrFocused() && w.visible && w.active && (this.hoverLabelFancyMenu != null)) {
			info.setReturnValue(this.hoverLabelFancyMenu);
			return;
		}
		if (this.customLabelFancyMenu != null) info.setReturnValue(this.customLabelFancyMenu);
	}

	@Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
	private void beforeIsMouseOverFancyMenu(double $$0, double $$1, CallbackInfoReturnable<Boolean> info) {
		if (this.hiddenFancyMenu) info.setReturnValue(false);
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
		this.setCustomClickSoundFancyMenu(null);
		this.setHiddenFancyMenu(false);
		this.setCustomLabelFancyMenu(null);
		this.setHoverLabelFancyMenu(null);
		this.setCustomWidthFancyMenu(null);
		this.setCustomHeightFancyMenu(null);
		this.setCustomXFancyMenu(null);
		this.setCustomYFancyMenu(null);
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
		return this.customBackgroundNormalFancyMenu;
	}

	@Unique
	@Override
	public void setCustomBackgroundHoverFancyMenu(@Nullable RenderableResource background) {
		this.customBackgroundHoverFancyMenu = background;
	}

	@Unique
	@Override
	public @Nullable RenderableResource getCustomBackgroundHoverFancyMenu() {
		return this.customBackgroundHoverFancyMenu;
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
		return this.customBackgroundInactiveFancyMenu;
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
        return vanillaTooltip_FancyMenu;
    }

    @Unique
    @Override
    public void setVanillaTooltip_FancyMenu(@Nullable VanillaTooltip tooltip) {
        this.vanillaTooltip_FancyMenu = tooltip;
    }

}
