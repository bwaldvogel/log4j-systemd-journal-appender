package de.bwaldvogel.log4j;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;

import java.util.Properties;

public class SystemdJournalAppenderWithLayoutIntegrationTest extends SystemdJournalAppenderIntegrationTest {

    @Before
    @Override
    public void configureLog4j()
    {
        Properties properties = new Properties();
        properties.put("log4j.rootLogger", "INFO, console, journal");

        properties.put("log4j.logger.de.bwaldvogel", "TRACE");

        properties.put("log4j.appender.console", "org.apache.log4j.ConsoleAppender");
        properties.put("log4j.appender.console.layout", "org.apache.log4j.PatternLayout");
        properties.put("log4j.appender.console.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss} %-5p [%.20c] %m%n");

        properties.put("log4j.appender.journal", SystemdJournalAppenderWithLayout.class.getName());
        properties.put("log4j.appender.journal.layout", "org.apache.log4j.PatternLayout");
        properties.put("log4j.appender.journal.layout.ConversionPattern", "%-5p [%.20c] %m%n");
        properties.put("log4j.appender.journal.appendStacktrace", "true");

        LogManager.resetConfiguration();
        PropertyConfigurator.configure(properties);
    }

}
