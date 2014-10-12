package de.bwaldvogel.log4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class SystemdJournalAdapter {

    private static final String LIBRARY_PATH = "/liblog4j-systemd-journal-adapter.so";

    static {
        try {
            loadLibraryFromJar();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + LIBRARY_PATH, e);
        }
    }

    public static native void sendv(int nKeysValues, String[] keys, String[] values);

    /**
     * Loads library from current JAR archive
     */
    private static void loadLibraryFromJar() throws IOException {
        File tempFile = File.createTempFile("liblog4j-systemd-journal-adapter", ".so");
        try {
            try (InputStream is = SystemdJournalAdapter.class.getResourceAsStream(LIBRARY_PATH);
                    OutputStream os = new FileOutputStream(tempFile);) {
                if (is == null) {
                    throw new FileNotFoundException(LIBRARY_PATH + " was not found in classloader.");
                }
                int readBytes;
                byte[] buffer = new byte[1024];
                while ((readBytes = is.read(buffer)) != -1) {
                    os.write(buffer, 0, readBytes);
                }
                os.flush();
                os.close();
            }
            System.load(tempFile.getAbsolutePath());
        } finally {
            if (!tempFile.delete()) {
                System.err.println("Failed to delete " + tempFile);
            }
        }
    }
}
