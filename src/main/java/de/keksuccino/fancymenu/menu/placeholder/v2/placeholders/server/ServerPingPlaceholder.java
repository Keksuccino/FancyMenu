
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.server;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.fancymenu.menu.servers.ServerCache;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.multiplayer.ServerData;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerPingPlaceholder extends Placeholder {

    public ServerPingPlaceholder() {
        super("serverping");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String ip = dps.values.get("ip");
        if (ip != null) {
            ServerData sd = ServerCache.getServer(ip);
            if (sd != null) {
                return "" + sd.ping;
            }
        }
        return null;
    }

    @Override
    public  List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("ip");
        return l;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.serverping");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.serverping.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.server");
    }

    @Override
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("ip", "someserver.com:25565");
        return dps;
    }

}
