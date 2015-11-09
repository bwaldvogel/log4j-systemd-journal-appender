package de.bwaldvogel.log4j;

import org.apache.log4j.spi.LoggingEvent;

public class SystemdJournalAppenderWithLayout extends SystemdJournalAppender {

    private boolean appendStacktrace = false;

    public SystemdJournalAppenderWithLayout() {
    }

    public SystemdJournalAppenderWithLayout(SystemdJournalLibrary journalLibrary) {
        super(journalLibrary);
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    @Override
    protected String buildRenderedMessage(LoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(layout.format(event));

        if (appendStacktrace) {
            String[] s = event.getThrowableStrRep();
            if (s != null) {
                int len = s.length;
                for (int i = 0; i < len; i++) {
                    sb.append(s[i]);
                    sb.append(LINE_SEPARATOR);
                }
            }
        }

        return sb.toString();
    }

    public void setAppendStacktrace(boolean appendStacktrace) {
        this.appendStacktrace = appendStacktrace;
    }
}
