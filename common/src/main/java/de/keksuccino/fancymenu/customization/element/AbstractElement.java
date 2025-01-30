package de.keksuccino.fancymenu.customization.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

public abstract class AbstractElement implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget {

	private static final Logger LOGGER = LogManager.getLogger();

	/** The {@link AbstractElement#builder} field is NULL for this element! Keep that in mind when using it as placeholder! **/
	@SuppressWarnings("all")
	public static final AbstractElement EMPTY_ELEMENT = new AbstractElement(null){public void render(@NotNull GuiGraphics g, int i1, int i2, float f){}};
	public static final int STAY_ON_SCREEN_EDGE_ZONE_SIZE = 2;

	public final ElementBuilder<?,?> builder;
	public ElementAnchorPoint anchorPoint = ElementAnchorPoints.MID_CENTERED;
	public String anchorPointElementIdentifier = null;
	protected AbstractElement cachedElementAnchorPointParent = null;
	/** Not the same as {@link AbstractElement#getAbsoluteX()}! This is the X-offset from the origin of its anchor! **/
	public int posOffsetX = 0;
	/** Not the same as {@link AbstractElement#getAbsoluteY()}! This is the Y-offset from the origin of its anchor! **/
	public int posOffsetY = 0;
	public int baseWidth = 0;
	public int baseHeight = 0;
	public String advancedX;
	public Integer cachedAdvancedX;
	public long lastAdvancedXParse = -1;
	public String advancedY;
	public Integer cachedAdvancedY;
	public long lastAdvancedYParse = -1;
	public String advancedWidth;
	public Integer cachedAdvancedWidth;
	public long lastAdvancedWidthParse = -1;
	public String advancedHeight;
	public Integer cachedAdvancedHeight;
	public long lastAdvancedHeightParse = -1;
	public boolean stretchX = false;
	public boolean stretchY = false;
	public boolean stayOnScreen = true;
	public volatile boolean visible = true;
	public volatile AppearanceDelay appearanceDelay = AppearanceDelay.NO_DELAY;
	public volatile float appearanceDelayInSeconds = 1.0F;
	public long appearanceDelayEndTime = -1;
	@NotNull
	public Fading fadeIn = Fading.NO_FADING;
	@NotNull
	public Fading fadeOut = Fading.NO_FADING;
	public float fadeInSpeed = 1.0F;
	public float fadeOutSpeed = 1.0F;
	public boolean shouldDoFadeInIfNeeded = false;
	public boolean fadeInStarted = false;
	public boolean fadeInFinished = false;
	public boolean shouldDoFadeOutIfNeeded = false;
	public boolean fadeOutStarted = false;
	public boolean fadeOutFinished = false;
	public long lastFadeInTick = -1;
	public long lastFadeOutTick = -1;
	public float opacity = 1.0F;
	@NotNull
	public String baseOpacity = "1.0";
	public float lastBaseOpacity = -1.0F;
	public long lastBaseOpacityParse = -1L;
	public float cachedBaseOpacity = 1.0F;
	public boolean becameVisible = false;
	public boolean becameInvisible = false;
	public boolean isNewMenu = ScreenCustomization.isNewMenu();
	public boolean fadeInElementJustCreated = true;
	public boolean fadeOutElementJustCreated = true;
	public boolean appearanceDelayElementJustCreated = true;
	public boolean lastTickAppearanceDelayed = false;
	public boolean autoSizing = false;
	public int autoSizingBaseScreenWidth = 0;
	public int autoSizingBaseScreenHeight = 0;
	public double autoSizingLastTickScreenWidth = -1;
	public double autoSizingLastTickScreenHeight = -1;
	public int autoSizingWidth = 0;
	public int autoSizingHeight = 0;
	public boolean stickyAnchor = false;
	public int animatedOffsetX = 0;
	public int animatedOffsetY = 0;
	/**
	 * This is for when the render scale was changed in a non-system-wide way like via {@link PoseStack#translate(float, float, float)}.<br>
	 * Elements that do not support scaling via {@link PoseStack#translate(float, float, float)} need to use this value to manually scale themselves.<br>
	 * This value is -1F by default and is not always set to an actual scale, so check this before using it!
	 **/
	public float customGuiScale = -1F;
	public LoadingRequirementContainer loadingRequirementContainer = new LoadingRequirementContainer();
	@Nullable
	public String customElementLayerName = null;
	/**
	 * Controls whether the element should enable parallax movement in response to mouse position.
	 * When true, the element will move slightly opposite to mouse movement (or with it if invertParallax is true)
	 * to create a depth effect.
	 */
	public boolean enableParallax = false;
	/**
	 * Controls the direction of parallax movement.
	 * When false (default), elements move opposite to mouse movement.
	 * When true, elements move with mouse movement.
	 */
	public boolean invertParallax = false;
	/**
	 * Controls the intensity of the parallax effect.
	 * Range is 0.0 to 1.0 where:
	 * - 0.0 means no movement
	 * - 1.0 means maximum movement
	 * Default is 0.5 for medium intensity.
	 */
	public float parallaxIntensity = 0.5f;
	public boolean loadOncePerSession = false;
	private String instanceIdentifier;
	@Nullable
	protected Layout parentLayout;
	@Nullable
	protected RuntimePropertyContainer cachedMemory;
	protected int cachedMouseX = 0;
	protected int cachedMouseY = 0;

	@SuppressWarnings("all")
	public AbstractElement(@NotNull ElementBuilder<?,?> builder) {
		this.builder = builder;
		this.instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
	}

	public void setParentLayout(@Nullable Layout parentLayout) {
		this.parentLayout = parentLayout;
	}

	@Nullable
	public Layout getParentLayout() {
		return this.parentLayout;
	}

	/**
	 * This method gets called after the element's {@link ElementBuilder} has finished constructing the instance,
	 * which means at this point everything should be ready to use, like the element's identifier.
	 */
	public void afterConstruction() {
	}

	@Override
	public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

	/**
	 * This is the internal render method that should only get overridden if there's really no other way around it.<br>
	 * The normal element rendering logic should be in {@link AbstractElement#render(GuiGraphics, int, int, float)}.
	 */
	public void renderInternal(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		this.cachedMouseX = mouseX;
		this.cachedMouseY = mouseY;

		this.tickBaseOpacity();

		this.renderTick_Head();

		this.tickVisibleInvisible();

		this.renderTick_Inner_Stage_1();

		if (!isEditor()) {

			this.tickAppearanceDelay(this.shouldRender());

			this.tickFadeInOut(this.shouldRender());

		}

		if (!this.shouldRender()) return;

		this.renderTick_Inner_Stage_2();

		//Render the actual element
		this.render(graphics, mouseX, mouseY, partial);

		this.renderTick_Tail();

	}

	public void renderTick_Head() {
	}

	public void renderTick_Inner_Stage_1() {
	}

	public void renderTick_Inner_Stage_2() {
	}

	public void renderTick_Tail() {
	}

	public void tickBaseOpacity() {

		//Don't update opacity while fade-in/out is active
		if (this.fadeInStarted && !this.fadeInFinished) return;
		if (this.fadeOutStarted && !this.fadeOutFinished) return;

		float newBaseOpacity = this.getBaseOpacity();
		if (newBaseOpacity != this.lastBaseOpacity) {
			this.updateOpacity();
		}
		this.lastBaseOpacity = newBaseOpacity;

	}

	public void tickVisibleInvisible() {

		if (!this._shouldRender()) {
			if (!this.becameInvisible) {
				this.becameInvisible = true;
				this.onBecomeInvisible();
			}
			this.becameVisible = false;
			return;
		}
		this.becameInvisible = false;
		if (!this.becameVisible) {
			this.becameVisible = true;
			this.onBecomeVisible();
		}

	}

	public void tickAppearanceDelay(boolean shouldRender) {

		if (!shouldRender) return;

		//Make element remember that the appearance delay got applied (for only-first-time appearance delays)
		if (this.lastTickAppearanceDelayed && !this.isAppearanceDelayed()) {
			this.getMemory().putProperty("appearance_delay_applied", true);
		}
		this.lastTickAppearanceDelayed = this.isAppearanceDelayed();

	}

	public void tickFadeInOut(boolean shouldRender) {

		if (shouldRender) {

			//Handle fade-in
			boolean fadeInIsResize = !this.isNewMenu && this.fadeInElementJustCreated;
			boolean fadeInDone = this.getMemory().putPropertyIfAbsentAndGet("fade_in_done", false);
			if (!fadeInIsResize || !fadeInDone) {
				if ((this.fadeIn != Fading.NO_FADING) && this.shouldDoFadeInIfNeeded && (this.lastBaseOpacity > 0.0F)) {
					if ((this.fadeIn != Fading.FIRST_TIME) || !fadeInDone) {
						if (!this.fadeInStarted) {
							this.fadeInStarted = true;
							this.opacity = 0.0F;
						}
						if ((this.lastFadeInTick + (long)(50.0F * this.fadeInSpeed)) < System.currentTimeMillis()) {
							this.lastFadeInTick = System.currentTimeMillis();
							this.opacity += 0.02F;
						}
						if (this.opacity >= this.lastBaseOpacity) {
							this.opacity = this.lastBaseOpacity;
							this.shouldDoFadeInIfNeeded = false;
							this.fadeInFinished = true;
							this.getMemory().putProperty("fade_in_done", true);
						}
					}
				}
			} else {
				this.shouldDoFadeInIfNeeded = false;
				this.fadeInStarted = false;
				this.fadeInFinished = false;
			}

		}

		this.fadeInElementJustCreated = false;

		//Handle fade-out
		if ((this.fadeOut != Fading.NO_FADING) && this.shouldDoFadeOutIfNeeded && (this.lastBaseOpacity > 0.0F)) {
			if (!this.fadeOutFinished) {
				boolean fadeOutDone = this.getMemory().putPropertyIfAbsentAndGet("fade_out_done", false);
				if ((this.fadeOut != Fading.FIRST_TIME) || !fadeOutDone) {
					if (!this.fadeOutStarted) {
						this.fadeOutStarted = true;
						this.opacity = this.lastBaseOpacity;
					}
					if ((this.lastFadeOutTick + (long)(50.0F * this.fadeOutSpeed)) < System.currentTimeMillis()) {
						this.lastFadeOutTick = System.currentTimeMillis();
						this.opacity -= 0.02F;
					}
					if (this.opacity <= 0.0F) {
						this.opacity = 0.0F;
						this.shouldDoFadeOutIfNeeded = false;
						this.fadeOutFinished = true;
						this.getMemory().putProperty("fade_out_done", true);
					}
				}
			}
		}

	}

	public void onBecomeVisible() {

		this.applyAppearanceDelay();
		this.updateOpacity();

		this.fadeInStarted = false;
		this.fadeInFinished = false;
		this.fadeOutStarted = false;
		this.fadeOutFinished = false;
		this.shouldDoFadeInIfNeeded = true;
		this.shouldDoFadeOutIfNeeded = false;

	}

	public void onBecomeInvisible() {

		this.updateOpacity();

		this.fadeInStarted = false;
		this.fadeInFinished = false;
		this.fadeOutStarted = false;
		this.fadeOutFinished = false;
		this.shouldDoFadeOutIfNeeded = true;
		this.shouldDoFadeInIfNeeded = false;

		if (this.fadeOutElementJustCreated) {
			this.shouldDoFadeOutIfNeeded = false;
		}
		this.fadeOutElementJustCreated = false;

	}

	public void applyAppearanceDelay() {
		boolean isResize = !this.isNewMenu && this.appearanceDelayElementJustCreated;
		this.appearanceDelayElementJustCreated = false;
		if (isEditor()) {
			this.appearanceDelayEndTime = -1;
			return;
		}
		boolean applied = this.getMemory().putPropertyIfAbsentAndGet("appearance_delay_applied", false);
		if ((!isResize || !applied) && (this.appearanceDelay != AppearanceDelay.NO_DELAY) && (this.appearanceDelayInSeconds > 0.0F)) {
			if ((this.appearanceDelay == AppearanceDelay.FIRST_TIME) && applied) {
				this.appearanceDelayEndTime = -1;
			} else {
				this.appearanceDelayEndTime = System.currentTimeMillis() + ((long)(this.appearanceDelayInSeconds * 1000.0F));
			}
		} else {
			this.appearanceDelayEndTime = -1;
		}
		this.lastTickAppearanceDelayed = this.isAppearanceDelayed();
	}

	public void updateOpacity() {
		this.opacity = this.getBaseOpacity();
	}

	public float getBaseOpacity() {
		long now = System.currentTimeMillis();
		if ((this.lastBaseOpacityParse + 30L) > now) return this.cachedBaseOpacity;
		this.lastBaseOpacityParse = now;
		String s = PlaceholderParser.replacePlaceholders(this.baseOpacity);
		if (MathUtils.isFloat(s)) {
			float f = Float.parseFloat(s);
			if (f < 0.0F) f = 0.0F;
			if (f > 1.0F) f = 1.0F;
			this.cachedBaseOpacity = f;
			return f;
		}
		this.cachedBaseOpacity = 1.0F;
		return 1.0F;
	}

	/**
	 * Gets called every {@link Screen} tick, after {@link Screen#tick()} got called.
	 */
	public void tick() {
	}

	/**
	 * Gets called before a {@link Screen} gets closed.<br>
	 * A screen gets closed when a new active {@link Screen} gets set via {@link Minecraft#setScreen(Screen)}.<br><br>
	 *
	 * Keep in mind that, just like most Vanilla GUI stuff, {@link AbstractElement}s get rebuilt every time the {@link Screen} gets resized,
	 * so this method will only get called for the VERY LAST {@link AbstractElement} instance that got built for the {@link Screen} while it was active.
	 * It does not get called for instances that got built earlier (by resizing the {@link Screen} multiple times for example).
	 */
	public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
	}

	/**
	 * Gets called before a {@link Screen} gets closed.<br>
	 * A screen gets closed when a new active {@link Screen} gets set via {@link Minecraft#setScreen(Screen)}.<br><br>
	 *
	 * Keep in mind that, just like most Vanilla GUI stuff, {@link AbstractElement}s get rebuilt every time the {@link Screen} gets resized,
	 * so this method will only get called for the VERY LAST {@link AbstractElement} instance that got built for the {@link Screen} while it was active.
	 * It does not get called for instances that got built earlier (by resizing the {@link Screen} multiple times for example).
	 */
	@Deprecated
	public void onCloseScreen() {
	}


	/**
	 * Gets called after a {@link Screen} got opened via {@link Minecraft#setScreen(Screen)}.<br>
	 * The {@link Screen} is already initialized at the time this method gets called.<br><br>
	 *
	 * Keep in mind that, just like most Vanilla GUI stuff, {@link AbstractElement}s get rebuilt every time the {@link Screen} gets resized,
	 * so this method will only get called for the FIRST {@link AbstractElement} instance that gets build for the {@link Screen} after opening it.
	 * It does not get called for instances that get build because of resizing the {@link Screen}.
	 */
	public void onOpenScreen() {
	}

	@ApiStatus.Internal
	public void _onOpenScreen() {

		this.onOpenScreen();

	}

	/**
	 * Gets called before the current {@link Screen} gets resized.<br>
	 * Does NOT get called on initial resize (when opening the screen). Use {@link AbstractElement#onOpenScreen()} for that instead.<br><br>
	 *
	 * Just like most Vanilla GUI stuff, {@link AbstractElement}s get rebuilt every time the screen size changes,
	 * so every time this method gets called, it gets called for a new {@link AbstractElement} instance.<br><br>
	 *
	 * This method should never get called more than once for an {@link AbstractElement} instance.
	 */
	public void onBeforeResizeScreen() {
	}

	/**
	 * Gets called before the element instance gets destroyed.<br><br>
	 *
	 * Just like most Vanilla GUI stuff, {@link AbstractElement}s get rebuilt every time the screen size changes,
	 * so this method gets called every time the {@link Screen} gets resized and when it gets closed by setting
	 * a new {@link Screen} (or no screen) via {@link Minecraft#setScreen(Screen)}.
	 */
	public void onDestroyElement() {
	}

	/**
	 * All widgets of the returned list will get registered to {@link Screen#children()}.<br>
	 * Keep in mind the returned widgets will NOT get registered to {@link Screen#renderables}.<br>
	 * Widgets need to extend {@link GuiEventListener} and {@link NarratableEntry}.
	 */
	@SuppressWarnings("all")
	@Nullable
	public List<GuiEventListener> getWidgetsToRegister() {
		return null;
	}

	@NotNull
	public String getInstanceIdentifier() {
		return this.instanceIdentifier;
	}

	public void setInstanceIdentifier(@NotNull String id) {
		this.instanceIdentifier = Objects.requireNonNull(id);
	}

	/**
	 * Returns the actual/final X position the element will have when it gets rendered.
	 */
	public int getAbsoluteX() {
		int x = 0;
		if (this.anchorPoint != null) {
			x = this.anchorPoint.getElementPositionX(this);
		}

		if (this.advancedX != null) {
			long now = System.currentTimeMillis();
			if (((this.lastAdvancedXParse + 30) > now) && (this.cachedAdvancedX != null)) {
				x = this.cachedAdvancedX;
			} else {
				String s = PlaceholderParser.replacePlaceholders(this.advancedX).replace(" ", "");
				if (MathUtils.isDouble(s)) {
					x = (int) Double.parseDouble(s);
					this.cachedAdvancedX = x;
					this.lastAdvancedXParse = now;
				}
			}
		}

		x += this.animatedOffsetX;

		boolean applyParallax = this.enableParallax && !isEditor();

		// Apply parallax effect if enabled and not in editor
		if (applyParallax) {
			// Calculate parallax offset using cached mouse position
			float centerX = getScreenWidth() / 2f;
			float offsetX = this.cachedMouseX - centerX;
			float parallaxOffset = offsetX * parallaxIntensity * 0.1f; // Scale factor to control maximum movement

			// Apply offset based on direction
			x += (int) (invertParallax ? parallaxOffset : -parallaxOffset);
		}

		if (this.stretchX) {
			x = 0;
		} else if (this.stayOnScreen && !this.stickyAnchor && !applyParallax) {
			if (x < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
				x = STAY_ON_SCREEN_EDGE_ZONE_SIZE;
			}
			if (x > (getScreenWidth() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteWidth())) {
				x = getScreenWidth() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteWidth();
			}
		}
		return x;
	}

	/**
	 * Returns the actual/final Y position the element will have when it gets rendered.
	 */
	public int getAbsoluteY() {
		int y = 0;
		if (this.anchorPoint != null) {
			y = this.anchorPoint.getElementPositionY(this);
		}

		if (this.advancedY != null) {
			long now = System.currentTimeMillis();
			if (((this.lastAdvancedYParse + 30) > now) && (this.cachedAdvancedY != null)) {
				y = this.cachedAdvancedY;
			} else {
				String s = PlaceholderParser.replacePlaceholders(this.advancedY).replace(" ", "");
				if (MathUtils.isDouble(s)) {
					y = (int) Double.parseDouble(s);
					this.cachedAdvancedY = y;
					this.lastAdvancedYParse = now;
				}
			}
		}

		y += this.animatedOffsetY;

		boolean applyParallax = this.enableParallax && !isEditor();

		// Apply parallax effect if enabled and not in editor
		if (applyParallax) {
			// Calculate parallax offset using cached mouse position
			float centerY = getScreenHeight() / 2f;
			float offsetY = this.cachedMouseY - centerY;
			float parallaxOffset = offsetY * parallaxIntensity * 0.1f; // Scale factor to control maximum movement

			// Apply offset based on direction
			y += (int) (invertParallax ? parallaxOffset : -parallaxOffset);
		}

		if (this.stretchY) {
			y = 0;
		} else if (this.stayOnScreen && !this.stickyAnchor && !applyParallax) {
			if (y < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
				y = STAY_ON_SCREEN_EDGE_ZONE_SIZE;
			}
			if (y > (getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight())) {
				y = getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight();
			}
		}
		return y;
	}

	public void setAutoSizingBaseWidthAndHeight() {
		Window window = Minecraft.getInstance().getWindow();
		double guiWidth = getScreenWidth() * window.getGuiScale();
		double guiHeight = getScreenHeight() * window.getGuiScale();
		this.autoSizingBaseScreenWidth = (int)guiWidth;
		this.autoSizingBaseScreenHeight = (int)guiHeight;
	}

	public void updateAutoSizing(boolean ignoreLastTickScreenSize) {

		Window window = Minecraft.getInstance().getWindow();
		double guiWidth = getScreenWidth() * window.getGuiScale();
		double guiHeight = getScreenHeight() * window.getGuiScale();

		if (((this.autoSizingLastTickScreenWidth != guiWidth) || (this.autoSizingLastTickScreenHeight != guiHeight)) || ignoreLastTickScreenSize) {
			if (this.autoSizing && (this.autoSizingBaseScreenWidth > 0) && (this.autoSizingBaseScreenHeight > 0)) {
				double percentX = Math.max(1.0D, (guiWidth / (double) this.autoSizingBaseScreenWidth) * 100.0D);
				double percentY = Math.max(1.0D, (guiHeight / (double) this.autoSizingBaseScreenHeight) * 100.0D);
				double percent = Math.min(percentX, percentY);
				this.autoSizingWidth = Math.max(1, (int) ((percent / 100.0D) * (double) this.baseWidth));
				this.autoSizingHeight = Math.max(1, (int) ((percent / 100.0D) * (double) this.baseHeight));
				if ((this.autoSizingBaseScreenWidth == guiWidth) && (this.autoSizingBaseScreenHeight == guiHeight)) {
					this.autoSizingWidth = 0;
					this.autoSizingHeight = 0;
				}
			} else {
				this.autoSizingWidth = 0;
				this.autoSizingHeight = 0;
			}
		}
		this.autoSizingLastTickScreenWidth = guiWidth;
		this.autoSizingLastTickScreenHeight = guiHeight;

	}

	/**
	 * Returns the actual/final width the element will have when it gets rendered.
	 */
	public int getAbsoluteWidth() {
		if (this.advancedWidth != null) {
			long now = System.currentTimeMillis();
			//Cache advancedWidth for 30ms to save performance (thanks to danorris for the idea!)
			if (((this.lastAdvancedWidthParse + 30) > now) && (this.cachedAdvancedWidth != null)) {
				return this.cachedAdvancedWidth;
			} else {
				String s = PlaceholderParser.replacePlaceholders(this.advancedWidth).replace(" ", "");
				if (MathUtils.isDouble(s)) {
					this.cachedAdvancedWidth = (int) Double.parseDouble(s);
					this.lastAdvancedWidthParse = now;
					return this.cachedAdvancedWidth;
				}
			}
		}
		if (this.stretchX) {
			return getScreenWidth();
		}
		this.updateAutoSizing(false);
		if (this.autoSizing && (this.autoSizingWidth > 0)) {
			return this.autoSizingWidth;
		}
		return this.baseWidth;
	}

	/**
	 * Returns the actual/final height the element will have when it gets rendered.
	 */
	public int getAbsoluteHeight() {
		if (this.advancedHeight != null) {
			long now = System.currentTimeMillis();
			//Cache advancedHeight for 30ms to save performance (thanks to danorris for the idea!)
			if (((this.lastAdvancedHeightParse + 30) > now) && (this.cachedAdvancedHeight != null)) {
				return this.cachedAdvancedHeight;
			} else {
				String s = PlaceholderParser.replacePlaceholders(this.advancedHeight).replace(" ", "");
				if (MathUtils.isDouble(s)) {
					this.cachedAdvancedHeight = (int) Double.parseDouble(s);
					this.lastAdvancedHeightParse = now;
					return this.cachedAdvancedHeight;
				}
			}
		}
		if (this.stretchY) {
			return getScreenHeight();
		}
		this.updateAutoSizing(false);
		if (this.autoSizing && (this.autoSizingHeight > 0)) {
			return this.autoSizingHeight;
		}
		return this.baseHeight;
	}

	/**
	 * Returns this element's PARENT element, if this element uses the {@link ElementAnchorPoints#ELEMENT} anchor.
	 */
	@Nullable
	public AbstractElement getElementAnchorPointParent() {
		if (this.anchorPointElementIdentifier == null) return null;
		if (this.cachedElementAnchorPointParent == null) {
			this.cachedElementAnchorPointParent = getElementByInstanceIdentifier(this.anchorPointElementIdentifier);
		}
		return this.cachedElementAnchorPointParent;
	}

	/**
	 * This is to set this element's PARENT element, if this element uses the {@link ElementAnchorPoints#ELEMENT} anchor.
	 */
	public void setElementAnchorPointParent(@Nullable AbstractElement element) {
		this.cachedElementAnchorPointParent = element;
	}

	/**
	 * This is the X position used by the CHILD element if this element is used as {@link ElementAnchorPoints#ELEMENT} anchor.
	 */
	public int getChildElementAnchorPointX() {
		return this.getAbsoluteX();
	}

	/**
	 * This is the Y position used by the CHILD element if this element is used as {@link ElementAnchorPoints#ELEMENT} anchor.
	 */
	public int getChildElementAnchorPointY() {
		return this.getAbsoluteY();
	}

	public boolean isAppearanceDelayed() {
		return (System.currentTimeMillis() < this.appearanceDelayEndTime);
	}

	public boolean shouldRender() {

		if (!isEditor() && this.loadOncePerSession && this.shouldHideOncePerSessionElement()) return false;

		if (this.isAppearanceDelayed() && !isEditor()) return false;

		boolean b = this._shouldRender();
		if (!isEditor()) {
			if (!b && this.fadeOutStarted && !this.fadeOutFinished) return true;
		}

		return b;

	}

	protected boolean _shouldRender() {
		if (!this.loadingRequirementsMet()) return false;
		return this.visible;
	}

	public boolean loadingRequirementsMet() {
		if (isEditor()) return true;
		return this.loadingRequirementContainer.requirementsMet();
	}

	public boolean shouldHideOncePerSessionElement() {
		return Objects.requireNonNullElse(this.getMemory().getBooleanProperty("hide_once_per_session_element"), false);
	}

	public void setHideOncePerSessionElement() {
		this.getMemory().putPropertyIfAbsent("hide_once_per_session_element", true);
	}

	@NotNull
	public Component getDisplayName() {
		if (this.customElementLayerName != null) return Component.literal(this.customElementLayerName);
		return this.builder.getDisplayName(this);
	}

	/**
	 * The memory of an {@link AbstractElement} remembers variables across element rebuilding.<br>
	 * It can be used if an element needs to access data of its ancestors.<br><br>
	 *
	 * Every element (based on its instance identifier) has its own memory.
	 */
	@NotNull
	public RuntimePropertyContainer getMemory() {
		if (this.cachedMemory == null) {
			this.cachedMemory = ElementMemories.getMemory(this.getInstanceIdentifier());
		}
		return this.cachedMemory;
	}

	public static String fixBackslashPath(String path) {
		if (path != null) return path.replace("\\", "/");
		return null;
	}

	protected static boolean isEditor() {
		return (getScreen() instanceof LayoutEditorScreen);
	}

	@Nullable
	public static Screen getScreen() {
		return Minecraft.getInstance().screen;
	}

	public static int getScreenWidth() {
		Screen s = getScreen();
		return (s != null) ? s.width : 0;
	}

	public static int getScreenHeight() {
		Screen s = getScreen();
		return (s != null) ? s.height : 0;
	}

	@SuppressWarnings("all")
	@Nullable
	public static AbstractElement getElementByInstanceIdentifier(String identifier) {
		identifier = identifier.replace("vanillabtn:", "").replace("button_compatibility_id:", "");
		if (isEditor()) {
			AbstractEditorElement editorElement = ((LayoutEditorScreen)getScreen()).getElementByInstanceIdentifier(identifier);
			if (editorElement != null) return editorElement.element;
		} else {
			ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
			if (layer != null) return layer.getElementByInstanceIdentifier(identifier);
		}
		return null;
	}

	/**
	 * Replaces placeholders and deserializes serialized {@link MutableComponent}s.
	 */
	@NotNull
	public static Component buildComponent(@NotNull String serializedComponentOrPlainText) {
		serializedComponentOrPlainText = PlaceholderParser.replacePlaceholders(serializedComponentOrPlainText);
		if (!serializedComponentOrPlainText.startsWith("{")) return Component.literal(serializedComponentOrPlainText);
		try {
			Component c = deserializeComponentFromJson(serializedComponentOrPlainText);
			if (c != null) return c;
		} catch (Exception ignore) {}
		return Component.literal(serializedComponentOrPlainText);
	}

	@Nullable
	protected static MutableComponent deserializeComponentFromJson(@NotNull String json) {
		try {
			JsonElement jsonElement = JsonParser.parseString(json);
			return (jsonElement == null) ? null : deserializeComponent(jsonElement);
		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to deserialize Component!", ex);
		}
		return null;
	}

	private static MutableComponent deserializeComponent(JsonElement jsonElement) {
		if (ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow() instanceof MutableComponent m) {
			return m;
		}
		throw new IllegalStateException("Deserialized component was not a MutableComponent!");
	}

	@Override
	public void setFocused(boolean var1) {
	}

	@Override
	public boolean isFocused() {
		return false;
	}

	@Override
	public @NotNull NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}

	@Override
	public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
	}

	@Override
	public boolean isFocusable() {
		return false;
	}

	@Override
	public void setFocusable(boolean focusable) {
		throw new RuntimeException("AbstractElements are not focusable!");
	}

	@Override
	public boolean isNavigatable() {
		return false;
	}

	@Override
	public void setNavigatable(boolean navigatable) {
		throw new RuntimeException("AbstractElements are not navigatable!");
	}

	/**
	 * This method always needs to return FALSE, otherwise menus will BREAK!
	 */
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return false;
	}

	public enum Alignment {
		
		LEFT("left"),
		RIGHT("right"),
		CENTERED("centered");
		
		public final String key;
		
		Alignment(String key) {
			this.key = key;
		}

		public static Alignment getByName(@NotNull String name) {
			for (Alignment a : Alignment.values()) {
				if (a.key.equals(name)) {
					return a;
				}
			}
			return null;
		}
		
	}

	public enum AppearanceDelay {

		NO_DELAY("no_delay"),
		FIRST_TIME("first_time"),
		EVERY_TIME("every_time");

		public final String name;

		AppearanceDelay(String name) {
			this.name = name;
		}

		@Nullable
		public static AppearanceDelay getByName(@NotNull String name) {
			for (AppearanceDelay d : AppearanceDelay.values()) {
				if (d.name.equals(name)) {
					return d;
				}
			}
			return null;
		}

	}

	public enum Fading {

		NO_FADING("no_fading"),
		FIRST_TIME("first_time"),
		EVERY_TIME("every_time");

		private final String name;

		Fading(@NotNull String name) {
			this.name = name;
		}

		public @NotNull String getName() {
			return this.name;
		}

		@Nullable
		public static Fading getByName(@NotNull String name) {
			for (Fading mode : Fading.values()) {
				if (mode.getName().equals(name)) return mode;
			}
			return null;
		}

	}

}
