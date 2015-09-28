package de.bwaldvogel.log4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Before;
import org.junit.Test;

public class SystemdJournalAppenderIntegrationTest {

    private static final Logger LOGGER = LogManager.getLogger(SystemdJournalAppenderIntegrationTest.class.getName());

    @Before
    public void clearMdc() {
        ThreadContext.clearAll();
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
        ThreadContext.put("some key1", "some value %d");
        ThreadContext.put("some key2", "some other value with unicode: →←üöß");
        LOGGER.info("this is a test message with a MDC");
    }

    @Test
    public void testMessageWithPlaceholders() {
        ThreadContext.put("some key1%s", "%1$");
        ThreadContext.put("%1$", "%1$");
        LOGGER.info("this is a test message with special placeholder characters: %1$");
    }

    @Test
    public void testMessageWithStacktrace() {
        LOGGER.info("this is a test message with an exception", new RuntimeException("some exception"));
    }

}
