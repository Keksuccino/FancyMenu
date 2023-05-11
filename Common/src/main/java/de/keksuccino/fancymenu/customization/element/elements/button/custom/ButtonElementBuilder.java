package de.keksuccino.fancymenu.customization.element.elements.button.custom;

import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.widget.Button;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ButtonElementBuilder extends ElementBuilder<ButtonElement, ButtonEditorElement> {

    public ButtonElementBuilder() {
        super("custom_button", "addbutton");
    }

    @Override
    public @NotNull ButtonElement buildDefaultInstance() {
        ButtonElement element = new ButtonElement(this);
        element.width = 100;
        element.height = 20;
        element.label = "New Button";
        return element;
    }

    @Override
    public @NotNull ButtonElement deserializeElement(@NotNull SerializedElement serialized) {

        ButtonElement element = buildDefaultInstance();

        element.label = serialized.getEntryValue("label");

        String buttonAction = serialized.getEntryValue("buttonaction");
        String actionValue = serialized.getEntryValue("value");
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

        element.hoverSound = serialized.getEntryValue("hoversound");
        if (element.hoverSound != null) {
            File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(element.hoverSound));
            if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
                SoundRegistry.registerSound(element.hoverSound, element.hoverSound);
            } else {
                element.hoverSound = null;
            }
        }

        element.hoverLabel = serialized.getEntryValue("hoverlabel");

        element.tooltip = serialized.getEntryValue("description");

        element.button = new Button(0, 0, 0, 0, Component.literal(""), true, (press) -> {
            for (ActionExecutor.ActionContainer c : element.actions) {
                c.execute();
            }
        });

        element.clickSound = serialized.getEntryValue("clicksound");
        if (element.clickSound != null) {
            File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(element.clickSound));
            if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
                SoundHandler.registerSound(f.getPath(), f.getPath());
            } else {
                element.clickSound = null;
            }
        }

        element.backgroundTextureNormal = serialized.getEntryValue("backgroundnormal");

        element.backgroundTextureHover = serialized.getEntryValue("backgroundhovered");

        String loopBackAnimations = serialized.getEntryValue("loopbackgroundanimations");
        if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
            element.loopBackgroundAnimations = false;
        }

        String restartBackAnimationsOnHover = serialized.getEntryValue("restartbackgroundanimations");
        if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
            element.restartBackgroundAnimationsOnHover = false;
        }

        element.backgroundAnimationNormal = serialized.getEntryValue("backgroundanimationnormal");

        element.backgroundAnimationHover = serialized.getEntryValue("backgroundanimationhovered");

        return element;

    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull ButtonElement element) {

        SerializedElement serialized = new SerializedElement();

        if ((element.actions != null) && !element.actions.isEmpty()) {
            String buttonaction = "";
            for (ActionExecutor.ActionContainer c : element.actions) {
                String s2 = c.action;
                if (c.value != null) {
                    s2 += ";" + c.value;
                }
                buttonaction += s2 + "%btnaction_splitter_fm%";
            }
            serialized.addEntry("buttonaction", buttonaction);
        }

        if (element.backgroundTextureNormal != null) {
            serialized.addEntry("backgroundnormal", element.backgroundTextureNormal);
        }
        if (element.backgroundAnimationNormal != null) {
            serialized.addEntry("backgroundanimationnormal", element.backgroundAnimationNormal);
        }
        if (element.backgroundTextureHover != null) {
            serialized.addEntry("backgroundhovered", element.backgroundTextureHover);
        }
        if (element.backgroundAnimationHover != null) {
            serialized.addEntry("backgroundanimationhovered", element.backgroundAnimationHover);
        }
        serialized.addEntry("restartbackgroundanimations", "" + element.restartBackgroundAnimationsOnHover);
        serialized.addEntry("loopbackgroundanimations", "" + element.loopBackgroundAnimations);
        if (element.hoverSound != null) {
            serialized.addEntry("hoversound", element.hoverSound);
        }
        if (element.hoverLabel != null) {
            serialized.addEntry("hoverlabel", element.hoverLabel);
        }
        if (element.clickSound != null) {
            serialized.addEntry("clicksound", element.clickSound);
        }
        if (element.tooltip != null) {
            serialized.addEntry("description", element.tooltip);
        }
        if (element.label != null) {
            serialized.addEntry("label", element.label);
        }

        return serialized;

    }

    @Override
    public @NotNull ButtonEditorElement wrapIntoEditorElement(@NotNull ButtonElement element, @NotNull LayoutEditorScreen editor) {
        return new ButtonEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("helper.creator.add.button");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}
