[Log4j][log4j] appender for [systemd journal][systemd-journal].

### Dependencies ###
    - Linux with systemd-journal

## Building ##

```bash
make
```

## Usage ##

Example `log4j.properties`:
```
log4j.rootLogger=INFO, console, journal

log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p [%.20c] %m%n

log4j.appender.journal=de.bwaldvogel.SystemdJournalAppender
```

This will log to stdout (console) as well as to systemd-journal.

[log4j]:
[systemd-journal]: http://www.freedesktop.org/software/systemd/man/systemd-journald.service.html
