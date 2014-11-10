[![Build Status](https://travis-ci.org/bwaldvogel/log4j-systemd-journal-appender.png?branch=master)](https://travis-ci.org/bwaldvogel/log4j-systemd-journal-appender)

[Log4j][log4j] appender that logs event meta data such as the timestamp, the logger name, the exception stacktrace, [ThreadContext (aka MDC)][thread-context] or the Java thread name to [fields][systemd-journal-fields] in [systemd journal][systemd-journal] (aka "the Journal") .

Read Lennart Poettering's blog post [systemd for Developers III][systemd-for-developers] if you are not familar with [systemd journal][systemd-journal].

## Usage with Log4j 2.x ##
Add the following Maven dependency to your project:

```xml
<dependency>
	<groupId>de.bwaldvogel</groupId>
	<artifactId>log4j-systemd-journal-appender</artifactId>
	<version>2.1.0</version>
	<scope>runtime</scope>
</dependency>
```

## Usage with Log4j 1.x ##

See the [`1.x` branch][1.x-branch] of this project.

### Runtime dependencies ###
    - Linux with systemd-journal
    - Log4j 2.x

## Configuration

The appender can be configured with the following properties

	Property name         | Default           | Type    | Description
	--------------------- | ----------------- | ------- | -----------
	`logSource`           | false             | boolean | Determines whether the log locations are logged. Note that there is a performance overhead when switched on. The data is logged in standard systemd journal fields `CODE_FILE`, `CODE_LINE` and `CODE_FUNC`.
	`logStacktrace`       | true              | boolean | Determines whether the full exception stack trace is logged. This data is logged in the user field `STACKTRACE`.
	`logThreadName`       | true              | boolean | Determines whether the thread name is logged. This data is logged in the user field `THREAD_NAME`.
	`logLoggerName`       | true              | boolean | Determines whether the logger name is logged. This data is logged in the user field `LOG4J_LOGGER`.
	`logThreadContext`    | true              | boolean | Determines whether the [thread context][thread-context] is logged. Each key/value pair is logged as user field with the `threadContextPrefix` prefix.
	`threadContextPrefix` | `THREAD_CONTEXT_` | String  | Determines how [thread context][thread-context] keys should be prefixed when `logThreadContext` is set to true. Note that keys need to match the regex pattern `[A-Z0-9_]+` and are normalized otherwise.

## Example ##

### `log4j2.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO" packages="de.bwaldvogel.log4j">
    <Appenders>
        <Console name="console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n" />
        </Console>
        <SystemdJournal name="journal" logStacktraces="true" logSource="false" />
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="console" />
            <AppenderRef ref="journal" />
        </Root>
    </Loggers>
</Configuration>
```

This will tell Log4j to log to [systemd journal][systemd-journal] as well as to stdout (console).
Note that a layout is not set for `SystemdJournal`.
This is because meta data of a log event such as the timestamp, the logger name or the Java thread name are mapped to [systemd-journal fields][systemd-journal-fields] and need not be rendered into a string that loses all the semantic information.

### `YourExample.java`
```java
import org.apache.logging.log4j.*;

class YourExample {

    private static Logger logger = LogManager.getLogger(YourExample.class);

    public static void main(String[] args) {
        ThreadContext.put("MY_KEY", "some value");
        logger.info("this is an example");
    }
}
```

Running this sample class will log a message to journald:

### Systemd Journal

```
# journalctl -n
Okt 13 21:26:00 myhost java[2370]: this is an example
```

Use `journalctl -o verbose` to show all fields:

```
# journalctl -o verbose -n
Mo 2014-10-13 21:26:00.873732 CEST [s=c25294…;i=470;b=ea0fe2…;m=14612…;t=50
    PRIORITY=6
    _TRANSPORT=journal
    _UID=1000
    _GID=1000
    _CAP_EFFECTIVE=0
    _SYSTEMD_OWNER_UID=1000
    _SYSTEMD_SLICE=user-1000.slice
    _BOOT_ID=ea0fe2…
    _MACHINE_ID=4abc6d…
    _HOSTNAME=myhost
    _SYSTEMD_CGROUP=/user.slice/user-1000.slice/session-2.scope
    _SYSTEMD_SESSION=2
    _SYSTEMD_UNIT=session-2.scope
    CODE_FILE=src/main/c/log4j-systemd-journal-adapter.cpp
    CODE_LINE=30
    CODE_FUNC=Java_de_bwaldvogel_log4j_SystemdJournalAdapter_sendv
    SYSLOG_IDENTIFIER=java
    _COMM=java
    _EXE=/opt/oracle-jdk-bin-1.7.0.65/bin/java
    MESSAGE=this is an example
    THREAD_NAME=main
    THREAD_CONTEXT_MY_KEY=some value
    LOG4J_LOGGER=YourExample
    _PID=2370
    _CMDLINE=/opt/oracle-jdk-bin-1.7.0.65/bin/java …
    _SOURCE_REALTIME_TIMESTAMP=1413228360873732
```

Note that the [ThreadContext][thread-context] key-value pair `{"MY_KEY": "some value"}` is automatically added as field with prefix `THREAD_CONTEXT`.

You can use the power of [systemd journal][systemd-journal] to filter for interesting messages. Example:

`journalctl LOG4J_LOGGER=YourExample THREAD_NAME=main` will only show messages that are logged from the Java main thread via the `YourExample` logger.

## Related Work ##

* [logback-journal][logback-journal]
	* Systemd Journal appender for Logback

[1.x-branch]: https://github.com/bwaldvogel/log4j-systemd-journal-appender/tree/1.x
[log4j]: http://logging.apache.org/log4j/2.x/
[thread-context]: http://logging.apache.org/log4j/2.x/manual/thread-context.html
[systemd-for-developers]: http://0pointer.de/blog/projects/journal-submit.html
[systemd-journal]: http://www.freedesktop.org/software/systemd/man/systemd-journald.service.html
[systemd-journal-fields]: http://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html
[logback-journal]: https://github.com/gnieh/logback-journal
