package de.keksuccino.fancymenu.networking.neoforge.packets.execute;

import de.keksuccino.fancymenu.util.LocalPlayerUtils;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Objects;

public class ClientboundExecutePacketPayloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ClientboundExecutePacketPayloadHandler INSTANCE = new ClientboundExecutePacketPayloadHandler();

    public static ClientboundExecutePacketPayloadHandler getInstance() {
        return INSTANCE;
    }

    public void handleData(final ClientboundExecutePacketPayload data, final PlayPayloadContext context) {

        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                String cmd = Objects.requireNonNull(data.command());
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                if (Minecraft.getInstance().player != null) LocalPlayerUtils.sendPlayerCommand(Minecraft.getInstance().player, cmd);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to handle ExecutePacketPayload!", ex);
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

    }

}
