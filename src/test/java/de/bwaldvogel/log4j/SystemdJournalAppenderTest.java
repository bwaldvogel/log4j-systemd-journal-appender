package de.bwaldvogel.log4j;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
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
        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, false, journalLibrary,
                false, false, false, false, false, false, null, null);

        Message message = mock(Message.class);
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

        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, false, journalLibrary,
                true, false, false, false, false, false, null, null);

        Message message = mock(Message.class);
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
        expectedArgs.add(Integer.valueOf(62));
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }

    @Test
    public void testAppend_DoNotLogException() {

        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, false, journalLibrary,
                false, false, false, false, false, false, null, null);

        Message message = mock(Message.class);
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

        SystemdJournalAppender journalAppender = new SystemdJournalAppender("Journal", null, false, journalLibrary,
                false, false, true, true, true, true, null, "some-identifier");

        Message message = mock(Message.class);
        when(message.getFormattedMessage()).thenReturn("some message");

        Map<String, String> contextMap = new LinkedHashMap<>();
        LogEvent event = mock(LogEvent.class);
        when(event.getMessage()).thenReturn(message);
        when(event.getLoggerName()).thenReturn("some logger");
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getThreadName()).thenReturn("the thread");
        when(event.getContextMap()).thenReturn(contextMap);

        contextMap.put("foo%s$1%d", "bar");

        journalAppender.append(event);

        List<Object> expectedArgs = new ArrayList<>();
        expectedArgs.add("some message");
        expectedArgs.add("PRIORITY=%d");
        expectedArgs.add(6);
        expectedArgs.add("THREAD_NAME=%s");
        expectedArgs.add("the thread");
        expectedArgs.add("LOG4J_LOGGER=%s");
        expectedArgs.add("some logger");
        expectedArgs.add("LOG4J_APPENDER=%s");
        expectedArgs.add("Journal");
        expectedArgs.add("THREAD_CONTEXT_FOO_S_1_D=%s");
        expectedArgs.add("bar");
        expectedArgs.add("SYSLOG_IDENTIFIER=%s");
        expectedArgs.add("some-identifier");
        expectedArgs.add(null);

        verify(journalLibrary).sd_journal_send("MESSAGE=%s", expectedArgs.toArray());
    }
}
