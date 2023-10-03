package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.util.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class ButtonElementBuilder extends ElementBuilder<ButtonElement, ButtonEditorElement> {

    public ButtonElementBuilder() {
        super("custom_button");
    }

    @Override
    public @NotNull ButtonElement buildDefaultInstance() {
        ButtonElement element = new ButtonElement(this);
        element.baseWidth = 100;
        element.baseHeight = 20;
        element.label = "New Button";
        element.button = new ExtendedButton(0, 0, 0, 0, Component.empty(), (press) -> {
            if ((CustomizationOverlay.getCurrentMenuBarInstance() == null) || !CustomizationOverlay.getCurrentMenuBarInstance().isUserNavigatingInMenuBar()) {
                element.getExecutableBlock().execute();
            }
        });
//        ((ExtendedButton)element.button).setFocusable(false);
//        ((ExtendedButton)element.button).setNavigatable(false);
        return element;
    }

    @Override
    public @NotNull ButtonElement deserializeElement(@NotNull SerializedElement serialized) {

        ButtonElement element = buildDefaultInstance();

        element.label = serialized.getValue("label");

        String buttonExecutableBlockId = serialized.getValue("button_element_executable_block_identifier");
        if (buttonExecutableBlockId != null) {
            AbstractExecutableBlock b = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, buttonExecutableBlockId);
            if (b instanceof GenericExecutableBlock g) {
                element.actionExecutor = g;
            }
        } else {
            //Legacy support for old button action format
            GenericExecutableBlock g = new GenericExecutableBlock();
            g.getExecutables().addAll(ActionInstance.deserializeAll(serialized));
            element.actionExecutor = g;
        }

        element.hoverSound = serialized.getValue("hoversound");
        if (element.hoverSound != null) {
            File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(element.hoverSound));
            if (f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
                SoundRegistry.registerSound(element.hoverSound, element.hoverSound);
            } else {
                element.hoverSound = null;
            }
        }

        element.hoverLabel = serialized.getValue("hoverlabel");

        element.tooltip = serialized.getValue("description");

        element.button = new ExtendedButton(0, 0, 0, 0, Component.literal(""), (press) -> {
            element.getExecutableBlock().execute();
        });
//        ((ExtendedButton)element.button).setFocusable(false);
//        ((ExtendedButton)element.button).setNavigatable(false);

        element.clickSound = serialized.getValue("clicksound");
        if (element.clickSound != null) {
            File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(element.clickSound));
            if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
                SoundHandler.registerSound(f.getPath(), f.getPath());
            } else {
                element.clickSound = null;
            }
        }

        element.backgroundTextureNormal = serialized.getValue("backgroundnormal");

        element.backgroundTextureHover = serialized.getValue("backgroundhovered");

        element.backgroundTextureInactive = serialized.getValue("background_texture_inactive");

        String loopBackAnimations = serialized.getValue("loopbackgroundanimations");
        if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
            element.loopBackgroundAnimations = false;
        }

        String restartBackAnimationsOnHover = serialized.getValue("restartbackgroundanimations");
        if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
            element.restartBackgroundAnimationsOnHover = false;
        }

        element.backgroundAnimationNormal = serialized.getValue("backgroundanimationnormal");

        element.backgroundAnimationHover = serialized.getValue("backgroundanimationhovered");

        element.backgroundAnimationInactive = serialized.getValue("background_animation_inactive");

        return element;

    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull ButtonElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("button_element_executable_block_identifier", element.actionExecutor.identifier);
        element.actionExecutor.serializeToExistingPropertyContainer(serializeTo);

        if (element.backgroundTextureNormal != null) {
            serializeTo.putProperty("backgroundnormal", element.backgroundTextureNormal);
        }
        if (element.backgroundAnimationNormal != null) {
            serializeTo.putProperty("backgroundanimationnormal", element.backgroundAnimationNormal);
        }
        if (element.backgroundTextureHover != null) {
            serializeTo.putProperty("backgroundhovered", element.backgroundTextureHover);
        }
        if (element.backgroundAnimationHover != null) {
            serializeTo.putProperty("backgroundanimationhovered", element.backgroundAnimationHover);
        }
        if (element.backgroundTextureInactive != null) {
            serializeTo.putProperty("background_texture_inactive", element.backgroundTextureInactive);
        }
        if (element.backgroundAnimationInactive != null) {
            serializeTo.putProperty("background_animation_inactive", element.backgroundAnimationInactive);
        }
        serializeTo.putProperty("restartbackgroundanimations", "" + element.restartBackgroundAnimationsOnHover);
        serializeTo.putProperty("loopbackgroundanimations", "" + element.loopBackgroundAnimations);
        if (element.hoverSound != null) {
            serializeTo.putProperty("hoversound", element.hoverSound);
        }
        if (element.hoverLabel != null) {
            serializeTo.putProperty("hoverlabel", element.hoverLabel);
        }
        if (element.clickSound != null) {
            serializeTo.putProperty("clicksound", element.clickSound);
        }
        if (element.tooltip != null) {
            serializeTo.putProperty("description", element.tooltip);
        }
        if (element.label != null) {
            serializeTo.putProperty("label", element.label);
        }

        return serializeTo;

    }

    @Override
    public @NotNull ButtonEditorElement wrapIntoEditorElement(@NotNull ButtonElement element, @NotNull LayoutEditorScreen editor) {
        return new ButtonEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        if ((element instanceof ButtonElement b) && !b.getButton().getMessage().getString().replace(" ", "").isEmpty()) {
            return b.getButton().getMessage();
        }
        return Component.translatable("fancymenu.editor.add.button");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}
