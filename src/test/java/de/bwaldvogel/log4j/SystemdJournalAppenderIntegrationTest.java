package de.bwaldvogel.log4j;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

public class SystemdJournalAppenderIntegrationTest {

    private Logger getLogger() {
        return Logger.getLogger(getClass());
    }

    @Before
    public void configureLog4j()
    {
        Properties properties = new Properties();
        properties.put("log4j.rootLogger", "INFO, console, journal");

        properties.put("log4j.logger.de.bwaldvogel", "TRACE");

        properties.put("log4j.appender.console", "org.apache.log4j.ConsoleAppender");
        properties.put("log4j.appender.console.layout", "org.apache.log4j.PatternLayout");
        properties.put("log4j.appender.console.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss} %-5p [%.20c] %m%n");

        properties.put("log4j.appender.journal", SystemdJournalAppender.class.getName());
        properties.put("log4j.appender.journal.threshold", "TRACE");
        properties.put("log4j.appender.journal.logStacktrace", "true");
        properties.put("log4j.appender.journal.logThreadName", "true");
        properties.put("log4j.appender.journal.logLoggerName", "true");
        properties.put("log4j.appender.journal.logAppenderName", "true");
        properties.put("log4j.appender.journal.logMdc", "true");
        properties.put("log4j.appender.journal.syslogIdentifier", "log4journaltest");

        LogManager.resetConfiguration();
        PropertyConfigurator.configure(properties);
    }

    @Before
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void testMessages() {
        getLogger().trace("this is a test message with level TRACE");
        getLogger().debug("this is a test message with level DEBUG");
        getLogger().info("this is a test message with level INFO");
        getLogger().warn("this is a test message with level WARN");
        getLogger().error("this is a test message with level ERROR");
    }

    @Test
    public void testMessageWithUnicode() {
        getLogger().info("this is a test message with unicode: →←üöß");
    }

    @Test
    public void testMessageWithMDC() {
        MDC.put("some key1", "some value %d");
        MDC.put("some key2", "some other value with unicode: →←üöß");
        getLogger().info("this is a test message with a MDC");
    }

    @Test
    public void testMessageWithStacktrace() {
        getLogger().info("this is a test message with an exception", new RuntimeException("some exception"));
    }

}
