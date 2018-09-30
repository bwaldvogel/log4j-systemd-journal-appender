package de.bwaldvogel.log4j;

import com.sun.jna.Native;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Plugin(name = "SystemdJournal", category = "Core", elementType = "appender", printObject = true)
public class SystemdJournalAppender extends AbstractAppender {

    private final SystemdJournalLibrary journalLibrary;

    private final boolean logStacktrace;

    private final boolean logSource;

    private final boolean logThreadName;

    private final boolean logLoggerName;

    private final boolean logAppenderName;

    private final boolean logThreadContext;

    private final String threadContextPrefix;

    private final String syslogIdentifier;

    SystemdJournalAppender(String name, Filter filter, Layout<?> layout, boolean ignoreExceptions,
                           SystemdJournalLibrary journalLibrary, boolean logSource, boolean logStacktrace, boolean logThreadName,
                           boolean logLoggerName, boolean logAppenderName, boolean logThreadContext, String threadContextPrefix, String syslogIdentifier) {
        super(name, filter, layout, ignoreExceptions);
        this.journalLibrary = journalLibrary;
        this.logSource = logSource;
        this.logStacktrace = logStacktrace;
        this.logThreadName = logThreadName;
        this.logLoggerName = logLoggerName;
        this.logAppenderName = logAppenderName;
        this.logThreadContext = logThreadContext;
        if (threadContextPrefix == null) {
            this.threadContextPrefix = "THREAD_CONTEXT_";
        } else {
            this.threadContextPrefix = normalizeKey(threadContextPrefix);
        }
        this.syslogIdentifier = syslogIdentifier;
    }

    @PluginFactory
    public static SystemdJournalAppender createAppender(@PluginAttribute("name") final String name,
            @PluginAttribute("ignoreExceptions") final String ignoreExceptionsString,
            @PluginAttribute("logSource") final String logSourceString,
            @PluginAttribute("logStacktrace") final String logStacktraceString,
            @PluginAttribute("logLoggerName") final String logLoggerNameString,
            @PluginAttribute("logAppenderName") final String logAppenderNameString,
            @PluginAttribute("logThreadName") final String logThreadNameString,
            @PluginAttribute("logThreadContext") final String logThreadContextString,
            @PluginAttribute("threadContextPrefix") final String threadContextPrefix,
            @PluginAttribute("syslogIdentifier") final String syslogIdentifier,
            @PluginElement("Layout") final Layout<?> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginConfiguration final Configuration config) {
        final boolean ignoreExceptions = Booleans.parseBoolean(ignoreExceptionsString, true);
        final boolean logSource = Booleans.parseBoolean(logSourceString, false);
        final boolean logStacktrace = Booleans.parseBoolean(logStacktraceString, true);
        final boolean logThreadName = Booleans.parseBoolean(logThreadNameString, true);
        final boolean logLoggerName = Booleans.parseBoolean(logLoggerNameString, true);
        final boolean logAppenderName = Booleans.parseBoolean(logAppenderNameString, true);
        final boolean logThreadContext = Booleans.parseBoolean(logThreadContextString, true);

        if (name == null) {
            LOGGER.error("No name provided for SystemdJournalAppender");
            return null;
        }

        final SystemdJournalLibrary journalLibrary;
        try {
            journalLibrary = Native.loadLibrary("systemd", SystemdJournalLibrary.class);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load systemd library." +
                " Please note that JNA requires an executable temporary folder." +
                " It can be explicitly defined with -Djna.tmpdir", e);
        }

        return new SystemdJournalAppender(name, filter, layout, ignoreExceptions, journalLibrary, logSource, logStacktrace,
                logThreadName, logLoggerName, logAppenderName, logThreadContext, threadContextPrefix, syslogIdentifier);
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
        switch (level.getStandardLevel()) {
        case FATAL:
            return 2; // LOG_CRIT
        case ERROR:
            return 3; // LOG_ERR
        case WARN:
            return 4; // LOG_WARNING
        case INFO:
            return 6; // LOG_INFO
        case DEBUG:
        case TRACE:
            return 7; // LOG_DEBUG
        default:
            throw new IllegalArgumentException("Cannot map log level: " + level);
        }
    }

    @Override
    public void append(LogEvent event) {
        List<Object> args = new ArrayList<>();

        args.add(buildFormattedMessage(event));

        args.add("PRIORITY=%d");
        args.add(Integer.valueOf(log4jLevelToJournalPriority(event.getLevel())));

        if (logThreadName) {
            args.add("THREAD_NAME=%s");
            args.add(event.getThreadName());
        }

        if (logLoggerName) {
            args.add("LOG4J_LOGGER=%s");
            args.add(event.getLoggerName());
        }

        if (logAppenderName) {
            args.add("LOG4J_APPENDER=%s");
            args.add(getName());
        }

        if (logStacktrace && event.getThrown() != null) {
            StringWriter stacktrace = new StringWriter();
            event.getThrown().printStackTrace(new PrintWriter(stacktrace));
            args.add("STACKTRACE=%s");
            args.add(stacktrace.toString());
        }

        if (logSource && event.getSource() != null) {
            String fileName = event.getSource().getFileName();
            args.add("CODE_FILE=%s");
            args.add(fileName);

            String methodName = event.getSource().getMethodName();
            args.add("CODE_FUNC=%s");
            args.add(methodName);

            int lineNumber = event.getSource().getLineNumber();
            args.add("CODE_LINE=%d");
            args.add(Integer.valueOf(lineNumber));
        }

        if (logThreadContext) {
            ReadOnlyStringMap context = event.getContextData();
            if (context != null) {
                for (Entry<String, String> entry : context.toMap().entrySet()) {
                    String key = entry.getKey();
                    args.add(threadContextPrefix + normalizeKey(key) + "=%s");
                    args.add(entry.getValue());
                }
            }
        }

        if (syslogIdentifier != null && !syslogIdentifier.isEmpty()) {
            args.add("SYSLOG_IDENTIFIER=%s");
            args.add(syslogIdentifier);
        }

        args.add(null); // null terminated

        journalLibrary.sd_journal_send("MESSAGE=%s", args.toArray());
    }

    private String buildFormattedMessage(LogEvent event) {
        if (getLayout() != null) {
            byte[] message = getLayout().toByteArray(event);
            return new String(message, StandardCharsets.UTF_8);
        }
        return event.getMessage().getFormattedMessage();
    }

    private static String normalizeKey(String key) {
        return key.toUpperCase().replaceAll("[^_A-Z0-9]", "_");
    }
}
