package de.keksuccino.fancymenu.customization.element;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

public abstract class AbstractElement implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget {

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
	//TODO übernehmen
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
	public float baseOpacity = 1.0F;
	public boolean becameVisible = false;
	public boolean becameInvisible = false;
	public boolean isNewMenu = ScreenCustomization.isNewMenu();
	public boolean fadeInElementJustCreated = true;
	public boolean appearanceDelayElementJustCreated = true;
	public boolean lastTickAppearanceDelayed = false;
	//-------------------
	/**
	 * This is for when the render scale was changed in a non-system-wide way like via {@link PoseStack#translate(float, float, float)}.<br>
	 * Elements that do not support scaling via {@link PoseStack#translate(float, float, float)} need to use this value to manually scale themselves.<br>
	 * This value is -1F by default and is not always set to an actual scale, so check this before using it!
	 **/
	public float customGuiScale = -1F;
	public LoadingRequirementContainer loadingRequirementContainer = new LoadingRequirementContainer();
	@Nullable
	public String customElementLayerName = null;
	private String instanceIdentifier;
	@Nullable
	protected Layout parentLayout;
	@Nullable
	protected RuntimePropertyContainer cachedMemory;

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

	@Override
	public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

	//TODO übernehmen
	/**
	 * This is the internal render method that should only get overridden if there's really no other way around it.<br>
	 * The normal element rendering logic should be in {@link AbstractElement#render(GuiGraphics, int, int, float)}.
	 */
	public void renderInternal(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		this.renderTick_Head();

		//This is the first stage of shouldRender() checks + handling of becomeVisible/Invisible methods
		if (!this.tickVisibleInvisible()) return;

		this.renderTick_Post_ShouldRender_Stage_1();

		//This is the second stage of shouldRender() checks that also checks for appearance delays
		if (!this.shouldRender()) return;

		if (!isEditor()) {

			this.tickAppearanceDelay();

			this.tickFadeInOut();

		}

		this.renderTick_Post_ShouldRender_Stage_2();

		//Render the actual element
		this.render(graphics, mouseX, mouseY, partial);

		this.renderTick_Tail();

	}

	//TODO übernehmen
	public void renderTick_Head() {
	}

	//TODO übernehmen
	public void renderTick_Post_ShouldRender_Stage_1() {
	}

	//TODO übernehmen
	public void renderTick_Post_ShouldRender_Stage_2() {
	}

	//TODO übernehmen
	public void renderTick_Tail() {
	}

	//TODO übernehmen
	public boolean tickVisibleInvisible() {

		//This is the first stage of shouldRender() checks + handling of becomeVisible/Invisible methods
		if (!this._shouldRender()) {
			if (!this.becameInvisible) {
				this.becameInvisible = true;
				this.onBecomeInvisible();
			}
			this.becameVisible = false;
			return false;
		}
		this.becameInvisible = false;
		if (!this.becameVisible) {
			this.becameVisible = true;
			this.onBecomeVisible();
		}

		return true;

	}

	//TODO übernehmen
	public void tickAppearanceDelay() {

		//Make element remember that the appearance delay got applied (for only-first-time appearance delays)
		if (this.lastTickAppearanceDelayed && !this.isAppearanceDelayed()) {
			this.getMemory().putProperty("appearance_delay_applied", true);
		}
		this.lastTickAppearanceDelayed = this.isAppearanceDelayed();

	}

	//TODO übernehmen
	public void tickFadeInOut() {

		//Handle fade-in
		boolean fadeInIsResize = !this.isNewMenu && this.fadeInElementJustCreated;
		boolean fadeInDone = this.getMemory().putPropertyIfAbsentAndGet("fade_in_done", false);
		if (!fadeInIsResize || !fadeInDone) {
			if ((this.fadeIn != Fading.NO_FADING) && this.shouldDoFadeInIfNeeded && (this.baseOpacity > 0.0F)) {
				if ((this.fadeIn != Fading.FIRST_TIME) || !fadeInDone) {
					if (!this.fadeInStarted) {
						this.fadeInStarted = true;
						this.opacity = 0.0F;
					}
					if ((this.lastFadeInTick + (long)(50.0F * this.fadeInSpeed)) < System.currentTimeMillis()) {
						this.lastFadeInTick = System.currentTimeMillis();
						this.opacity += 0.02F;
					}
					if (this.opacity >= this.baseOpacity) {
						this.opacity = this.baseOpacity;
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
		this.fadeInElementJustCreated = false;

		//Handle fade-out
		if ((this.fadeOut != Fading.NO_FADING) && this.shouldDoFadeOutIfNeeded && (this.baseOpacity > 0.0F)) {
			boolean fadeOutDone = this.getMemory().putPropertyIfAbsentAndGet("fade_out_done", false);
			if ((this.fadeOut != Fading.FIRST_TIME) || !fadeOutDone) {
				if (!this.fadeOutStarted) {
					this.fadeOutStarted = true;
					this.opacity = this.baseOpacity;
				}
				if ((this.lastFadeOutTick + (long)(100.0F * this.fadeOutSpeed)) < System.currentTimeMillis()) {
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

	//TODO übernehmen
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

	//TODO übernehmen
	public void onBecomeInvisible() {

		this.updateOpacity();

		this.fadeInStarted = false;
		this.fadeInFinished = false;
		this.fadeOutStarted = false;
		this.fadeOutFinished = false;
		this.shouldDoFadeOutIfNeeded = true;
		this.shouldDoFadeInIfNeeded = false;

	}

	//TODO übernehmen
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
				this.appearanceDelayEndTime = System.currentTimeMillis() + ((long)this.appearanceDelayInSeconds * 1000L);
			}
		} else {
			//this.getMemory().putProperty("appearance_delay_applied", false);
			this.appearanceDelayEndTime = -1;
		}
		this.lastTickAppearanceDelayed = this.isAppearanceDelayed();
	}

	//TODO übernehmen
	public void updateOpacity() {
		this.opacity = this.baseOpacity;
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
	 * Take in mind the returned widgets will NOT get registered to {@link Screen#renderables}.<br>
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
			//Cache advancedX for 30ms to save performance (thanks to danorris for the idea!)
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
		if (this.stretchX) {
			x = 0;
		} else if (this.stayOnScreen) {
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
			//Cache advancedY for 30ms to save performance (thanks to danorris for the idea!)
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
		if (this.stretchY) {
			y = 0;
		} else if (this.stayOnScreen) {
			if (y < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
				y = STAY_ON_SCREEN_EDGE_ZONE_SIZE;
			}
			if (y > (getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight())) {
				y = getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight();
			}
		}
		return y;
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

	//TODO übernehmen
	public boolean isAppearanceDelayed() {
		return (System.currentTimeMillis() < this.appearanceDelayEndTime);
	}

	//TODO übernehmen
	public boolean shouldRender() {
		if (this.isAppearanceDelayed() && !isEditor()) return false;
		boolean b = this._shouldRender();
		if (!isEditor()) {
			if (!b && this.fadeOutStarted && !this.fadeOutFinished) return true;
		}
		return b;
	}

	//TODO übernehmen
	protected boolean _shouldRender() {
		if (!this.loadingRequirementsMet()) return false;
		return this.visible;
	}

	protected boolean loadingRequirementsMet() {
		if (isEditor()) return true;
		return this.loadingRequirementContainer.requirementsMet();
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
			Component c = Component.Serializer.fromJson(serializedComponentOrPlainText);
			if (c != null) return c;
		} catch (Exception ignore) {}
		return Component.literal(serializedComponentOrPlainText);
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

	//TODO übernehmen
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
