package de.keksuccino.fancymenu.util.rendering.entity;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public class WrappedFancyPlayerWidget extends AbstractWidget implements NavigatableWidget {

    private static final String FER_PLAYER_WIDGET_CLASS = "it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget";

    private boolean focusable = true;
    private boolean navigatable = true;

    @NotNull
    public static WrappedFancyPlayerWidget build(int x, int y, int width, int height) throws ReflectiveOperationException {
        return new WrappedFancyPlayerWidget(x, y, width, height);
    }

    @NotNull
    protected final Object wrapped;

    protected WrappedFancyPlayerWidget(int x, int y, int width, int height) throws ReflectiveOperationException {
        super(x, y, width, height, Component.empty());
        Class<?> widgetClass = Class.forName(FER_PLAYER_WIDGET_CLASS);
        Constructor<?> constructor = widgetClass.getConstructor(int.class, int.class, int.class, int.class);
        this.wrapped = constructor.newInstance(x, y, width, height);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.invoke("render", graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        this.invoke("updateNarration", output);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.invoke("setX", x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.invoke("setY", y);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.invoke("setWidth", width);
    }

    public void setHeight(int height) {
        this.height = height;
        this.invoke("setHeight", height);
    }

    public void setSize(int width, int height) {
        this.setWidth(width);
        this.height = height;
        this.invoke("setSize", width, height);
    }

    @Override
    public boolean isFocusable() {
        return this.focusable;
    }

    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    public boolean isNavigatable() {
        return this.navigatable;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    public WrappedFancyPlayerWidget setBodyFollowsMouse(boolean followsMouse) {
        this.invoke("setBodyFollowsMouse", followsMouse);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadFollowsMouse(boolean followsMouse) {
        this.invoke("setHeadFollowsMouse", followsMouse);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadRotation(float x, float y, float z) {
        this.invoke("setHeadRotation", x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setBodyRotation(float x, float y, float z) {
        this.invoke("setBodyRotation", x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftArmRotation(float x, float y, float z) {
        this.invoke("setLeftArmRotation", x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setRightArmRotation(float x, float y, float z) {
        this.invoke("setRightArmRotation", x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftLegRotation(float x, float y, float z) {
        this.invoke("setLeftLegRotation", x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setRightLegRotation(float x, float y, float z) {
        this.invoke("setRightLegRotation", x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setSlim(boolean isSlim) {
        this.invoke("setSlim", isSlim);
        return this;
    }

    public WrappedFancyPlayerWidget setSkin(@NotNull ResourceLocation skin, @Nullable ResourceLocation cape, boolean slim) {
        this.invoke("setSkin", skin, cape, slim);
        return this;
    }

    public WrappedFancyPlayerWidget copyLocalPlayer() {
        this.invoke("copyLocalPlayer");
        return this;
    }

    public WrappedFancyPlayerWidget setName(String name) {
        this.invoke("setName", name);
        return this;
    }

    public WrappedFancyPlayerWidget setPinName(boolean pin) {
        this.invoke("setPinName", pin);
        return this;
    }

    public WrappedFancyPlayerWidget setShowName(boolean showName) {
        this.invoke("setShowName", showName);
        return this;
    }

    public WrappedFancyPlayerWidget setShowCape(boolean showCape) {
        this.invoke("setShowCape", showCape);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftArm(boolean showLeftArm) {
        this.invoke("setShowLeftArm", showLeftArm);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftSleeve(boolean showLeftSleeve) {
        this.invoke("setShowLeftSleeve", showLeftSleeve);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightArm(boolean showRightArm) {
        this.invoke("setShowRightArm", showRightArm);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightSleeve(boolean showRightSleeve) {
        this.invoke("setShowRightSleeve", showRightSleeve);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftLeg(boolean showLeftLeg) {
        this.invoke("setShowLeftLeg", showLeftLeg);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftPants(boolean showLeftPants) {
        this.invoke("setShowLeftPants", showLeftPants);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightLeg(boolean showRightLeg) {
        this.invoke("setShowRightLeg", showRightLeg);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightPants(boolean showRightPants) {
        this.invoke("setShowRightPants", showRightPants);
        return this;
    }

    public WrappedFancyPlayerWidget setShowHead(boolean showHead) {
        this.invoke("setShowHead", showHead);
        return this;
    }

    public WrappedFancyPlayerWidget setShowHat(boolean showHat) {
        this.invoke("setShowHat", showHat);
        return this;
    }

    public WrappedFancyPlayerWidget setShowBody(boolean showBody) {
        this.invoke("setShowBody", showBody);
        return this;
    }

    public WrappedFancyPlayerWidget setShowJacket(boolean showJacket) {
        this.invoke("setShowJacket", showJacket);
        return this;
    }

    public WrappedFancyPlayerWidget setPose(@NotNull Pose pose) {
        this.invoke("setPose", pose);
        return this;
    }

    public WrappedFancyPlayerWidget setBaby(boolean isBaby) {
        this.invoke("setBaby", isBaby);
        return this;
    }

    public WrappedFancyPlayerWidget setParrots(@Nullable Parrot.Variant leftParrot, @Nullable Parrot.Variant rightParrot) {
        this.invoke("setParrots", leftParrot, rightParrot);
        return this;
    }

    public WrappedFancyPlayerWidget setBodyMovement(boolean shouldMove) {
        this.invoke("setBodyMovement", shouldMove);
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable Item item) {
        this.invoke("setRightHandItem", item);
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable ItemStack item) {
        this.invoke("setRightHandItem", item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable Item item) {
        this.invoke("setLeftHandItem", item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable ItemStack item) {
        this.invoke("setLeftHandItem", item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable Item item) {
        this.invoke("setHeadWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable ItemStack item) {
        this.invoke("setHeadWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable Item item) {
        this.invoke("setChestWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable ItemStack item) {
        this.invoke("setChestWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable Item item) {
        this.invoke("setLegsWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable ItemStack item) {
        this.invoke("setLegsWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable Item item) {
        this.invoke("setFeetWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable ItemStack item) {
        this.invoke("setFeetWearable", item);
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(String profileName) {
        this.invoke("copyPlayer", profileName);
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(UUID profileId) {
        this.invoke("copyPlayer", profileId);
        return this;
    }

    public WrappedFancyPlayerWidget uncopyPlayer() {
        this.invoke("uncopyPlayer");
        return this;
    }

    private void invoke(@NotNull String methodName, Object... args) {
        try {
            Method method = this.findMethod(methodName, args);
            if (method != null) {
                method.setAccessible(true);
                method.invoke(this.wrapped, args);
            }
        } catch (Throwable ignored) {}
    }

    @Nullable
    private Method findMethod(@NotNull String methodName, Object... args) {
        for (Method method : this.wrapped.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || (method.getParameterCount() != args.length)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                Object arg = args[i];
                if ((arg != null) && !wrap(parameterTypes[i]).isAssignableFrom(arg.getClass())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return method;
            }
        }
        return null;
    }

    @NotNull
    private static Class<?> wrap(@NotNull Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return Void.class;
    }

}
