package de.bwaldvogel.log4j;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

public class SystemdJournalAppenderWithLayoutTest {

    private SystemdJournalLibrary journalLibrary;
    private Layout layout;

    @Before
    public void prepare() {
        journalLibrary = mock(SystemdJournalLibrary.class);
        layout = mock(Layout.class);

        MDC.clear();
    }

    @Test
    public void testAppend_Simple() {
        SystemdJournalAppenderWithLayout journalAppender = new SystemdJournalAppenderWithLayout(journalLibrary);
        journalAppender.setLogThreadName(false);
        journalAppender.setLogLoggerName(false);
        journalAppender.setLogAppenderName(false);
        journalAppender.setLayout(layout);

        Logger logger = Logger.getLogger(SystemdJournalAppenderWithLayoutTest.class);
        LoggingEvent event = new LoggingEvent("foo", logger, Level.INFO, "some message", null);

        when(layout.format(event)).thenReturn("formatted message");

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("formatted message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

    @Test
    public void testAppend_DoNotAppendStackTrace() {
        SystemdJournalAppenderWithLayout journalAppender = new SystemdJournalAppenderWithLayout(journalLibrary);
        journalAppender.setLogThreadName(false);
        journalAppender.setLogLoggerName(false);
        journalAppender.setLogAppenderName(false);
        journalAppender.setAppendStacktrace(false);
        journalAppender.setLogStacktrace(false);
        journalAppender.setLayout(layout);

        Logger logger = Logger.getLogger(SystemdJournalAppenderWithLayoutTest.class);
        LoggingEvent event = new LoggingEvent("foo", logger, Level.INFO, "some message", new RuntimeException());

        when(layout.format(event)).thenReturn("formatted message");

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("formatted message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

}
