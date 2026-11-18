package de.keksuccino.fancymenu.customization.aiexport;

import org.jetbrains.annotations.NotNull;

/**
 * Implemented by elements, menu backgrounds and decoration overlays that know things only while they run, like errors,
 * which their saved properties can't tell. AI exports of them include that info.
 */
public interface AiExportRuntimeInfo {

    /**
     * Writes the runtime info as bullets. Gets called on the main thread.
     */
    void writeAiExportRuntimeInfo(@NotNull AiExportWriter writer);

}
