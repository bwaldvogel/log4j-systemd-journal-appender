package de.bwaldvogel.log4j;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SystemdJournalAppenderTest {

    private static final Logger LOGGER = Logger.getLogger(SystemdJournalAppenderTest.class);

    private static final String APPENDER_NAME = "journaldAppender";

    @BeforeClass
    public static void setUpJournaldAppender() {
        SystemdJournalAppender journaldAppender = new SystemdJournalAppender();
        journaldAppender.setName(APPENDER_NAME);
        journaldAppender.setThreshold(Level.TRACE);
        Logger.getRootLogger().addAppender(journaldAppender);
    }

    @AfterClass
    public static void closeJournaldAppender() {
        Appender appender = Logger.getRootLogger().getAppender(APPENDER_NAME);
        if (appender != null) {
            appender.close();
        }
    }

    @Before
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void testMessages() {
        LOGGER.trace("this is a test message with level TRACE");
        LOGGER.debug("this is a test message with level DEBUG");
        LOGGER.info("this is a test message with level INFO");
        LOGGER.warn("this is a test message with level WARN");
        LOGGER.error("this is a test message with level ERROR");
    }

    @Test
    public void testMessageWithUnicode() {
        LOGGER.info("this is a test message with unicode: →←üöß");
    }

    @Test
    public void testMessageWithMDC() {
        MDC.put("some key1", "some value %d");
        MDC.put("some key2", "some other value with unicode: →←üöß");
        LOGGER.info("this is a test message with a MDC");
    }

    @Test
    public void testMessageWithStacktrace() {
        LOGGER.info("this is a test message with an exception", new RuntimeException("some exception"));
    }

}
