package de.keksuccino.fancymenu.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricPlatformHelperTest {

    @Test
    void getLoaderVersionQueriesFabricLoaderContainer() {
        RecordingFabricPlatformHelper platformHelper = new RecordingFabricPlatformHelper();

        assertEquals("loader-version", platformHelper.getLoaderVersion());
        assertEquals("fabricloader", platformHelper.requestedModId);
    }

    private static final class RecordingFabricPlatformHelper extends FabricPlatformHelper {

        private String requestedModId;

        @Override
        public String getModVersion(String modId) {
            this.requestedModId = modId;
            return "loader-version";
        }

    }

}
