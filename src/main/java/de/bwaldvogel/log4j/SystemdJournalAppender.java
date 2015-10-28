package de.bwaldvogel.log4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.sun.jna.Native;

public class SystemdJournalAppender extends AppenderSkeleton {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final SystemdJournalLibrary journalLibrary;

    private boolean logStacktrace = true;

    private boolean logThreadName = true;

    private boolean logLoggerName = true;

    private boolean logAppenderName = true;

    private boolean logMdc = true;

    private boolean appendStacktrace = false;

    private String mdcPrefix = "LOG4J_MDC_";

    private String syslogIdentifier;

    public SystemdJournalAppender() {
        journalLibrary = (SystemdJournalLibrary) Native.loadLibrary("systemd", SystemdJournalLibrary.class);
    }

    SystemdJournalAppender(SystemdJournalLibrary journalLibrary) {
        this.journalLibrary = journalLibrary;
    }

    @Override
    public void close() {
        // ignore
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    private int log4jLevelToJournalPriority(Level level) {
        //
        // syslog.h
        //
        // #define LOG_EMERG 0 - system is unusable
        // #define LOG_ALERT 1 - action must be taken immediately
        // #define LOG_CRIT 2 - critical conditions
        // #define LOG_ERR 3 - error conditions
        // #define LOG_WARNING 4 - warning conditions
        // #define LOG_NOTICE 5 - normal but significant condition
        // #define LOG_INFO 6 - informational
        // #define LOG_DEBUG 7 - debug-level messages
        //
        switch (level.toInt()) {
        case Level.FATAL_INT:
            return 2; // LOG_CRIT
        case Level.ERROR_INT:
            return 3; // LOG_ERR
        case Level.WARN_INT:
            return 4; // LOG_WARNING
        case Level.INFO_INT:
            return 6; // LOG_INFO
        case Level.DEBUG_INT:
        case Level.TRACE_INT:
            return 7; // LOG_DEBUG
        default:
            throw new IllegalArgumentException("Cannot map log level: " + level);
        }
    }

    @Override
    protected void append(LoggingEvent event) {
        List<Object> args = new ArrayList<>();
        String message;

        if (this.layout != null) {
            message = this.layout.format(event);
        }
        else {
            message = event.getRenderedMessage();
        }
        if (appendStacktrace) {
            String[] s = event.getThrowableStrRep();
            if (s != null) {
                StringBuilder msgB = new StringBuilder(this.layout.format(event));
                int len = s.length;
                for(int i = 0; i < len; i++) {
                    msgB.append(s[i]);
                    msgB.append(LINE_SEPARATOR);
                }
                message = msgB.toString();
            }
        }
        args.add(message);

        args.add("PRIORITY=%d");
        args.add(Integer.valueOf(log4jLevelToJournalPriority(event.getLevel())));

        if (syslogIdentifier != null && !syslogIdentifier.isEmpty()) {
            args.add("SYSLOG_IDENTIFIER=%s");
            args.add(syslogIdentifier);
        }

        if (logThreadName) {
            args.add("THREAD_NAME=%s");
            args.add(event.getThreadName());
        }

        if (logLoggerName) {
            args.add("LOG4J_LOGGER=%s");
            args.add(event.getLogger().getName());
        }

        if (logAppenderName) {
            args.add("LOG4J_APPENDER=%s");
            args.add(getName());
        }

        if (logStacktrace && event.getThrowableStrRep() != null) {
            StringBuilder sb = new StringBuilder();
            for (String stackTrace : event.getThrowableStrRep()) {
                sb.append(stackTrace).append(LINE_SEPARATOR);
            }
            args.add("STACKTRACE=%s");
            args.add(sb.toString());
        }

        Map<?, ?> properties = event.getProperties();
        if (logMdc && properties != null) {
            for (Entry<?, ?> entry : properties.entrySet()) {
                Object key = entry.getKey();
                args.add(mdcPrefix + normalizeKey(key) + "=%s");
                args.add(entry.getValue().toString());
            }
        }

        args.add(null);

        journalLibrary.sd_journal_send("MESSAGE=%s", args.toArray());
    }

    private static String normalizeKey(Object key) {
        return key.toString().toUpperCase().replaceAll("[^_A-Z0-9]", "_");
    }

    public void setLogStacktrace(boolean logStacktrace) {
        this.logStacktrace = logStacktrace;
    }

    public void setLogThreadName(boolean logThreadName) {
        this.logThreadName = logThreadName;
    }

    public void setLogLoggerName(boolean logLoggerName) {
        this.logLoggerName = logLoggerName;
    }

    public void setLogAppenderName(boolean logAppenderName) {
        this.logAppenderName = logAppenderName;
    }

    public void setLogMdc(boolean logMdc) {
        this.logMdc = logMdc;
    }

    public void setAppendStacktrace(boolean appendStacktrace) {
        this.appendStacktrace = appendStacktrace;
    }

    public void setMdcPrefix(String mdcPrefix) {
        this.mdcPrefix = normalizeKey(mdcPrefix);
    }

    public String getSyslogIdentifier() {
        return syslogIdentifier;
    }

    public void setSyslogIdentifier(String syslogIdentifier) {
        this.syslogIdentifier = syslogIdentifier;
    }
}
