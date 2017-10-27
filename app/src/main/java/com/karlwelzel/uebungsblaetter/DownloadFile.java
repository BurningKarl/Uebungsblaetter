package com.karlwelzel.uebungsblaetter;

import java.io.File;

/**
 * Created by karl on 19.10.17.
 */

public class DownloadFile {
    public final String url;
    public final File file;
    public final String title;

    public DownloadFile(String url, File file) {
        this.url = url;
        this.file = file;
        this.title = file.getName();
    }

    public DownloadFile(String url, File file, String title) {
        this.url = url;
        this.file = file;
        this.title = title;
    }
}
