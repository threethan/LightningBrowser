package com.threethan.browser.lib;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;


// Contains functions which are not application-specific
public class FileLib {

    /** @noinspection IOStreamConstructor*/ // Fix requires higher API
    public static void copy(File fIn, File fOut) {
        try {
            InputStream in = new FileInputStream(fIn);
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(fOut.getParentFile()).mkdirs();
            OutputStream out = new FileOutputStream(fOut);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v("PATH", fOut.getAbsolutePath());
    }
}
