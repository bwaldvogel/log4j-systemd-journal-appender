package de.bwaldvogel.log4j;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SystemdJournalAppenderTest {

    @Mock
    private SystemdJournalLibrary journalLibrary;

    @Mock
    private Message message;

    @Before
    public void prepare() {
        ThreadContext.clearAll();
    }

    @Test
    public void testAppend_Simple() {
        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, null, false,
                journalLibrary, false, false, false, false, false, false, null, null, null, null);

        when(message.getFormattedMessage()).thenReturn("some message");
        LogEvent event = new Log4jLogEvent.Builder().setMessage(message).setLevel(Level.INFO).build();

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

    @Test
    public void testAppend_LogSource() {

        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, null, false,
                journalLibrary, true, false, false, false, false, false, null, null, null, null);

        when(message.getFormattedMessage()).thenReturn("some message");
        LogEvent event = new Log4jLogEvent.Builder() //
                .setMessage(message)//
                .setLoggerFqcn(journalAppender.getClass().getName())//
                .setLevel(Level.INFO).build();
        event.setIncludeLocation(true);

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add("CODE_FILE=%s");
        expectedArgs.add("SystemdJournalAppenderTest.java");
        expectedArgs.add("CODE_FUNC=%s");
        expectedArgs.add("testAppend_LogSource");
        expectedArgs.add("CODE_LINE=%d");
        expectedArgs.add(Integer.valueOf(68));
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

    @Test
    public void testAppend_DoNotLogException() {

        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, null, false,
                journalLibrary, false, false, false, false, false, false, null, null, null, null);

        when(message.getFormattedMessage()).thenReturn("some message");

        LogEvent event = new Log4jLogEvent.Builder() //
                .setMessage(message)//
                .setLoggerFqcn(journalAppender.getClass().getName())//
                .setThrown(new Throwable()) //
                .setLevel(Level.INFO).build();
        event.setIncludeLocation(true);

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

    @Test
    public void testAppend_ThreadAndContext() {

        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, null, false,
                journalLibrary, false, false, true, true, true, true, null, "some-identifier", "3", "TEST_LOGGER_NAME");

        when(message.getFormattedMessage()).thenReturn("some message");

        DefaultThreadContextMap contextMap = new DefaultThreadContextMap();
        LogEvent event = mock(LogEvent.class);
        when(event.getMessage()).thenReturn(message);
        when(event.getLoggerName()).thenReturn("some logger");
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getThreadName()).thenReturn("the thread");
        when(event.getContextData()).thenReturn(contextMap);

        contextMap.put("foo%s$1%d", "bar");

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add("THREAD_NAME=%s");
        expectedArgs.add("the thread");
        expectedArgs.add("TEST_LOGGER_NAME_LOGGER=%s");
        expectedArgs.add("some logger");
        expectedArgs.add("LOG4J_APPENDER=%s");
        expectedArgs.add("Journal");
        expectedArgs.add("THREAD_CONTEXT_FOO_S_1_D=%s");
        expectedArgs.add("bar");
        expectedArgs.add("SYSLOG_IDENTIFIER=%s");
        expectedArgs.add("some-identifier");
        expectedArgs.add("SYSLOG_FACILITY=%d");
        expectedArgs.add(3);
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }
}
