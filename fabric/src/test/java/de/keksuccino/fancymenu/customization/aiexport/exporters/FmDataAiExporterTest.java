package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The FMData export runs on dedicated servers, so it has to work without a running client.
 */
class FmDataAiExporterTest {

    @Test
    void exportWorksWithoutAClient() {
        AiExportDocument document = FmDataAiExporter.export();
        assertFalse(document.text().isBlank());
        assertTrue(document.text().startsWith("# "), "Exports start with a heading");
        assertTrue(document.fileNameBase().matches("[a-z_]+"), document.fileNameBase());
    }

}
