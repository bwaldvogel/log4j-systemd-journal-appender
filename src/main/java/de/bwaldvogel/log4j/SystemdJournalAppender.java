package de.bwaldvogel.log4j;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;

import com.sun.jna.Native;

public class SystemdJournalAppender extends AppenderSkeleton {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final SystemdJournalLibrary journalLibrary;

    public SystemdJournalAppender() {
        journalLibrary = (SystemdJournalLibrary) Native.loadLibrary("systemd-journal", SystemdJournalLibrary.class);
    }

    @Override
    public void close() {
        // ignore
    }

    @Override
    public boolean requiresLayout() {
        return false;
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

        args.add(event.getRenderedMessage());

        args.add("PRIORITY=%d");
        args.add(Integer.valueOf(log4jLevelToJournalPriority(event.getLevel())));

        args.add("THREAD_NAME=%s");
        args.add(event.getThreadName());

        args.add("LOG4J_LOGGER=%s");
        args.add(event.getLogger().getName());

        if (event.getThrowableStrRep() != null) {
            StringBuilder sb = new StringBuilder();
            for (String stackTrace : event.getThrowableStrRep()) {
                sb.append(stackTrace).append(LINE_SEPARATOR);
            }
            args.add("EXCEPTION=%s");
            args.add(sb.toString());
        }

        Hashtable<?, ?> context = MDC.getContext();
        if (context != null) {
            Enumeration<?> keys = context.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                String normalizedKey = key.toString().toUpperCase().replaceAll("[^_A-Z0-9]", "_");
                args.add("LOG4J_MDC_" + normalizedKey + "=%s");
                args.add(context.get(key).toString());
            }
        }

        journalLibrary.sd_journal_send("MESSAGE=%s", args.toArray());
    }
}
