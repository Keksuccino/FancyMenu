package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
		this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

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
			return;
		}
		IAudio globalClickSound = GlobalCustomizationHandler.getCustomButtonClickSound();
		if (globalClickSound != null) {
			globalClickSound.setSoundChannel(SoundSource.MASTER);
			globalClickSound.stop();
			globalClickSound.play();
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
		if ((Object)this instanceof AbstractSliderButton) {
			return GlobalCustomizationHandler.getCustomSliderHandleNormal();
		}
		return GlobalCustomizationHandler.getCustomButtonBackgroundNormal();
	}

	@Unique
	@Nullable
	private RenderableResource getGlobalBackgroundHover_FancyMenu() {
		if ((Object)this instanceof AbstractSliderButton) {
			return GlobalCustomizationHandler.getCustomSliderHandleHover();
		}
		return GlobalCustomizationHandler.getCustomButtonBackgroundHover();
	}

	@Unique
	@Nullable
	private RenderableResource getGlobalBackgroundInactive_FancyMenu() {
		if ((Object)this instanceof AbstractSliderButton) {
			return GlobalCustomizationHandler.getCustomSliderHandleInactive();
		}
		return GlobalCustomizationHandler.getCustomButtonBackgroundInactive();
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

}
