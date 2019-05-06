package org.jboss.pnc.buildagent.logback.formatter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.jboss.pnc.buildagent.api.logging.LogFormatter;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class LogbackFormatter implements LogFormatter {

    private final Logger logger;
    private final ConsoleAppender<ILoggingEvent> appender;

    public LogbackFormatter() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger = context.getLogger("org.jboss.pnc._userlog_.build-log");
        appender = (ConsoleAppender<ILoggingEvent>) logger.getAppender("STDOUT-BUILD-LOG");
    }

    @Override
    public String format(String message) {
        ILoggingEvent logEvent = new LoggingEvent(LogbackFormatter.class.getName(), logger, Level.INFO, message, null, new Object[0]);
        byte[] encode = appender.getEncoder().encode(logEvent);
        return new String(encode, StandardCharsets.UTF_8);
    }
}
