[![Build Status](https://travis-ci.org/bwaldvogel/log4j-systemd-journal-appender.png?branch=1.x)](https://travis-ci.org/bwaldvogel/log4j-systemd-journal-appender)

[Log4j][log4j] appender that logs event meta data such as the timestamp, the logger name, the exception stacktrace, [mapped diagnostic contexts (MDC)][mdc] or the Java thread name to [fields][systemd-journal-fields] in [systemd journal][systemd-journal] (aka "the Journal") .

Read Lennart Poettering's blog post [systemd for Developers III][systemd-for-developers] if you are not familar with [systemd journal][systemd-journal].

## Usage ##
Add the following Maven dependency to your project:

```xml
<dependency>
	<groupId>de.bwaldvogel</groupId>
	<artifactId>log4j-systemd-journal-appender</artifactId>
	<version>1.3.2</version>
	<scope>runtime</scope>
</dependency>
```

Configure the appender in your `log4j.properties`:
```
log4j.appender.journal=de.bwaldvogel.log4j.SystemdJournalAppender
```

Alternatively use `de.bwaldvogel.log4j.SystemdJournalAppenderWithLayout` if you want to format messages with a layout.

## Configuration ##

The appender can be configured with the following properties

       Property name      | Default      | Type    | Description
       -------------------| ------------ | ------- | -----------
       `logStacktrace`    | true         | boolean | Determines whether the full exception stack trace is logged. This data is logged in the user field `STACKTRACE`.
       `logThreadName`    | true         | boolean | Determines whether the thread name is logged. This data is logged in the user field `THREAD_NAME`.
       `logLoggerName`    | true         | boolean | Determines whether the logger name is logged. This data is logged in the user field `LOG4J_LOGGER`.
       `logAppenderName`  | true         | boolean | Determines whether the appender name is logged. This data is logged in the user field `LOG4J_APPENDER`.
       `logMdc`           | true         | boolean | Determines whether the [thread context][thread-context] is logged. Each key/value pair is logged as user field with the `mdcPrefix` prefix.
       `mdcPrefix`        | `LOG4J_MDC_` | String  | Determines how [MDC][mdc] keys should be prefixed when `logMdc` is set to true. Note that keys need to match the regex pattern `[A-Z0-9_]+` and are normalized otherwise.
       `syslogIdentifier` | null         | String  | This data is logged in the user field `SYSLOG_IDENTIFIER`.  If this is not set, the underlying system will use the command name (usually `java`) instead.
       `appendStacktrace` | true         | boolean | `SystemdJournalAppenderWithLayout` only: Determines whether the full exception stack trace is to be appended to the log message.

### Runtime dependencies ###

* Java 8 or later
* Linux with systemd-journal
* Log4j 1.2

## Examples ##

### `log4j.properties` without layout
```
log4j.rootLogger=INFO, journal, console

log4j.appender.journal=de.bwaldvogel.log4j.SystemdJournalAppender
log4j.appender.journal.logStacktrace=true
log4j.appender.journal.logThreadName=true
log4j.appender.journal.logLoggerName=true

log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p [%.20c] %m%n
```

### `log4j.properties` with layout
```
log4j.rootLogger=INFO, journal, console

log4j.appender.journal=de.bwaldvogel.log4j.SystemdJournalAppenderWithLayout
log4j.appender.journal.layout=org.apache.log4j.PatternLayout
log4j.appender.journal.layout.ConversionPattern=%-5p [%.20c] %m%n

log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p [%.20c] %m%n
```

This will tell Log4j to log to [systemd journal][systemd-journal] as well as to stdout (console).
Note that a layout is not used in case of `SystemdJournalAppender` because meta data of a log event such as the timestamp, the logger name or the Java thread name are
mapped to [systemd-journal fields][systemd-journal-fields] and need not be rendered into a string that loses all the semantic information.
An additional layout is used with the `SystemdJournalAppenderWithLayout` appender.
Note that the message does not include the timestamp, as this will be provided by the journal viewer normally.

### `YourExample.java`
```java
import org.apache.log4j.*;

class YourExample {

    private static Logger logger = Logger.getLogger(YourExample.class);

    public static void main(String[] args) {
        MDC.put("MY_KEY", "some value");
        logger.info("this is an example");
    }
}
```

Running this sample class will log a message to journald:

### Systemd Journal

```
# journalctl -n
Okt 13 21:26:00 myhost java[2370]: INFO  [YourExample] this is an example
```

Use `journalctl -o verbose` to show all fields:

```
# journalctl -o verbose -n
Di 2015-09-29 21:07:05.850017 CEST [s=45e0…;i=984;b=c257…;m=1833…;t=520e…;x=3e1e…]
    PRIORITY=6
    _TRANSPORT=journal
    _UID=1000
    _GID=1000
    _CAP_EFFECTIVE=0
    _SYSTEMD_OWNER_UID=1000
    _SYSTEMD_SLICE=user-1000.slice
    _MACHINE_ID=4abc6d…
    _HOSTNAME=myhost
    _SYSTEMD_CGROUP=/user.slice/user-1000.slice/session-2.scope
    _SYSTEMD_SESSION=2
    _SYSTEMD_UNIT=session-2.scope
    _BOOT_ID=c257f8…
    THREAD_NAME=main
    LOG4J_LOGGER=de.bwaldvogel.log4j.SystemdJournalAppenderIntegrationTest
    _COMM=java
    _EXE=/opt/oracle-jdk-bin-1.7.0.80/bin/java
    MESSAGE=this is a test message with a MDC
    CODE_FILE=SystemdJournalAppenderIntegrationTest.java
    CODE_FUNC=testMessageWithMDC
    CODE_LINE=36
    THREAD_CONTEXT_SOME_KEY1=some value %d
    THREAD_CONTEXT_SOME_KEY2=some other value with unicode: →←üöß
    SYSLOG_IDENTIFIER=log4j2-test
    LOG4J_APPENDER=Journal
    _PID=8224
    _CMDLINE=/opt/oracle-jdk-bin-1.7.0.80/bin/java …
    _SOURCE_REALTIME_TIMESTAMP=1443553625850017
```

Note that the [MDC][mdc] key-value pair `{"MY_KEY": "some value"}` is automatically added as field with prefix `LOG4J_MDC`.

You can use the power of [systemd journal][systemd-journal] to filter for interesting messages. Example:

`journalctl CODE_FUNC=testMessageWithMDC THREAD_NAME=main` will only show messages that are logged from the Java main thread in a method called `testMessageWithMDC`.

## Related Work ##

* [logback-journal][logback-journal]
	* Systemd Journal appender for Logback

[log4j]: http://logging.apache.org/log4j
[mdc]: https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/MDC.html
[systemd-for-developers]: http://0pointer.de/blog/projects/journal-submit.html
[systemd-journal]: http://www.freedesktop.org/software/systemd/man/systemd-journald.service.html
[systemd-journal-fields]: http://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html
[logback-journal]: https://github.com/gnieh/logback-journal
