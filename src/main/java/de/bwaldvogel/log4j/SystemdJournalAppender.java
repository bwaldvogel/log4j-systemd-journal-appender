package de.bwaldvogel.log4j;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;

public class SystemdJournalAppender extends AppenderSkeleton {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Override
    public void close() {
        // ignore
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    private String log4jLevelToJournalPriority(Level level) {
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
        if (level == Level.FATAL) {
            return "2"; // LOG_CRIT
        } else if (level == Level.ERROR) {
            return "3"; // LOG_ERR
        } else if (level == Level.WARN) {
            return "4"; // LOG_WARNING
        } else if (level == Level.INFO) {
            return "6"; // LOG_INFO
        } else if (level == Level.DEBUG) {
            return "7"; // LOG_DEBUG
        } else {
            throw new IllegalArgumentException("Cannot map log level: " + level);
        }
    }

    @Override
    protected void append(LoggingEvent event) {
        Map<String, String> logData = new HashMap<String, String>();

        logData.put("MESSAGE", event.getRenderedMessage());
        logData.put("PRIORITY", log4jLevelToJournalPriority(event.getLevel()));

        logData.put("THREAD_NAME", event.getThreadName());
        logData.put("LOG4J_LOGGER", event.getLogger().getName());

        if (event.getThrowableStrRep() != null) {
            StringBuilder sb = new StringBuilder();
            for (String stackTrace : event.getThrowableStrRep()) {
                sb.append(stackTrace).append(LINE_SEPARATOR);
            }
            logData.put("EXCEPTION", sb.toString());
        }

        Hashtable<?, ?> context = MDC.getContext();
        if (context != null) {
            Enumeration<?> keys = context.keys();
            while (keys.hasMoreElements()) {
                Object nextElement = keys.nextElement();
                String key = "LOG4J_MDC_" + nextElement;
                logData.put(key, context.get(nextElement).toString());
            }
        }
        String[] keys = new String[logData.size()];
        String[] values = new String[logData.size()];
        int i = 0;
        for (Entry<String, String> entry : logData.entrySet()) {
            keys[i] = entry.getKey().toUpperCase().replaceAll("[^_A-Z0-9]", "_");
            values[i] = entry.getValue();
            i++;
        }

        assert logData.size() == keys.length;
        assert logData.size() == values.length;
        SystemdJournalAdapter.sendv(logData.size(), keys, values);
    }

}
