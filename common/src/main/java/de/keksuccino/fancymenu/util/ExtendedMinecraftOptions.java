package de.keksuccino.fancymenu.util;

import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

/**
 * This interface gets applied to {@link Options} and adds methods to get all {@link OptionInstance}s, etc.
 */
@ClassExtender(Options.class)
public interface ExtendedMinecraftOptions {

    @NotNull
    Map<String, OptionInstance<?>> getOptionInstancesFancyMenu();

}
