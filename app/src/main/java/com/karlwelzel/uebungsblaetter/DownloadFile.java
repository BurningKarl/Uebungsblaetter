package com.karlwelzel.uebungsblaetter;

import java.io.File;
import java.net.URL;

/**
 * Created by karl on 19.10.17.
 */

public class DownloadFile {
    public final URL url;
    public final File file;
    public final String title;

    public DownloadFile(URL url, File file) {
        this.url = url;
        this.file = file;
        this.title = file.getName();
    }

    public DownloadFile(URL url, File file, String title) {
        this.url = url;
        this.file = file;
        this.title = title;
    }
}
