package de.keksuccino.fancymenu.customization.placeholder.placeholders.server;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerPlayerCountPlaceholder extends Placeholder {

    public ServerPlayerCountPlaceholder() {
        super("serverplayercount");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String ip = dps.values.get("ip");
        if (ip != null) {
            ServerData sd = ServerCache.getServer(ip);
            if (sd != null) {
                if (sd.status != null) {
                    return sd.status.getString();
                } else {
                    return "0/0";
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("ip");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.variables.serverplayercount");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.fancymenu.editor.dynamicvariabletextfield.variables.serverplayercount.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.server");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("ip", "someserver.com:25565");
        return dps;
    }

}
