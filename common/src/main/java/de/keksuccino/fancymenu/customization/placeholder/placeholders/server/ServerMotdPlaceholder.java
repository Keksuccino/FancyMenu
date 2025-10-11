package de.keksuccino.fancymenu.customization.placeholder.placeholders.server;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerMotdPlaceholder extends Placeholder {

    public ServerMotdPlaceholder() {
        super("servermotd");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String ip = dps.values.get("ip");
        String line = dps.values.get("line");
        if ((ip != null) && (line != null) && MathUtils.isInteger(line)) {
            return getServerMotdLine(ip, Integer.valueOf(line));
        }
        return null;
    }

    private static String getServerMotdLine(String ip, int line) {
        try {
            if (line > 2) {
                line = 2;
            }
            if (line < 1) {
                line = 1;
            }
            ServerData sd = ServerCache.getServer(ip);
            if (sd != null) {
                if (sd.motd != null) {
                    List<String> lines = splitMotdLines(sd.motd.getString());
                    if (lines.size() >= 2) {
                        return lines.get(line-1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static List<String> splitMotdLines(String motd) {
        List<String> l = new ArrayList<>();
        try {
            if (motd.contains("\n")) {
                l.addAll(Arrays.asList(motd.split("\n")));
            } else {
                l.add(motd);
                l.add("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return l;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("ip");
        l.add("line");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.servermotd");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.servermotd.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.server");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("ip", "someserver.com:25565");
        dps.values.put("line", "1");
        return dps;
    }

}
