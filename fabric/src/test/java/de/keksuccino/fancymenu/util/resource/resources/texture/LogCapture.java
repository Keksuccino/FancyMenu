package de.keksuccino.fancymenu.util.resource.resources.texture;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records the warnings and errors that the loggers of some classes log from any thread while the capture is open.
 */
public final class LogCapture implements AutoCloseable {

    private final LoggerContext context = (LoggerContext) LogManager.getContext(false);
    private final Map<String, LoggerConfig> previousConfigs = new LinkedHashMap<>();
    private final RecordingAppender appender = new RecordingAppender("LogCapture-" + System.nanoTime());

    private LogCapture(@NotNull Class<?>... loggerClasses) {
        this.appender.start();
        Configuration configuration = this.context.getConfiguration();
        for (Class<?> loggerClass : loggerClasses) {
            String loggerName = loggerClass.getName();
            this.previousConfigs.put(loggerName, configuration.getLoggers().get(loggerName));
            LoggerConfig logger = new LoggerConfig(loggerName, Level.ALL, false);
            logger.addAppender(this.appender, Level.WARN, null);
            configuration.addLogger(loggerName, logger);
        }
        this.context.updateLoggers();
    }

    /**
     * Starts recording the warnings and errors of the loggers of the classes.
     */
    @NotNull
    public static LogCapture problemsOf(@NotNull Class<?>... loggerClasses) {
        return new LogCapture(loggerClasses);
    }

    /**
     * The messages of all warnings and errors that got logged so far.
     */
    @NotNull
    public List<String> messages() {
        List<String> messages = new ArrayList<>();
        for (LogEvent event : this.appender.events) {
            messages.add(event.getLevel() + " " + event.getMessage().getFormattedMessage());
        }
        return messages;
    }

    @Override
    public void close() {
        Configuration configuration = this.context.getConfiguration();
        for (Map.Entry<String, LoggerConfig> entry : this.previousConfigs.entrySet()) {
            configuration.removeLogger(entry.getKey());
            if (entry.getValue() != null) configuration.addLogger(entry.getKey(), entry.getValue());
        }
        this.context.updateLoggers();
        this.appender.stop();
    }

    private static final class RecordingAppender extends AbstractAppender {

        private final List<LogEvent> events = new CopyOnWriteArrayList<>();

        private RecordingAppender(@NotNull String name) {
            super(name, null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            this.events.add(event.toImmutable());
        }

    }

}
