package com.karlwelzel.uebungsblaetter;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by karl on 19.10.17.
 */

public class DownloadDocument implements Serializable {
    public final URL url;
    public final File file;
    public final String title;
    private Date date = null;
    private double points = -1;

    public DownloadDocument(URL url, File file, String title) {
        this.url = url;
        this.file = file;
        this.title = title;
    }

    public boolean equals(Object o) {
        return o instanceof DownloadDocument && this.url.equals(((DownloadDocument) o).url);
    }

    public int hashCode() {
        return this.url.hashCode();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    public String getSubtitle() {
        StringBuilder builder = new StringBuilder();
        if (getDate() != null) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT, Locale.GERMAN);
            builder.append(dateFormat.format(getDate()));
            if (getPoints() >= 0) {
                builder.append(" - ");
                builder.append(String.format(Locale.GERMAN, "%.1f", getPoints()));
                builder.append(" Punkte");
            }
        }
        return builder.toString();
    }
}
