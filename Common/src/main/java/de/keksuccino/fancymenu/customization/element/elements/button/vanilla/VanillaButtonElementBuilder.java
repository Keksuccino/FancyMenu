package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElementBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class VanillaButtonElementBuilder extends ButtonElementBuilder {

    public static final VanillaButtonElementBuilder INSTANCE = new VanillaButtonElementBuilder();

    @Override
    public @NotNull String getIdentifier() {
        return "vanilla_button";
    }

    @Override
    public @NotNull VanillaButtonElement buildDefaultInstance() {
        VanillaButtonElement element = new VanillaButtonElement(this);
        element.width = 100;
        element.height = 20;
        element.label = "New Button";
        return element;
    }

    @Override
    public @Nullable SerializedElement serializeElementInternal(@NotNull AbstractElement elementAbstract) {

        try {

            VanillaButtonElement element = (VanillaButtonElement) elementAbstract;
            SerializedElement ori = super.serializeElementInternal(element);
            if (ori != null) {

                SerializedElement serialized = new SerializedElement("vanilla_button");
                for (Map.Entry<String, String> m : ori.getEntries().entrySet()) {
                    serialized.addEntry(m.getKey(), m.getValue());
                }

                serialized.addEntry("button_identifier", element.vanillaButtonIdentifier);
                serialized.addEntry("is_hidden", "" + element.vanillaHidden);

                return serialized;

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;

    }

    @Override
    public @NotNull VanillaButtonElement deserializeElement(@NotNull SerializedElement serialized) {

        VanillaButtonElement element = (VanillaButtonElement) super.deserializeElement(serialized);

        String hidden = serialized.getEntryValue("is_hidden");
        if ((hidden != null) && hidden.equalsIgnoreCase("true")) {
            element.vanillaHidden = true;
        }

        String buttonId = serialized.getEntryValue("button_identifier");
        if (buttonId != null) {
            element.vanillaButtonIdentifier = buttonId;
        } else {
            throw new NullPointerException("[FANCYMENU] Failed to deserialize VanillaButtonElement! Button ID was NULL!");
        }

        return element;

    }

    @Override
    public @Nullable VanillaButtonElement deserializeElementInternal(@NotNull SerializedElement serializedElement) {
        return (VanillaButtonElement) super.deserializeElementInternal(serializedElement);
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
