package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.List;

/**
 * Exports FancyMenu's variables for AI chats.
 */
public final class VariablesAiExporter {

    /** Lists with more entries only get their first entries listed, so huge lists don't blow up the export. */
    static final int MAX_LISTED_ENTRIES = 200;

    static final String VARIABLES_INTRO = "FancyMenu variables are global values. Text variables hold one text, list variables hold a list of text entries. "
            + "They stay saved when the game closes, unless they are set to get cleared on every game launch. "
            + "Texts can show a variable with `$$variable_name` or the \"Get Variable Value\" placeholder (`getvariable`), and actions like \"Set Variable Value\" (`set_variable`) change them. "
            + "For list variables, `$$variable_name` and everything else that only knows text give the whole list as a JSON array of strings, like `[\"apple\",\"banana\"]`. "
            + "Text that gets set to a list variable gets read as such a JSON array, blank text empties the list, and any other text becomes its only entry, so a list never turns into a text variable by itself. "
            + "Lists have their own actions (`add_to_list`, `set_list_entry`, `remove_from_list`, `clear_list`, `set_list`, `arrange_list`), "
            + "placeholders (`list_get`, `list_size`, `list_index_of`, `list_join`), the \"List Contains Entry\" requirement "
            + "and the \"For Each List Entry\" action script block, which runs its actions once for every entry, with the entry as `$$list_entry` and its index as `$$list_index`. "
            + "The first entry has index 0, and negative indexes count from the end, so -1 is the last entry.";

    private VariablesAiExporter() {
    }

    @NotNull
    public static AiExportDocument exportAll() {
        List<UserVariableSnapshot> variables = VariableHandler.getVariableSnapshots().stream().sorted(Comparator.comparing(UserVariableSnapshot::name)).toList();
        return AiExports.create("All Variables", "variables", writer -> {
            writer.heading(2, "Variables (" + variables.size() + ")");
            writer.paragraph(VARIABLES_INTRO);
            if (variables.isEmpty()) {
                writer.paragraph("There are no variables.");
                return;
            }
            variables.forEach(variable -> writeVariable(writer, variable));
        });
    }

    @NotNull
    public static AiExportDocument exportSingle(@NotNull Variable variable) {
        UserVariableSnapshot snapshot = variable.snapshot();
        return AiExports.create("Variable " + snapshot.name(), "variable_" + snapshot.name(), writer -> {
            writer.heading(2, "Variable");
            writer.paragraph(VARIABLES_INTRO);
            writeVariable(writer, snapshot);
        });
    }

    static void writeVariable(@NotNull AiExportWriter writer, @NotNull UserVariableSnapshot variable) {
        if (!variable.isList()) {
            writer.field("$$" + variable.name(), variable.value(), variable.resetOnLaunch() ? "text variable, gets cleared on every game launch" : "text variable");
            return;
        }
        List<String> entries = variable.listEntries();
        String title = "**$$" + variable.name() + ":** list variable with " + entries.size() + ((entries.size() == 1) ? " entry" : " entries");
        writer.bullet(variable.resetOnLaunch() ? (title + ", gets emptied on every game launch") : title);
        writer.nested(() -> {
            int listed = Math.min(entries.size(), MAX_LISTED_ENTRIES);
            for (int i = 0; i < listed; i++) {
                writer.field("[" + i + "]", entries.get(i));
            }
            if (entries.size() > listed) writer.bullet("(" + (entries.size() - listed) + " more entries are not listed)");
        });
    }

}
