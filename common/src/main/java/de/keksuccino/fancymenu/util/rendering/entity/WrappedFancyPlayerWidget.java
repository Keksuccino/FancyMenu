package de.keksuccino.fancymenu.util.rendering.entity;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public class WrappedFancyPlayerWidget extends AbstractWidget implements NavigatableWidget {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FANCY_PLAYER_WIDGET_CLASS_NAME = "it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget";
    private static final String ROTATION_CLASS_NAME = "it.crystalnest.fancy_entity_renderer.api.Rotation";
    private static final String RENDER_MODE_CLASS_NAME = "it.crystalnest.fancy_entity_renderer.api.entity.RenderMode";
    private static final Set<String> LOGGED_FAILURES = ConcurrentHashMap.newKeySet();
    private static final Map<String, Optional<Class<?>>> RESOLVED_CLASSES = new ConcurrentHashMap<>();
    private static final Map<MethodKey, Optional<Method>> WRAPPED_METHODS = new ConcurrentHashMap<>();

    @Nullable
    private static volatile Constructor<?> wrappedConstructor;
    private static volatile boolean wrappedConstructorResolved = false;

    @Nullable
    protected final Object wrapped;
    protected volatile boolean available;

    @NotNull
    public static WrappedFancyPlayerWidget build(int x, int y, int width, int height) {
        return new WrappedFancyPlayerWidget(x, y, width, height);
    }

    protected WrappedFancyPlayerWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.wrapped = createWrapped(x, y, width, height);
        this.available = this.wrapped != null;
    }

    @Nullable
    private static Object createWrapped(int x, int y, int width, int height) {
        if (!FancyEntityRendererUtils.isFerLoaded()) {
            return null;
        }
        Constructor<?> constructor = getWrappedConstructor();
        if (constructor == null) {
            return null;
        }
        try {
            return constructor.newInstance(x, y, width, height);
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            logFailureOnce("construct", "[FANCYMENU] Failed to construct FancyPlayerWidget reflectively!", ex);
            return null;
        }
    }

    @Nullable
    private static Constructor<?> getWrappedConstructor() {
        if (wrappedConstructorResolved) {
            return wrappedConstructor;
        }
        synchronized (WrappedFancyPlayerWidget.class) {
            if (wrappedConstructorResolved) {
                return wrappedConstructor;
            }
            Class<?> wrappedClass = resolveClass(FANCY_PLAYER_WIDGET_CLASS_NAME);
            if (wrappedClass != null) {
                try {
                    wrappedConstructor = wrappedClass.getConstructor(int.class, int.class, int.class, int.class);
                } catch (ReflectiveOperationException ex) {
                    logFailureOnce("constructor", "[FANCYMENU] Failed to resolve FancyPlayerWidget constructor reflectively!", ex);
                }
            }
            wrappedConstructorResolved = true;
            return wrappedConstructor;
        }
    }

    @Nullable
    private static Class<?> resolveClass(@NotNull String className) {
        return RESOLVED_CLASSES.computeIfAbsent(className, ignored -> {
            try {
                return Optional.of(Class.forName(className, false, WrappedFancyPlayerWidget.class.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError ex) {
                if (FancyEntityRendererUtils.isFerLoaded()) {
                    logFailureOnce("class:" + className, "[FANCYMENU] Failed to resolve Fancy Entity Renderer class '" + className + "' reflectively!", ex);
                }
                return Optional.empty();
            }
        }).orElse(null);
    }

    @Nullable
    private static Class<?> getRotationClass() {
        return resolveClass(ROTATION_CLASS_NAME);
    }

    @Nullable
    private static Class<?> getRenderModeClass() {
        return resolveClass(RENDER_MODE_CLASS_NAME);
    }

    @Nullable
    private Method getWrappedMethod(@NotNull String methodName, @NotNull Class<?>... parameterTypes) {
        MethodKey key = new MethodKey(methodName, List.of(parameterTypes));
        return WRAPPED_METHODS.computeIfAbsent(key, ignored -> {
            try {
                return Optional.of(this.wrapped.getClass().getMethod(methodName, parameterTypes));
            } catch (ReflectiveOperationException ex) {
                logFailureOnce("method:" + key, "[FANCYMENU] Failed to resolve FancyPlayerWidget method '" + methodName + "' reflectively!", ex);
                return Optional.empty();
            }
        }).orElse(null);
    }

    @Nullable
    private Object invokeWrapped(@NotNull String methodName, @NotNull Class<?>[] parameterTypes, @NotNull Object... args) {
        if (!this.available || (this.wrapped == null)) {
            return null;
        }
        Method method = this.getWrappedMethod(methodName, parameterTypes);
        if (method == null) {
            this.available = false;
            return null;
        }
        try {
            return method.invoke(this.wrapped, args);
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            this.available = false;
            logFailureOnce("invoke:" + methodName + ":" + List.of(parameterTypes), "[FANCYMENU] Failed to invoke FancyPlayerWidget method '" + methodName + "' reflectively!", ex);
            return null;
        }
    }

    private static void logFailureOnce(@NotNull String key, @NotNull String message, @NotNull Throwable ex) {
        if (LOGGED_FAILURES.add(key)) {
            LOGGER.error(message, ex);
        }
    }

    public boolean isAvailable() {
        return this.available && (this.wrapped != null);
    }

    private void invokeWrappedVoid(@NotNull String methodName) {
        this.invokeWrapped(methodName, new Class<?>[0]);
    }

    private void invokeWrappedVoid(@NotNull String methodName, @NotNull Class<?> parameterType, @Nullable Object arg) {
        this.invokeWrapped(methodName, new Class<?>[]{parameterType}, arg);
    }

    private void invokeWrappedVoid(@NotNull String methodName, @NotNull Class<?> firstParameterType, @Nullable Object firstArg, @NotNull Class<?> secondParameterType, @Nullable Object secondArg) {
        this.invokeWrapped(methodName, new Class<?>[]{firstParameterType, secondParameterType}, firstArg, secondArg);
    }

    private void invokeWrappedVoid(@NotNull String methodName, @NotNull Class<?> firstParameterType, @Nullable Object firstArg, @NotNull Class<?> secondParameterType, @Nullable Object secondArg, @NotNull Class<?> thirdParameterType, @Nullable Object thirdArg) {
        this.invokeWrapped(methodName, new Class<?>[]{firstParameterType, secondParameterType, thirdParameterType}, firstArg, secondArg, thirdArg);
    }

    private void invokeWrappedVoid(@NotNull String methodName, @NotNull Class<?> firstParameterType, @Nullable Object firstArg, @NotNull Class<?> secondParameterType, @Nullable Object secondArg, @NotNull Class<?> thirdParameterType, @Nullable Object thirdArg, @NotNull Class<?> fourthParameterType, @Nullable Object fourthArg) {
        this.invokeWrapped(methodName, new Class<?>[]{firstParameterType, secondParameterType, thirdParameterType, fourthParameterType}, firstArg, secondArg, thirdArg, fourthArg);
    }

    private boolean invokeWrappedBoolean(@NotNull String methodName) {
        Object result = this.invokeWrapped(methodName, new Class<?>[0]);
        return result instanceof Boolean b && b;
    }

    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.invokeWrappedVoid("extractRenderState", GuiGraphicsExtractor.class, graphics, int.class, mouseX, int.class, mouseY, float.class, partial);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        this.invokeWrappedVoid("updateNarration", NarrationElementOutput.class, output);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.invokeWrappedVoid("setX", int.class, x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.invokeWrappedVoid("setY", int.class, y);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.invokeWrappedVoid("setWidth", int.class, width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.invokeWrappedVoid("setHeight", int.class, height);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.invokeWrappedVoid("setSize", int.class, width, int.class, height);
    }

    public WrappedFancyPlayerWidget setBodyFollowsMouse(boolean followsMouse) {
        this.invokeWrappedVoid("setBodyFollowsMouse", boolean.class, followsMouse);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadFollowsMouse(boolean followsMouse) {
        this.invokeWrappedVoid("setHeadFollowsMouse", boolean.class, followsMouse);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadRotation(Object rotation) {
        Class<?> rotationClass = getRotationClass();
        if (rotationClass != null) {
            this.invokeWrappedVoid("setHeadRotation", rotationClass, rotation);
        }
        return this;
    }

    public WrappedFancyPlayerWidget setHeadRotation(float x, float y, float z) {
        this.invokeWrappedVoid("setHeadRotation", float.class, x, float.class, y, float.class, z);
        return this;
    }

    public WrappedFancyPlayerWidget setBodyRotation(Object rotation) {
        Class<?> rotationClass = getRotationClass();
        if (rotationClass != null) {
            this.invokeWrappedVoid("setBodyRotation", rotationClass, rotation);
        }
        return this;
    }

    public WrappedFancyPlayerWidget setBodyRotation(float x, float y, float z) {
        this.invokeWrappedVoid("setBodyRotation", float.class, x, float.class, y, float.class, z);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftArmRotation(Object rotation) {
        Class<?> rotationClass = getRotationClass();
        if (rotationClass != null) {
            this.invokeWrappedVoid("setLeftArmRotation", rotationClass, rotation);
        }
        return this;
    }

    public WrappedFancyPlayerWidget setLeftArmRotation(float x, float y, float z) {
        this.invokeWrappedVoid("setLeftArmRotation", float.class, x, float.class, y, float.class, z);
        return this;
    }

    public WrappedFancyPlayerWidget setRightArmRotation(Object rotation) {
        Class<?> rotationClass = getRotationClass();
        if (rotationClass != null) {
            this.invokeWrappedVoid("setRightArmRotation", rotationClass, rotation);
        }
        return this;
    }

    public WrappedFancyPlayerWidget setRightArmRotation(float x, float y, float z) {
        this.invokeWrappedVoid("setRightArmRotation", float.class, x, float.class, y, float.class, z);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftLegRotation(Object rotation) {
        Class<?> rotationClass = getRotationClass();
        if (rotationClass != null) {
            this.invokeWrappedVoid("setLeftLegRotation", rotationClass, rotation);
        }
        return this;
    }

    public WrappedFancyPlayerWidget setLeftLegRotation(float x, float y, float z) {
        this.invokeWrappedVoid("setLeftLegRotation", float.class, x, float.class, y, float.class, z);
        return this;
    }

    public WrappedFancyPlayerWidget setRightLegRotation(Object rotation) {
        Class<?> rotationClass = getRotationClass();
        if (rotationClass != null) {
            this.invokeWrappedVoid("setRightLegRotation", rotationClass, rotation);
        }
        return this;
    }

    public WrappedFancyPlayerWidget setRightLegRotation(float x, float y, float z) {
        this.invokeWrappedVoid("setRightLegRotation", float.class, x, float.class, y, float.class, z);
        return this;
    }

    public WrappedFancyPlayerWidget setSlim(boolean isSlim) {
        this.invokeWrappedVoid("setSlim", boolean.class, isSlim);
        return this;
    }

    public WrappedFancyPlayerWidget setSkin(@Nullable PlayerSkin skin) {
        this.invokeWrappedVoid("setSkin", PlayerSkin.class, skin);
        return this;
    }

    public WrappedFancyPlayerWidget copyLocalPlayer() {
        this.invokeWrappedVoid("copyLocalPlayer");
        return this;
    }

    public WrappedFancyPlayerWidget setName(String name) {
        this.invokeWrappedVoid("setName", String.class, name);
        return this;
    }

    public WrappedFancyPlayerWidget setShowName(boolean showName) {
        this.invokeWrappedVoid("setShowName", boolean.class, showName);
        return this;
    }

    public WrappedFancyPlayerWidget setUpsideDown(boolean isUpsideDown) {
        this.invokeWrappedVoid("setUpsideDown", boolean.class, isUpsideDown);
        return this;
    }

    public WrappedFancyPlayerWidget setPose(@NotNull Pose pose) {
        this.invokeWrappedVoid("setPose", Pose.class, pose);
        return this;
    }

    public WrappedFancyPlayerWidget setRenderMode(@NotNull Object renderMode) {
        Class<?> renderModeClass = getRenderModeClass();
        if (renderModeClass != null) {
            this.invokeWrappedVoid("setRenderMode", renderModeClass, renderMode);
        }
        return this;
    }

    @ApiStatus.Experimental
    public WrappedFancyPlayerWidget setGlowing(int glowColor) {
        this.invokeWrappedVoid("setGlowing", int.class, glowColor);
        return this;
    }

    @ApiStatus.Experimental
    public WrappedFancyPlayerWidget setMoving(boolean isMoving) {
        this.invokeWrappedVoid("setMoving", boolean.class, isMoving);
        return this;
    }

    public WrappedFancyPlayerWidget setOnFire(boolean onFire) {
        this.invokeWrappedVoid("setOnFire", boolean.class, onFire);
        return this;
    }

    public WrappedFancyPlayerWidget setOnFire(boolean onFire, Identifier fireType) {
        this.invokeWrappedVoid("setOnFire", boolean.class, onFire, Identifier.class, fireType);
        return this;
    }

    public WrappedFancyPlayerWidget setBaby(boolean isBaby) {
        this.invokeWrappedVoid("setBaby", boolean.class, isBaby);
        return this;
    }

    public WrappedFancyPlayerWidget setParrots(@Nullable Parrot.Variant leftParrot, @Nullable Parrot.Variant rightParrot) {
        this.invokeWrappedVoid("setParrots", Parrot.Variant.class, leftParrot, Parrot.Variant.class, rightParrot);
        return this;
    }

    public WrappedFancyPlayerWidget setBodyMovement(boolean shouldMove) {
        this.invokeWrappedVoid("setMoving", boolean.class, shouldMove);
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable Item item) {
        this.invokeWrappedVoid("setRightHandItem", Item.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable ItemStack item) {
        this.invokeWrappedVoid("setRightHandItem", ItemStack.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable Item item) {
        this.invokeWrappedVoid("setLeftHandItem", Item.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable ItemStack item) {
        this.invokeWrappedVoid("setLeftHandItem", ItemStack.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.invokeWrappedVoid("setHeadWearable", String.class, item, HolderLookup.Provider.class, provider);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.invokeWrappedVoid("setChestWearable", String.class, item, HolderLookup.Provider.class, provider);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.invokeWrappedVoid("setLegsWearable", String.class, item, HolderLookup.Provider.class, provider);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.invokeWrappedVoid("setFeetWearable", String.class, item, HolderLookup.Provider.class, provider);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable Item item) {
        this.invokeWrappedVoid("setHeadWearable", Item.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable Item item) {
        this.invokeWrappedVoid("setChestWearable", Item.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable Item item) {
        this.invokeWrappedVoid("setLegsWearable", Item.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable Item item) {
        this.invokeWrappedVoid("setFeetWearable", Item.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable ItemStack item) {
        this.invokeWrappedVoid("setHeadWearable", ItemStack.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable ItemStack item) {
        this.invokeWrappedVoid("setChestWearable", ItemStack.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable ItemStack item) {
        this.invokeWrappedVoid("setLegsWearable", ItemStack.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable ItemStack item) {
        this.invokeWrappedVoid("setFeetWearable", ItemStack.class, item);
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(String profileName) {
        this.invokeWrappedVoid("copyPlayer", String.class, profileName);
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(UUID profileId) {
        this.invokeWrappedVoid("copyPlayer", UUID.class, profileId);
        return this;
    }

    public WrappedFancyPlayerWidget uncopyPlayer() {
        this.invokeWrappedVoid("uncopyPlayer");
        return this;
    }

    public boolean isCopyingPlayer() {
        return this.invokeWrappedBoolean("isCopyingPlayer");
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
    }

    private record MethodKey(@NotNull String name, @NotNull List<Class<?>> parameterTypes) {}

}
