package de.keksuccino.fancymenu.customization.placeholder.placeholders.client;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.KeyMappingUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class KeybindKeyPlaceholder extends Placeholder {

    @NotNull
    private final Supplier<KeyMapping[]> keyMappingsSupplier;

    public KeybindKeyPlaceholder() {
        this(KeyMappingUtils::getKeyMappings);
    }

    KeybindKeyPlaceholder(@NotNull Supplier<KeyMapping[]> keyMappingsSupplier) {
        super("keybind_key");
        this.keyMappingsSupplier = Objects.requireNonNull(keyMappingsSupplier);
    }

    /**
     * Keyboard key names get resolved by the windowing library, which is only safe to do on the main thread.
     */
    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String keybind = dps.values.get("keybind");
        if (keybind == null) return null;
        KeyMapping keyMapping = KeyMappingUtils.findKeyMapping(this.keyMappingsSupplier.get(), keybind);
        if (keyMapping == null) return "";
        // Same text the Controls screen shows for the keybind, so it also includes key modifiers like "Ctrl + G" on (Neo)Forge
        return keyMapping.getTranslatedKeyMessage().getString();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("keybind");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.keybind_key");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.keybind_key.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("keybind", "key.jump");
        return dps;
    }

}
