package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.List;

/**
 * Exports FancyMenu's variables for AI chats.
 */
public final class VariablesAiExporter {

    private static final String VARIABLES_INTRO = "FancyMenu variables are global text values. They stay saved when the game closes, unless they are set to get cleared on every game launch. "
            + "Texts can show a variable with `$$variable_name` or the \"Get Variable Value\" placeholder (`getvariable`), and actions like \"Set Variable Value\" (`set_variable`) change them.";

    private VariablesAiExporter() {
    }

    @NotNull
    public static AiExportDocument exportAll() {
        List<Variable> variables = VariableHandler.getVariables().stream().sorted(Comparator.comparing(Variable::getName)).toList();
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
        return AiExports.create("Variable " + variable.getName(), "variable_" + variable.getName(), writer -> {
            writer.heading(2, "Variable");
            writer.paragraph(VARIABLES_INTRO);
            writeVariable(writer, variable);
        });
    }

    private static void writeVariable(@NotNull AiExportWriter writer, @NotNull Variable variable) {
        writer.field("$$" + variable.getName(), variable.getValue(), variable.isResetOnLaunch() ? "gets cleared on every game launch" : null);
    }

}
