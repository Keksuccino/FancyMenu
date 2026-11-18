package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.fmdata.FmDataServerListener;
import de.keksuccino.fancymenu.customization.fmdata.FmDataServerListenerHandler;
import de.keksuccino.fancymenu.customization.fmdata.FmDataWelcomeData;
import de.keksuccino.fancymenu.customization.fmdata.FmDataWelcomeDataHandler;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Exports the server-side FMData setup (listeners and welcome data) for AI chats.<br>
 * Runs on dedicated servers too, so it must never touch client-only classes.
 */
public final class FmDataAiExporter {

    private FmDataAiExporter() {
    }

    @NotNull
    public static AiExportDocument export() {
        List<FmDataServerListener> listeners = FmDataServerListenerHandler.getListeners();
        List<FmDataWelcomeData> welcomeData = FmDataWelcomeDataHandler.getWelcomeDataEntries();
        return AiExportDocument.create("Server FMData Setup", "server_fmdata", writer -> writer.fieldText("Exported on", "the server, with the /fmdata command"), writer -> {

            writer.heading(2, "FMData Server Listeners (" + listeners.size() + ")");
            writer.paragraph("FMData lets FancyMenu clients and the server send text data to each other. Clients send data with the \"Send FM Data To Server\" action. "
                    + "Server listeners run server commands when data with a matching data identifier and data arrives. "
                    + "A match value of `*` matches everything. Users manage listeners with `/fmdata listener add|edit|remove|list`.");
            if (listeners.isEmpty()) writer.paragraph("There are no listeners.");
            for (FmDataServerListener listener : listeners) {
                writer.heading(3, "Listener " + listener.listener_name);
                writer.field("Name", listener.listener_name);
                writer.field("Data identifier must match", listener.listen_for_identifier, "matching type " + AiExportWriter.inlineCode(listener.matchingTypeIdentifierNormalized()) + ", ignores case: " + AiExportText.yesNo(listener.ignore_case_identifier));
                writer.field("Data must match", listener.listen_for_data, "matching type " + AiExportWriter.inlineCode(listener.matchingTypeDataNormalized()) + ", ignores case: " + AiExportText.yesNo(listener.ignore_case_data));
                writer.field("Only fires for players matching", (listener.fire_for_player != null) ? listener.fire_for_player : "*", (listener.fire_for_player == null) ? "fires for everyone" : "a player selector");
                writer.field("Commands it runs", String.join("\n", listener.commands_to_execute_on_fire));
            }

            writer.heading(2, "FMData Welcome Data (" + welcomeData.size() + ")");
            writer.paragraph("Data the server sends to FancyMenu clients when they join. Clients react to it with the \"On FM Data Received\" listener. "
                    + "Users manage it with `/fmdata welcome_data add|edit|remove|list`.");
            if (welcomeData.isEmpty()) writer.paragraph("There is no welcome data.");
            for (FmDataWelcomeData data : welcomeData) {
                writer.heading(3, "Welcome Data " + data.welcome_data_name);
                writer.field("Name", data.welcome_data_name);
                writer.field("Sent to players matching", (data.target_player != null) ? data.target_player : "*");
                writer.field("Data identifier", data.data_identifier);
                writer.field("Data", data.data);
            }

        });
    }

}
