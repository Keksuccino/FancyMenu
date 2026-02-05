package de.keksuccino.fancymenu.customization.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.JsonOps;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
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
import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class AbstractElement implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget, PropertyHolder {

	private static final Logger LOGGER = LogManager.getLogger();

	/** The {@link AbstractElement#builder} field is NULL for this element! Keep that in mind when using it as placeholder! **/
	@SuppressWarnings("all")
	public static final AbstractElement EMPTY_ELEMENT = new AbstractElement(null){public void render(@NotNull GuiGraphics g, int i1, int i2, float f){}};
	public static final int STAY_ON_SCREEN_EDGE_ZONE_SIZE = 2;

    private final Map<String, Property<?>> propertyMap = new LinkedHashMap<>();
	protected final ElementBuilder<?,?> builder;
    private String instanceIdentifier;

	public ElementAnchorPoint anchorPoint = ElementAnchorPoints.MID_CENTERED;
    /** X-offset from the element's origin/anchor **/
	public int posOffsetX = 0;
    /** Y-offset from the element's origin/anchor **/
	public int posOffsetY = 0;
	public int baseWidth = 0;
	public int baseHeight = 0;
	public boolean stayOnScreen = true;
	public volatile boolean visible = true;
	public volatile AppearanceDelay appearanceDelay = AppearanceDelay.NO_DELAY;
	public long appearanceDelayEndTime = -1;
	public volatile DisappearanceDelay disappearanceDelay = DisappearanceDelay.NO_DELAY;
	public long disappearanceDelayEndTime = -1;
	@NotNull
	public Fading fadeIn = Fading.NO_FADING;
	@NotNull
	public Fading fadeOut = Fading.NO_FADING;
	public boolean shouldDoFadeInIfNeeded = false;
	public boolean fadeInStarted = false;
	public boolean fadeInFinished = false;
	public boolean shouldDoFadeOutIfNeeded = false;
	public boolean fadeOutStarted = false;
	public boolean fadeOutFinished = false;
	public long lastFadeInTick = -1;
	public long lastFadeOutTick = -1;
	public float opacity = 1.0F;
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
	public boolean disappearanceDelayElementJustCreated = true;
	public boolean lastTickDisappearanceDelayed = false;
	public boolean lastTickRawShouldRender = false;
	public boolean lastTickShouldRender = false;
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
	public RequirementContainer requirementContainer = new RequirementContainer();
	@Nullable
	public String customElementLayerName = null;
	public boolean enableParallax = false;
	public boolean invertParallax = false;
	public boolean loadOncePerSession = false;
	public boolean layerHiddenInEditor = false;
	/** The rotation angle in degrees. 0 = no rotation, positive values rotate clockwise */
	public boolean advancedRotationMode = false;
	/** The vertical tilt angle in degrees. */
	/** The horizontal tilt angle in degrees. */
	public boolean advancedVerticalTiltMode = false;
	public boolean advancedHorizontalTiltMode = false;

    /**
     * This is for when the render scale was changed in a non-system-wide way like via {@link PoseStack#translate(float, float, float)}.<br>
     * Elements that do not support scaling via {@link PoseStack#translate(float, float, float)} need to use this value to manually scale themselves.<br>
     * This value is -1F by default and is not always set to an actual scale, so check this before using it!
     **/
    public float customGuiScale = -1F;

    protected String anchorPointElementIdentifier = null;
    protected AbstractElement cachedElementAnchorPointParent = null;
    @Nullable
    protected Layout parentLayout;
    @Nullable
    protected RuntimePropertyContainer cachedMemory;
    protected int cachedMouseX = 0;
    protected int cachedMouseY = 0;
    protected float lastParallaxIntensityX = -10000.0F;
    protected float lastParallaxIntensityY = -10000.0F;
    protected boolean allowDepthTestManipulation = false;
    /** Whether this element type supports rotation. Can be overridden in subclasses. */
    protected boolean supportsRotation = true;
    /** Whether this element type supports tilting. Can be overridden in subclasses. */
    protected boolean supportsTilting = true;

    public final Property<Boolean> shouldBeAffectedByDecorationOverlays = putProperty(Property.booleanProperty("should_be_affected_by_decoration_overlays", false, "fancymenu.elements.abstract.should_be_affected_by_decoration_overlays"));
    public final Property.ColorProperty inEditorColor = putProperty(Property.hexColorProperty("in_editor_color", DrawableColor.of(Color.ORANGE).getHex(), false, "fancymenu.elements.in_editor_color"));
    public final Property.IntegerProperty advancedX = putProperty(Property.integerProperty("advanced_posx", Integer.MIN_VALUE, "fancymenu.elements.features.advanced_positioning.posx", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.IntegerProperty advancedY = putProperty(Property.integerProperty("advanced_posy", Integer.MIN_VALUE, "fancymenu.elements.features.advanced_positioning.posy", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.IntegerProperty advancedWidth = putProperty(Property.integerProperty("advanced_width", Integer.MIN_VALUE, "fancymenu.elements.features.advanced_sizing.width", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.IntegerProperty advancedHeight = putProperty(Property.integerProperty("advanced_height", Integer.MIN_VALUE, "fancymenu.elements.features.advanced_sizing.height", Property.NumericInputBehavior.<Integer>builder().freeInput().build()));
    public final Property.BooleanProperty stretchX = putProperty(Property.booleanProperty("stretch_x", false, "fancymenu.elements.stretch.x"));
    public final Property.BooleanProperty stretchY = putProperty(Property.booleanProperty("stretch_y", false, "fancymenu.elements.stretch.y"));
    public final Property.FloatProperty appearanceDelaySeconds = putProperty(Property.floatProperty("appearance_delay_seconds", 1.0F, "fancymenu.element.general.appearance_delay.seconds"));
    public final Property.FloatProperty disappearanceDelaySeconds = putProperty(Property.floatProperty("disappearance_delay_seconds", 1.0F, "fancymenu.element.general.disappearance_delay.seconds"));
    public final Property.FloatProperty fadeInSpeed = putProperty(Property.floatProperty("fade_in_speed", 1.0F, "fancymenu.element.fading.fade_in.speed"));
    public final Property.FloatProperty fadeOutSpeed = putProperty(Property.floatProperty("fade_out_speed", 1.0F, "fancymenu.element.fading.fade_out.speed"));
    public final Property.FloatProperty baseOpacity = putProperty(Property.floatProperty("base_opacity", 1.0F, "fancymenu.element.base_opacity"));
    public final Property.FloatProperty parallaxIntensityX = putProperty(Property.floatProperty("parallax_intensity_x", 0.5F, "fancymenu.elements.parallax.intensity_x"));
    public final Property.FloatProperty parallaxIntensityY = putProperty(Property.floatProperty("parallax_intensity_y", 0.5F, "fancymenu.elements.parallax.intensity_y"));
    public final Property.FloatProperty rotationDegrees = putProperty(Property.floatProperty("rotation_degrees", 0.0F, "fancymenu.element.rotation.degrees"));
    public final Property.FloatProperty advancedRotationDegrees = putProperty(Property.floatProperty("advanced_rotation_degrees", 0.0F, "fancymenu.element.rotation.degrees"));
    public final Property.FloatProperty verticalTiltDegrees = putProperty(Property.floatProperty("vertical_tilt_degrees", 0.0F, "fancymenu.element.tilt.vertical.degrees"));
    public final Property.FloatProperty advancedVerticalTiltDegrees = putProperty(Property.floatProperty("advanced_vertical_tilt_degrees", 0.0F, "fancymenu.element.tilt.vertical.degrees"));
    public final Property.FloatProperty horizontalTiltDegrees = putProperty(Property.floatProperty("horizontal_tilt_degrees", 0.0F, "fancymenu.element.tilt.horizontal.degrees"));
    public final Property.FloatProperty advancedHorizontalTiltDegrees = putProperty(Property.floatProperty("advanced_horizontal_tilt_degrees", 0.0F, "fancymenu.element.tilt.horizontal.degrees"));

	@SuppressWarnings("all")
	public AbstractElement(@NotNull ElementBuilder<?,?> builder) {
		this.builder = builder;
		this.instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
	}

    @Override
    public @NotNull Map<String, Property<?>> getPropertyMap() {
        return this.propertyMap;
    }

    /**
	 * Returns whether this element type supports rotation.
	 * @return true if this element can be rotated, false otherwise
	 */
	public boolean supportsRotation() {
		return this.supportsRotation;
	}

	/**
	 * Sets whether this element type supports rotation.
	 * This should typically be called in the constructor of element subclasses.
	 * @param supportsRotation true to enable rotation, false to disable it
	 */
	protected void setSupportsRotation(boolean supportsRotation) {
		this.supportsRotation = supportsRotation;
	}

	/**
	 * Returns whether this element type supports tilting.
	 * @return true if this element can be tilted, false otherwise
	 */
	public boolean supportsTilting() {
		return this.supportsTilting;
	}

	/**
	 * Sets whether this element type supports tilting.
	 * This should typically be called in the constructor of element subclasses.
	 * @param supportsTilting true to enable tilting, false to disable it
	 */
	protected void setSupportsTilting(boolean supportsTilting) {
		this.supportsTilting = supportsTilting;
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

		if (this.allowDepthTestManipulation) {
			RenderSystem.disableDepthTest();
			RenderingUtils.setDepthTestLocked(true);
		}

		this.cachedMouseX = mouseX;
		this.cachedMouseY = mouseY;

		this.lastParallaxIntensityX = this.parallaxIntensityX.getFloat();
		this.lastParallaxIntensityY = this.parallaxIntensityY.getFloat();

		this.tickBaseOpacity();

		boolean rawShouldRender = this._shouldRender();
		boolean willStartFadeOutThisTick = !isEditor()
				&& !rawShouldRender
				&& !this.isDisappearanceDelayed()
				&& !this.becameInvisible
				&& (this.fadeOut != Fading.NO_FADING)
				&& !this.fadeOutElementJustCreated;

		// Apply transformations if needed
		boolean transformationsApplied = false;
		float rotDegrees = this.getRotationDegrees();
		float verticalTilt = this.getVerticalTiltDegrees();
		float horizontalTilt = this.getHorizontalTiltDegrees();
		boolean hasRotation = this.supportsRotation && (rotDegrees != 0.0F);
		boolean hasTilt = this.supportsTilting && (verticalTilt != 0.0F || horizontalTilt != 0.0F);

		if ((this.shouldRender() || willStartFadeOutThisTick) && (hasRotation || hasTilt)) {

			graphics.pose().pushPose();
			transformationsApplied = true;

			// Calculate center point of the element
			float centerX = this.getAbsoluteX() + (this.getAbsoluteWidth() / 2.0F);
			float centerY = this.getAbsoluteY() + (this.getAbsoluteHeight() / 2.0F);

			// Translate to center
			graphics.pose().translate(centerX, centerY, 0);

			// Apply tilting first (before rotation)
			if (hasTilt) {
				// Apply vertical tilt (rotation around X axis)
				if (verticalTilt != 0.0F) {
					graphics.pose().mulPose(Axis.XP.rotationDegrees(verticalTilt));
				}
				// Apply horizontal tilt (rotation around Y axis)
				if (horizontalTilt != 0.0F) {
					graphics.pose().mulPose(Axis.YP.rotationDegrees(horizontalTilt));
				}
			}

			// Apply rotation (around Z axis)
			if (hasRotation) {
				graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotDegrees));
			}

			// Translate back
			graphics.pose().translate(-centerX, -centerY, 0);

		}

		this.renderTick_Head();

		this.tickVisibleInvisible();

		this.renderTick_Inner_Stage_1();

		if (!isEditor()) {

			this.tickAppearanceDelay(this.shouldRender());
			this.tickDisappearanceDelay();

			this.tickFadeInOut(this.shouldRender());

		}

		this.renderTick_Inner_Stage_2();

		if (this.shouldRender()) {
			//Render the actual element
			this.render(graphics, mouseX, mouseY, partial);
		}

		this.renderTick_Tail();

		// Pop the transformations
		if (transformationsApplied) {
			graphics.pose().popPose();
		}

		this.lastTickShouldRender = this.shouldRender();

		if (this.allowDepthTestManipulation) {
			RenderingUtils.setDepthTestLocked(false);
			RenderSystem.enableDepthTest();
		}

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

		boolean rawShouldRender = this._shouldRender();

		if (!rawShouldRender) {
			if (this.lastTickRawShouldRender && this.lastTickShouldRender) {
				this.applyDisappearanceDelay();
			}
		} else if (this.disappearanceDelayEndTime != -1) {
			this.disappearanceDelayEndTime = -1;
			this.lastTickDisappearanceDelayed = false;
		}

		this.lastTickRawShouldRender = rawShouldRender;

		boolean effectiveVisible = rawShouldRender || this.isDisappearanceDelayed();

		if (!effectiveVisible) {
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

	public void tickDisappearanceDelay() {

		if (this.lastTickDisappearanceDelayed && !this.isDisappearanceDelayed() && !this.lastTickRawShouldRender) {
			this.getMemory().putProperty("disappearance_delay_applied", true);
		}
		this.lastTickDisappearanceDelayed = this.isDisappearanceDelayed();

	}

	public void tickFadeInOut(boolean shouldRender) {

		if (shouldRender) {

			//Handle fade-in
			boolean fadeInIsResize = !this.isNewMenu && this.fadeInElementJustCreated;
			boolean fadeInDone = this.getMemory().putPropertyIfAbsentAndGet("fade_in_done", false);
			if (!fadeInIsResize || !fadeInDone) {
				if ((this.fadeIn != Fading.NO_FADING) && this.shouldDoFadeInIfNeeded && (this.lastBaseOpacity > 0.0F)) {
					// Only start fade-in once
					if ((this.fadeIn != Fading.FIRST_TIME) || !this.getMemory().putPropertyIfAbsentAndGet("fade_in_done", false)) {
						if (!this.fadeInStarted) {
							this.fadeInStarted = true;
							this.opacity = 0.02F;
							this.lastFadeInTick = System.currentTimeMillis(); // initialize timer
						}
						long now = System.currentTimeMillis();
						float elapsedSeconds = (now - this.lastFadeInTick) / 1000.0F;
						this.lastFadeInTick = now;
						// Increase opacity based on elapsed time; 0.4 is the base rate for fadeSpeed = 1.
						float fadeInSpeed = Math.max(0.0F, this.fadeInSpeed.getFloat());
						this.opacity += elapsedSeconds * (0.4F * fadeInSpeed);
						this.opacity = Math.max(0.02F, this.opacity);
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
						this.lastFadeOutTick = System.currentTimeMillis(); // initialize timer
					}
					long now = System.currentTimeMillis();
					float elapsedSeconds = (now - this.lastFadeOutTick) / 1000.0F;
					this.lastFadeOutTick = now;
					// Decrease opacity based on elapsed time.
					float fadeOutSpeed = Math.max(0.0F, this.fadeOutSpeed.getFloat());
					this.opacity -= elapsedSeconds * (0.4F * fadeOutSpeed);
					this.opacity = Math.max(0.02F, this.opacity);
					if (this.opacity <= 0.02F) {
						this.opacity = 0.02F;
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
		this.fadeOutElementJustCreated = false;

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
		float delaySeconds = Math.max(0.0F, this.appearanceDelaySeconds.getFloat());
		if ((!isResize || !applied) && (this.appearanceDelay != AppearanceDelay.NO_DELAY) && (delaySeconds > 0.0F)) {
			if ((this.appearanceDelay == AppearanceDelay.FIRST_TIME) && applied) {
				this.appearanceDelayEndTime = -1;
			} else {
				this.appearanceDelayEndTime = System.currentTimeMillis() + ((long)(delaySeconds * 1000.0F));
			}
		} else {
			this.appearanceDelayEndTime = -1;
		}
		this.lastTickAppearanceDelayed = this.isAppearanceDelayed();
	}

	public void applyDisappearanceDelay() {
		boolean isResize = !this.isNewMenu && this.disappearanceDelayElementJustCreated;
		this.disappearanceDelayElementJustCreated = false;
		if (isEditor()) {
			this.disappearanceDelayEndTime = -1;
			return;
		}
		boolean applied = this.getMemory().putPropertyIfAbsentAndGet("disappearance_delay_applied", false);
		float delaySeconds = Math.max(0.0F, this.disappearanceDelaySeconds.getFloat());
		if ((!isResize || !applied) && (this.disappearanceDelay != DisappearanceDelay.NO_DELAY) && (delaySeconds > 0.0F)) {
			if ((this.disappearanceDelay == DisappearanceDelay.FIRST_TIME) && applied) {
				this.disappearanceDelayEndTime = -1;
			} else {
				this.disappearanceDelayEndTime = System.currentTimeMillis() + ((long)(delaySeconds * 1000.0F));
			}
		} else {
			this.disappearanceDelayEndTime = -1;
		}
		this.lastTickDisappearanceDelayed = this.isDisappearanceDelayed();
	}

	public void updateOpacity() {
		this.opacity = this.getBaseOpacity();
	}

	public float getBaseOpacity() {
		long now = System.currentTimeMillis();
		if ((this.lastBaseOpacityParse + 30L) > now) return this.cachedBaseOpacity;
		this.lastBaseOpacityParse = now;
		float f = this.baseOpacity.getFloat();
		if (f < 0.0F) f = 0.0F;
		if (f > 1.0F) f = 1.0F;
		this.cachedBaseOpacity = f;
		return f;
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

    public ElementBuilder<?, ?> getBuilder() {
        return builder;
    }

    public float getRotationDegrees() {
		if (!this.supportsRotation()) return 0;
		if (this.advancedRotationMode) {
			return this.advancedRotationDegrees.getFloat();
		}
		return this.rotationDegrees.getFloat();
	}

	public float getVerticalTiltDegrees() {
		if (!this.supportsTilting()) return 0;
		float value = this.advancedVerticalTiltMode ? this.advancedVerticalTiltDegrees.getFloat() : this.verticalTiltDegrees.getFloat();
		return Math.max(-60.0F, Math.min(60.0F, value));
	}

	public float getHorizontalTiltDegrees() {
		if (!this.supportsTilting()) return 0;
		float value = this.advancedHorizontalTiltMode ? this.advancedHorizontalTiltDegrees.getFloat() : this.horizontalTiltDegrees.getFloat();
		return Math.max(-60.0F, Math.min(60.0F, value));
	}

	/**
	 * Returns the actual/final X position the element will have when it gets rendered.
	 */
	public int getAbsoluteX() {

		int x = 0;
		if (this.anchorPoint != null) {
			x = this.anchorPoint.getElementPositionX(this);
		}

		if (!this.advancedX.isDefault()) {
			x = this.advancedX.getInteger();
		}

		x += this.animatedOffsetX;

		boolean applyParallax = this.enableParallax && !isEditor();

		// Apply parallax effect if enabled and not in editor
		if (applyParallax) {
			// Calculate parallax offset using cached mouse position
			float centerX = getScreenWidth() / 2f;
			float offsetX = this.cachedMouseX - centerX;
			float parallaxOffset = offsetX * this.lastParallaxIntensityX * 0.1f; // Scale factor to control maximum movement

			// Apply offset based on direction
			x += (int) (invertParallax ? parallaxOffset : -parallaxOffset);
		}

		if (this.stretchX.getBoolean()) {
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

		if (!this.advancedY.isDefault()) {
			y = this.advancedY.getInteger();
		}

		y += this.animatedOffsetY;

		boolean applyParallax = this.enableParallax && !isEditor();

		// Apply parallax effect if enabled and not in editor
		if (applyParallax) {
			// Calculate parallax offset using cached mouse position
			float centerY = getScreenHeight() / 2f;
			float offsetY = this.cachedMouseY - centerY;
			float parallaxOffset = offsetY * this.lastParallaxIntensityY * 0.1f; // Scale factor to control maximum movement

			// Apply offset based on direction
			y += (int) (invertParallax ? parallaxOffset : -parallaxOffset);
		}

		if (this.stretchY.getBoolean()) {
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

	/**
	 * Returns the actual/final width the element will have when it gets rendered.
	 */
	public int getAbsoluteWidth() {
		if (!this.advancedWidth.isDefault()) {
			return this.advancedWidth.getInteger();
		}
		if (this.stretchX.getBoolean()) {
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
		if (!this.advancedHeight.isDefault()) {
			return this.advancedHeight.getInteger();
		}
		if (this.stretchY.getBoolean()) {
			return getScreenHeight();
		}
		this.updateAutoSizing(false);
		if (this.autoSizing && (this.autoSizingHeight > 0)) {
			return this.autoSizingHeight;
		}
		return this.baseHeight;
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

	@Nullable
	public String getAnchorPointElementIdentifier() {
		return this.anchorPointElementIdentifier;
	}

	public void setAnchorPointElementIdentifier(@Nullable String anchorPointElementIdentifier) {

		// Handle null or empty string - clear the anchor
		if (anchorPointElementIdentifier == null || anchorPointElementIdentifier.trim().isEmpty()) {
			this.anchorPointElementIdentifier = null;
			this.cachedElementAnchorPointParent = null;
			return;
		}

		// Normalize the identifier (remove potential prefixes)
		String normalizedIdentifier = anchorPointElementIdentifier
				.replace("vanillabtn:", "")
				.replace("button_compatibility_id:", "");

		// Check for self-anchoring
		if (this.getInstanceIdentifier().equals(normalizedIdentifier)) {
			this.resetToDefaultAnchor();
			LOGGER.error("[FANCYMENU] Tried to anchor element to itself! (Element: " + this.getInstanceIdentifier() + ")",
					new IllegalStateException("Anchoring element to itself"));
			return;
		}

		// Try to get the target parent element (it might not exist yet, which is valid)
		AbstractElement parent = getElementByInstanceIdentifier(normalizedIdentifier);

		// Only check for circular dependencies if parent exists
		// If parent is null, it might be created later, which is a valid scenario
		if (parent != null) {
			// Check for circular dependencies (both direct and transitive)
			if (detectCircularDependency(this, parent)) {
				this.resetToDefaultAnchor();
				LOGGER.error("[FANCYMENU] Detected circular anchor dependency! Cannot anchor '" +
								this.getInstanceIdentifier() + "' to '" + normalizedIdentifier +
								"' as it would create a circular reference chain.",
						new IllegalStateException("Circular anchor dependency detected"));
				return;
			}
		}

		// Set the anchor identifier (parent might be null and resolved later)
		this.anchorPointElementIdentifier = normalizedIdentifier;
		this.cachedElementAnchorPointParent = parent;
	}

	/**
	 * Detects circular dependencies in the anchor chain.
	 * @param child The element being anchored
	 * @param proposedParent The element to anchor to
	 * @return true if adding this anchor would create a circular dependency
	 */
	protected boolean detectCircularDependency(AbstractElement child, AbstractElement proposedParent) {
		// Set a reasonable depth limit to prevent infinite loops
		final int MAX_DEPTH = 100;
		int depth = 0;

		Set<String> visitedElements = new HashSet<>();
		visitedElements.add(child.getInstanceIdentifier());

		AbstractElement current = proposedParent;
		while (current != null && depth < MAX_DEPTH) {
			String currentId = current.getInstanceIdentifier();

			// Check if we've encountered the child element in the chain
			if (visitedElements.contains(currentId)) {
				return true; // Circular dependency detected
			}

			visitedElements.add(currentId);

			// Move up the anchor chain
			String parentId = current.getAnchorPointElementIdentifier();
			if (parentId == null || parentId.trim().isEmpty()) {
				break; // Reached the top of the chain
			}

			// Normalize the parent ID
			parentId = parentId.replace("vanillabtn:", "").replace("button_compatibility_id:", "");

			current = getElementByInstanceIdentifier(parentId);
			depth++;
		}

		// Check if we hit the depth limit (which might indicate an issue)
		if (depth >= MAX_DEPTH) {
			LOGGER.warn("[FANCYMENU] Anchor chain depth exceeded " + MAX_DEPTH +
					" levels while checking for circular dependencies. This might indicate a problem.");
			return true; // Treat as circular to be safe
		}

		return false; // No circular dependency found
	}

	/**
	 * Resets the element's anchor to default values.
	 */
	public void resetToDefaultAnchor() {
		this.anchorPointElementIdentifier = null;
		this.cachedElementAnchorPointParent = null;
		this.anchorPoint = ElementAnchorPoints.MID_CENTERED;
		this.posOffsetX = 0;
		this.posOffsetY = 0;
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
		if (this.anchorPointElementIdentifier == null) element = null;
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

	public boolean isDisappearanceDelayed() {
		return (System.currentTimeMillis() < this.disappearanceDelayEndTime);
	}

	public boolean shouldRender() {

		if (!isEditor() && this.loadOncePerSession && this.shouldHideOncePerSessionElement()) return false;

		if (this.isAppearanceDelayed() && !isEditor()) return false;

		boolean b = this._shouldRender();
		if (!isEditor()) {
			if (!b && this.isDisappearanceDelayed()) return true;
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
		return this.requirementContainer.requirementsMet();
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
		return (LayoutEditorScreen.getCurrentInstance() != null);
	}

	@Nullable
	public static Screen getScreen() {
		if (LayoutEditorScreen.getCurrentInstance() != null) return LayoutEditorScreen.getCurrentInstance();
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
		if (LayoutEditorScreen.getCurrentInstance() != null) {
			AbstractEditorElement editorElement = LayoutEditorScreen.getCurrentInstance().getElementByInstanceIdentifier(identifier);
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
		if (!serializedComponentOrPlainText.startsWith("{") && !serializedComponentOrPlainText.startsWith("[")) return Component.literal(serializedComponentOrPlainText);
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

	public enum DisappearanceDelay {

		NO_DELAY("no_delay"),
		FIRST_TIME("first_time"),
		EVERY_TIME("every_time");

		public final String name;

		DisappearanceDelay(String name) {
			this.name = name;
		}

		@Nullable
		public static DisappearanceDelay getByName(@NotNull String name) {
			for (DisappearanceDelay d : DisappearanceDelay.values()) {
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
