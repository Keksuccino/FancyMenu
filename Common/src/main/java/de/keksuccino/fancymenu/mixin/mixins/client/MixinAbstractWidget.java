package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.keksuccino.fancymenu.events.widget.RenderWidgetEvent;
import net.minecraft.client.gui.GuiComponent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.io.File;

@Mixin(value = AbstractWidget.class)
public abstract class MixinAbstractWidget extends GuiComponent implements CustomizableWidget, UniqueWidget {

	@Shadow protected float alpha;
	@Shadow public boolean visible;
	@Shadow public boolean active;
	@Shadow protected boolean isHovered;

	@Unique @Nullable
	private String widgetIdentifierFancyMenu;
	@Unique @Nullable
	private Component customLabelFancyMenu;
	@Unique @Nullable
	private Component hoverLabelFancyMenu;
	@Unique @Nullable
	private String customClickSoundFancyMenu;
	@Unique @Nullable
	private String hoverSoundFancyMenu;
	@Unique
	private boolean hiddenFancyMenu = false;
	@Unique
	private boolean lastHoverStateFancyMenu = false;
	
	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	private void beforeRenderFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {

		//Handle onHoverStart
		if (!this.hiddenFancyMenu && this.visible && this.active && this.getWidgetFancyMenu().isHovered() && (this.lastHoverStateFancyMenu != this.getWidgetFancyMenu().isHovered())) {
			this.onHoverStartFancyMenu();
		}
		this.lastHoverStateFancyMenu = this.getWidgetFancyMenu().isHovered();

		//Handle Hidden State
		if (this.hiddenFancyMenu) {
			this.isHovered = false;
			this.setFocused(false);
			info.cancel();
			return;
		}

		//Fire RenderWidgetEvent.Pre
		try {
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(matrix, this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Inject(at = @At(value = "TAIL"), method = "render")
	private void afterRenderFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(matrix, this.getWidgetFancyMenu(), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playDownSound", cancellable = true)
	private void beforeWidgetClickSoundFancyMenu(SoundManager manager, CallbackInfo info) {
		if (this.customClickSoundFancyMenu != null) {
			File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.customClickSoundFancyMenu));
			if (f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
				SoundHandler.registerSound(f.getPath(), f.getPath());
				SoundHandler.resetSound(f.getPath());
				SoundHandler.playSound(f.getPath());
				info.cancel();
			}
		}
	}

	@Inject(method = "getMessage", at = @At("RETURN"), cancellable = true)
	private void onGetMessageFancyMenu(CallbackInfoReturnable<Component> info) {
		AbstractWidget w = this.getWidgetFancyMenu();
		if (w.isHoveredOrFocused() && w.visible && w.active && (this.hoverLabelFancyMenu != null)) info.setReturnValue(this.hoverLabelFancyMenu);
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

	@Shadow public abstract void setFocused(boolean bl);

	@Unique
	private void onHoverStartFancyMenu() {
		//Handle Hover Sound
		if (this.hoverSoundFancyMenu != null) {
			File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.hoverSoundFancyMenu));
			if (f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
				SoundHandler.registerSound(f.getPath(), f.getPath());
				SoundHandler.resetSound(f.getPath());
				SoundHandler.playSound(f.getPath());
			}
		}
	}

	@SuppressWarnings("all")
	@Unique
	private AbstractWidget getWidgetFancyMenu() {
		return (AbstractWidget)((Object)this);
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
	public void setCustomClickSoundFancyMenu(@Nullable String customClickSoundFancyMenu) {
		this.customClickSoundFancyMenu = customClickSoundFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public String getCustomClickSoundFancyMenu() {
		return this.customClickSoundFancyMenu;
	}

	@Unique
	@Nullable
	@Override
	public String getHoverSoundFancyMenu() {
		return this.hoverSoundFancyMenu;
	}

	@Unique
	@Override
	public void setHoverSoundFancyMenu(@Nullable String hoverSoundFancyMenu) {
		this.hoverSoundFancyMenu = hoverSoundFancyMenu;
	}

	@Unique
	@Override
	public boolean isHiddenFancyMenu() {
		return this.hiddenFancyMenu;
	}

	@Unique
	@Override
	public void setHiddenFancyMenu(boolean hiddenFancyMenu) {
		this.hiddenFancyMenu = hiddenFancyMenu;
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
