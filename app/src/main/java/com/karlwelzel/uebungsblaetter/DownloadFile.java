package com.karlwelzel.uebungsblaetter;

import java.io.File;

/**
 * Created by karl on 19.10.17.
 */

public class DownloadFile {
    public final String url;
    public final File file;
    private String title = "";

    public DownloadFile(String url, File file) {
        this.url = url;
        this.file = file;
    }

    public DownloadFile(String url, File file, String title) {
        this.url = url;
        this.file = file;
        this.title = title;
    }

    public String getTitle() {
        if (title.equals("")) {
            return file.getName();
        } else {
            return title;
        }
    }
}
