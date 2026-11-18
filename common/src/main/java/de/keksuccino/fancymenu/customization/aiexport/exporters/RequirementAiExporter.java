package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes requirement containers in AI exports, optionally with which requirements are met right now.
 */
public final class RequirementAiExporter {

    private static final Logger LOGGER = LogManager.getLogger();

    private RequirementAiExporter() {
    }

    /**
     * Exports the requirements of the requirement editor, checked right now.
     */
    @NotNull
    public static AiExportDocument export(@NotNull RequirementContainer container) {
        return AiExports.create("Requirements", "requirements", writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Requirements");
            writer.paragraph("Requirements decide if something happens, like if an element shows or if the actions in an IF block run. "
                    + "The results were checked at the moment of the export. Requirements with values that use `$$` value placeholders of a listener or element only get correct values while their script runs.");
            writeContainer(writer, "Requirements", container, references, true);
            references.write(writer);
        });
    }

    /**
     * Writes the container as bullet with all its requirements and groups nested below it.
     *
     * @param check If the requirements should get checked right now, to show which of them are met.
     */
    public static void writeContainer(@NotNull AiExportWriter writer, @NotNull String label, @NotNull RequirementContainer container, @NotNull AiExportReferences references, boolean check) {
        List<RequirementInstance> instances = container.getInstances();
        List<RequirementGroup> groups = container.getGroups();
        if (instances.isEmpty() && groups.isEmpty()) {
            writer.fieldText(label, "none, so they are always met");
            return;
        }
        String summary = "all of these must be met";
        if (check) summary += " (right now: " + describe(check(container)) + ")";
        writer.fieldText(label, summary);
        writer.nested(() -> {
            instances.forEach(instance -> writeInstance(writer, instance, references, check));
            groups.forEach(group -> writeGroup(writer, group, references, check));
        });
    }

    private static void writeInstance(@NotNull AiExportWriter writer, @NotNull RequirementInstance instance, @NotNull AiExportReferences references, boolean check) {
        references.addRequirement(instance.requirement);
        String name = AiExportText.safe(() -> AiExportText.plain(instance.requirement.getDisplayName()), instance.requirement.getIdentifier());
        StringBuilder line = new StringBuilder("**").append(name).append("** (").append(AiExportWriter.inlineCode(instance.requirement.getIdentifier())).append(")");
        if (instance.mode == RequirementInstance.RequirementMode.IF_NOT) {
            line.append(", inverted (mode `if_not`, so it is met when the requirement is NOT true)");
        }
        if (check) line.append(", right now: ").append(describe(check(instance)));
        writer.bullet(line.toString());
        if (instance.requirement.hasValue()) {
            writer.nested(() -> writer.field("Value", instance.value));
        }
    }

    private static void writeGroup(@NotNull AiExportWriter writer, @NotNull RequirementGroup group, @NotNull AiExportReferences references, boolean check) {
        String mode = (group.mode == RequirementGroup.GroupMode.OR) ? "mode `or`, so at least one of these must be met" : "mode `and`, so all of these must be met";
        StringBuilder line = new StringBuilder("**Group** ").append(AiExportWriter.inlineCode(group.identifier)).append(" (").append(mode).append(")");
        if (check) line.append(", right now: ").append(describe(check(group)));
        writer.bullet(line.toString());
        writer.nested(() -> {
            group.getInstances().forEach(instance -> writeInstance(writer, instance, references, check));
            group.getGroups().forEach(nested -> writeGroup(writer, nested, references, check));
        });
    }

    /**
     * Checks the whole container like {@link RequirementContainer#requirementsMet()}, but without its result cache and without requirements that would change something.
     */
    @NotNull
    public static CheckResult check(@NotNull RequirementContainer container) {
        List<CheckResult> results = new ArrayList<>();
        container.getGroups().forEach(group -> results.add(check(group)));
        container.getInstances().forEach(instance -> results.add(check(instance)));
        return combine(results, false);
    }

    @NotNull
    static CheckResult check(@NotNull RequirementGroup group) {
        List<CheckResult> results = new ArrayList<>();
        group.getInstances().forEach(instance -> results.add(check(instance)));
        group.getGroups().forEach(nested -> results.add(check(nested)));
        return combine(results, group.mode == RequirementGroup.GroupMode.OR);
    }

    @NotNull
    static CheckResult check(@NotNull RequirementInstance instance) {
        if (instance.requirement.hasCheckSideEffects()) return CheckResult.NOT_CHECKED;
        try {
            return instance.requirementMet() ? CheckResult.MET : CheckResult.NOT_MET;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to check requirement for an AI export: " + instance.requirement.getIdentifier(), ex);
            return CheckResult.FAILED;
        }
    }

    /**
     * Combines results like requirement groups do. Unknown results only decide the combined result when the known results don't.
     */
    @NotNull
    static CheckResult combine(@NotNull List<CheckResult> results, boolean orMode) {
        if (results.isEmpty()) return CheckResult.MET;
        boolean unknown = false;
        for (CheckResult result : results) {
            if (orMode && (result == CheckResult.MET)) return CheckResult.MET;
            if (!orMode && (result == CheckResult.NOT_MET)) return CheckResult.NOT_MET;
            if ((result == CheckResult.NOT_CHECKED) || (result == CheckResult.FAILED)) unknown = true;
        }
        if (unknown) return CheckResult.NOT_CHECKED;
        return orMode ? CheckResult.NOT_MET : CheckResult.MET;
    }

    @NotNull
    public static String describe(@NotNull CheckResult result) {
        return switch (result) {
            case MET -> "met";
            case NOT_MET -> "NOT met";
            case NOT_CHECKED -> "unknown (not checked, because checking changes the result of some requirements, like the once-per-session one)";
            case FAILED -> "unknown (checking failed)";
        };
    }

    public enum CheckResult {
        MET,
        NOT_MET,
        NOT_CHECKED,
        FAILED
    }

}
