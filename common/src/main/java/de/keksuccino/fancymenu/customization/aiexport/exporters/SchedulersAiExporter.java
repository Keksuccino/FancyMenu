package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerInstance;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Exports schedulers for AI chats.
 */
public final class SchedulersAiExporter {

    private static final String SCHEDULERS_INTRO = "Schedulers run their action script again and again in the background, every \"tick delay\" milliseconds, for \"ticks to run\" ticks or forever. "
            + "They start with the game when \"start on launch\" is on, or when the \"Start Scheduler\" action starts them by their identifier, and the \"Stop Scheduler\" action stops them. "
            + ActionScriptAiExporter.SCRIPT_INTRO;

    private SchedulersAiExporter() {
    }

    /**
     * Exports all saved schedulers.
     */
    @NotNull
    public static AiExportDocument exportAll() {
        return exportAll(SchedulerHandler.getInstances());
    }

    /**
     * @param schedulers The schedulers to export, like the ones of the scheduler manager, which can have unsaved changes.
     */
    @NotNull
    public static AiExportDocument exportAll(@NotNull List<SchedulerInstance> schedulers) {
        return AiExports.create("All Schedulers", "schedulers", writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Schedulers (" + schedulers.size() + ")");
            writer.paragraph(SCHEDULERS_INTRO);
            if (schedulers.isEmpty()) writer.paragraph("There are no schedulers.");
            schedulers.forEach(scheduler -> writeScheduler(writer, scheduler, references));
            references.write(writer);
        });
    }

    @NotNull
    public static AiExportDocument exportSingle(@NotNull SchedulerInstance scheduler) {
        return AiExports.create("Scheduler " + scheduler.getIdentifier(), "scheduler_" + scheduler.getIdentifier(), writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.paragraph(SCHEDULERS_INTRO);
            writeScheduler(writer, scheduler, references);
            references.write(writer);
        });
    }

    private static void writeScheduler(@NotNull AiExportWriter writer, @NotNull SchedulerInstance scheduler, @NotNull AiExportReferences references) {
        writer.heading(3, "Scheduler " + scheduler.getIdentifier());
        writer.field("Identifier", scheduler.getIdentifier());
        writer.fieldText("Running right now", AiExportText.yesNo(SchedulerHandler.isRunning(scheduler.getIdentifier())));
        writer.fieldText("Starts on game launch", AiExportText.yesNo(scheduler.isStartOnLaunch()));
        writer.field("Start delay in milliseconds", scheduler.getStartDelayMs());
        writer.field("Tick delay in milliseconds", scheduler.getTickDelayMs(), (scheduler.getTickDelayMs() <= 0) ? ("0 means every " + SchedulerHandler.DEFAULT_TICK_DELAY_MS + " milliseconds") : null);
        writer.field("Ticks to run", scheduler.getTicksToRun(), (scheduler.getTicksToRun() <= 0) ? "0 means it runs until it gets stopped" : null);
        writer.bullet("**Action script:**");
        writer.nested(() -> ActionScriptAiExporter.writeScript(writer, scheduler.getActionScript(), references));
    }

}
