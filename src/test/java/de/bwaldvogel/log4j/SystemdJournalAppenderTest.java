package de.bwaldvogel.log4j;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

public class SystemdJournalAppenderTest {

    private SystemdJournalLibrary journalLibrary;

    @Before
    public void prepare() {
        journalLibrary = mock(SystemdJournalLibrary.class);
    }

    @Test
    public void testAppend_Simple() {
        SystemdJournalAppender journalAppender = new SystemdJournalAppender(journalLibrary);
        journalAppender.setLogThreadName(false);
        journalAppender.setLogLoggerName(false);

        Logger logger = Logger.getLogger(SystemdJournalAppenderTest.class);
        LoggingEvent event = new LoggingEvent("foo", logger, Level.INFO, "some message", null);

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

    @Test
    public void testAppend_DoNotLogException() {

        SystemdJournalAppender journalAppender = new SystemdJournalAppender(journalLibrary);
        journalAppender.setLogStacktrace(false);
        journalAppender.setLogThreadName(false);
        journalAppender.setLogLoggerName(false);

        Logger logger = Logger.getLogger(SystemdJournalAppenderTest.class);
        LoggingEvent event = new LoggingEvent("foo", logger, Level.INFO, "some message", new Throwable());

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

    @Test
    public void testAppend_ThreadAndMdc() {

        SystemdJournalAppender journalAppender = new SystemdJournalAppender( journalLibrary);
        journalAppender.setLogThreadName(true);
        journalAppender.setLogLoggerName(true);
        journalAppender.setLogMdc(true);
        journalAppender.setMdcPrefix("");

        Logger logger = Logger.getLogger(SystemdJournalAppenderTest.class);
        LoggingEvent event = new LoggingEvent("foo", logger, Level.INFO, "some message", null);
        event.setProperty("foo", "bar");

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add("THREAD_NAME=%s");
        expectedArgs.add(Thread.currentThread().getName());
        expectedArgs.add("LOG4J_LOGGER=%s");
        expectedArgs.add(logger.getName());
        expectedArgs.add("FOO=%s");
        expectedArgs.add("bar");

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }
}
