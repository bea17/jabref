package net.sf.jabref.util;

import net.sf.jabref.export.SaveSession;

import java.io.File;

public class FileBasedLock {

    /**
     * This method checks whether there is a lock file for the given file. If
     * there is, it waits for 500 ms. This is repeated until the lock is gone
     * or we have waited the maximum number of times.
     *
     * @param file The file to check the lock for.
     * @param maxWaitCount The maximum number of times to wait.
     * @return true if the lock file is gone, false if it is still there.
     */
    public static boolean waitForFileLock(File file, int maxWaitCount) {
        // Check if the file is locked by another JabRef user:
        int lockCheckCount = 0;
        while (hasLockFile(file)) {

            if (lockCheckCount++ == maxWaitCount) {
                return false;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
        return true;
    }

    /**
     * Check whether a lock file exists for this file.
     * @param file The file to check.
     * @return true if a lock file exists, false otherwise.
     */
    public static boolean hasLockFile(File file) {
        File lock = new File(file.getPath() + SaveSession.LOCKFILE_SUFFIX);
        return lock.exists();
    }

    /**
     * Find the lock file's last modified time, if it has a lock file.
     * @param file The file to check.
     * @return the last modified time if lock file exists, -1 otherwise.
     */
    public static long getLockFileTimeStamp(File file) {
        File lock = new File(file.getPath() + SaveSession.LOCKFILE_SUFFIX);
        return lock.exists() ? lock.lastModified() : -1;
    }

    /**
     * Check if a lock file exists, and delete it if it does.
     * @return true if the lock file existed, false otherwise.
     */
    public static boolean deleteLockFile(File file) {
        File lock = new File(file.getPath() + SaveSession.LOCKFILE_SUFFIX);
        if (!lock.exists()) {
            return false;
        }
        lock.delete();
        return true;
    }
}
