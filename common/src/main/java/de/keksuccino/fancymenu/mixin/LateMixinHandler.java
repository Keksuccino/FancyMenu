package de.keksuccino.fancymenu.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Arrays;
import java.util.List;

public class LateMixinHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void initLateMixins() {

        LOGGER.info("[FANCYMENU] Adding late mixin configurations..");

        Mixins.addConfiguration("fancymenu.late.mixins.json");

//        // If any of the targets were already loaded, you can trigger retransform:
//        for (String target : listOfTargetClassNames()) {
//            try {
//                Class<?> clazz = Class.forName(target);
//                if (InstrumentationHolder.getInstrumentation().isModifiableClass(clazz)) {
//                    InstrumentationHolder.getInstrumentation().retransformClasses(clazz);
//                }
//            } catch (Exception e) {
//                // Possibly class not yet loaded, ignore.
//            }
//        }

    }

    @NotNull
    private static List<String> listOfTargetClassNames() {
        // Return a list of fully qualified names in the sub‐JAR that you intend to mix into.
        return Arrays.asList(
                "gg.essential.handlers.PauseMenuDisplay"
        );
    }

}
