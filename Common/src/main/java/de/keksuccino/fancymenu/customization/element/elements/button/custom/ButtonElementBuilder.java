package de.keksuccino.fancymenu.customization.element.elements.button.custom;

import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.widget.ExtendedButton;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
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
        element.width = 100;
        element.height = 20;
        element.label = "New Button";
        element.button = new ExtendedButton(0, 0, 0, 0, Component.literal(""), (press) -> {
            for (ActionExecutor.ActionContainer c : element.getActionList()) {
                c.execute();
            }
        }).setAutoRegisterToScreen(!isEditor());
        return element;
    }

    @Override
    public @NotNull ButtonElement deserializeElement(@NotNull SerializedElement serialized) {

        ButtonElement element = buildDefaultInstance();

        element.label = serialized.getValue("label");

        String buttonAction = serialized.getValue("buttonaction");
        String actionValue = serialized.getValue("value");
        if (actionValue == null) {
            actionValue = "";
        }
        if (buttonAction != null) {
            if (buttonAction.contains("%btnaction_splitter_fm%")) {
                for (String s : StringUtils.splitLines(buttonAction, "%btnaction_splitter_fm%")) {
                    if (s.length() > 0) {
                        String action = s;
                        String value = null;
                        if (s.contains(";")) {
                            action = s.split(";", 2)[0];
                            value = s.split(";", 2)[1];
                        }
                        element.actions.add(new ActionExecutor.ActionContainer(action, value));
                    }
                }
            } else {
                element.actions.add(new ActionExecutor.ActionContainer(buttonAction, actionValue));
            }
        }

        element.hoverSound = serialized.getValue("hoversound");
        if (element.hoverSound != null) {
            File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(element.hoverSound));
            if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
                SoundRegistry.registerSound(element.hoverSound, element.hoverSound);
            } else {
                element.hoverSound = null;
            }
        }

        element.hoverLabel = serialized.getValue("hoverlabel");

        element.tooltip = serialized.getValue("description");

        element.button = new ExtendedButton(0, 0, 0, 0, Component.literal(""), (press) -> {
            for (ActionExecutor.ActionContainer c : element.getActionList()) {
                c.execute();
            }
        }).setAutoRegisterToScreen(!isEditor());

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

        return element;

    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull ButtonElement element, @NotNull SerializedElement serializeTo) {

        if (!element.actions.isEmpty()) {
            String buttonaction = "";
            for (ActionExecutor.ActionContainer c : element.actions) {
                String s2 = c.action;
                if (c.value != null) {
                    s2 += ";" + c.value;
                }
                buttonaction += s2 + "%btnaction_splitter_fm%";
            }
            serializeTo.putProperty("buttonaction", buttonaction);
        }

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
        if (element instanceof ButtonElement b) {
            return b.getButton().getMessage();
        }
        return Component.translatable("fancymenu.editor.add.button");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}
