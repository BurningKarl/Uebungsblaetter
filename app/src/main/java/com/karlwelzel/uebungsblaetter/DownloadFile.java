package com.karlwelzel.uebungsblaetter;

import java.io.File;

/**
 * Created by karl on 19.10.17.
 */

public class DownloadFile {
    public String url;
    public File file;
    public String title = "";

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
        if (title == "") {
            return file.getName();
        } else {
            return title;
        }
    }
}
