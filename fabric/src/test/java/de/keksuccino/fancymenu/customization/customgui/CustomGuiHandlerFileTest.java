package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Covers reading the custom GUI file, which is missing in new profiles until the first save creates it.
 */
class CustomGuiHandlerFileTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearGuis() {
        CustomGuiHandler.CUSTOM_GUI_SCREENS.clear();
        CustomGuiHandler.OVERRIDDEN_SCREENS.clear();
    }

    @Test
    void missingFileClearsAllGuisWithoutWarningsOrErrors() {
        putOldGuis();
        File missing = this.tempDir.resolve("custom_gui_screens.txt").toFile();

        List<LogEvent> problems = captureProblems(() -> CustomGuiHandler.readFromFile(missing));

        assertTrue(problems.isEmpty(), () -> "Expected no warnings or errors, but captured: " + problems);
        assertTrue(CustomGuiHandler.getGuis().isEmpty());
        assertTrue(CustomGuiHandler.getOverriddenScreens().isEmpty());
        assertFalse(missing.exists(), "Reading must not create the file, saving does");
    }

    @Test
    void existingFileReplacesAllGuis() {
        putOldGuis();
        File file = this.tempDir.resolve("custom_gui_screens.txt").toFile();
        PropertyContainerSet set = new PropertyContainerSet("custom_gui_screens");
        set.putContainer(new PropertyContainer("overridden_screens"));
        set.putContainer(gui("new_gui").serialize());
        PropertiesParser.serializeSetToFile(set, file.getAbsolutePath());

        List<LogEvent> problems = captureProblems(() -> CustomGuiHandler.readFromFile(file));

        assertTrue(problems.isEmpty(), () -> "Expected no warnings or errors, but captured: " + problems);
        assertEquals(List.of("new_gui"), CustomGuiHandler.getGuiIdentifiers());
        assertTrue(CustomGuiHandler.getOverriddenScreens().isEmpty());
    }

    @Test
    void unreadableFileStillGetsReportedAsError() {
        File file = this.tempDir.resolve("custom_gui_screens.txt").toFile();
        PropertyContainerSet set = new PropertyContainerSet("custom_gui_screens");
        set.putContainer(gui("locked_gui").serialize());
        PropertiesParser.serializeSetToFile(set, file.getAbsolutePath());
        // Some systems (Windows, root users) can still read files without read permission, so the test only runs where the permission works
        assumeTrue(file.setReadable(false, false) && !file.canRead(), "Files can't be made unreadable here");
        putOldGuis();

        try {
            List<LogEvent> problems = captureProblems(() -> CustomGuiHandler.readFromFile(file));

            assertEquals(1, problems.size(), () -> "Expected one error, but captured: " + problems);
            assertEquals(Level.ERROR, problems.get(0).getLevel());
            assertTrue(CustomGuiHandler.getGuis().isEmpty());
            assertTrue(CustomGuiHandler.getOverriddenScreens().isEmpty());
        } finally {
            file.setReadable(true, false);
        }
    }

    private static void putOldGuis() {
        CustomGui old = gui("old_gui");
        CustomGuiHandler.CUSTOM_GUI_SCREENS.put(old.identifier, old);
        CustomGuiHandler.OVERRIDDEN_SCREENS.put("old_screen", old.identifier);
    }

    @NotNull
    private static CustomGui gui(@NotNull String identifier) {
        CustomGui gui = new CustomGui();
        gui.identifier = identifier;
        gui.title = identifier;
        return gui;
    }

    /**
     * Returns the warnings and errors {@link CustomGuiHandler} logs while the invocation runs.
     */
    @NotNull
    private static List<LogEvent> captureProblems(@NotNull Runnable invocation) {
        String loggerName = CustomGuiHandler.class.getName();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        LoggerConfig previous = configuration.getLoggers().get(loggerName);
        RecordingAppender appender = new RecordingAppender("CustomGuiHandlerFileTest-" + System.nanoTime());
        appender.start();
        LoggerConfig logger = new LoggerConfig(loggerName, Level.ALL, false);
        logger.addAppender(appender, Level.WARN, null);
        configuration.addLogger(loggerName, logger);
        context.updateLoggers();
        try {
            invocation.run();
            return new ArrayList<>(appender.events);
        } finally {
            configuration.removeLogger(loggerName);
            if (previous != null) configuration.addLogger(loggerName, previous);
            context.updateLoggers();
            appender.stop();
        }
    }

    private static final class RecordingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private RecordingAppender(@NotNull String name) {
            super(name, null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            this.events.add(event.toImmutable());
        }

    }

}
