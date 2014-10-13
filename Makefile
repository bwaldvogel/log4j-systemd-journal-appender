all: src/main/resources/liblog4j-systemd-journal-adapter.so gradleBuild

src/main/resources/liblog4j-systemd-journal-adapter.so: src/main/c/log4j-systemd-journal-adapter.cpp
	gradle --daemon compileJava
	javah -cp build/classes/main -o src/main/c/log4j-systemd-journal-adapter.h de.bwaldvogel.log4j.SystemdJournalAdapter
	${CXX} -fPIC -shared -o src/main/resources/liblog4j-systemd-journal-adapter.so -lsystemd -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux src/main/c/log4j-systemd-journal-adapter.cpp

gradleBuild:
	gradle --daemon build

clean:
	gradle --daemon clean
	-rm src/main/c/log4j-systemd-journal-adapter.h
	-rm src/main/resources/liblog4j-systemd-journal-adapter.so
