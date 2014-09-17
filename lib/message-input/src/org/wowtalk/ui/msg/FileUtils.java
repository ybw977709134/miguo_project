package org.wowtalk.ui.msg;

import java.io.*;

public class FileUtils {
    public static boolean copyFile(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean copyFile(InputStream in, File dst) {
        try {
            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean copyFile(String srcFilePath, String destFilePath) {
        return copyFile(new File(srcFilePath), new File(destFilePath));
    }

    /**
     * Get extension from given filename.
     * @param filename
     * @return
     */
    public static String getExt(String filename) {
        if (null == filename) return null;
        int i = filename.lastIndexOf(".");
        if (-1 == i) return null;
        return filename.substring(i + 1);
    }
}
