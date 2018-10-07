package com.karlwelzel.uebungsblaetter;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by karl on 19.10.17.
 */

@SuppressWarnings("CanBeFinal")
public class DownloadDocument {
    public URL url;
    public File file;
    public String titleId;
    public String title;
    public int sheetNumber; // < 0: no sheet, >= 0: number of the sheet
    private Date date = null;
    private double points = -1;
    private int maximumPoints;

    public DownloadDocument(URL url, File file, String titleId, String title, int sheetNumber,
                            int maximumPoints) {
        this.url = url;
        this.file = file;
        this.titleId = titleId;
        this.title = title;
        this.sheetNumber = sheetNumber;
        this.maximumPoints = maximumPoints;
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

    public int getMaximumPoints() {
        return maximumPoints;
    }

    public void setMaximumPoints(int maximumPoints) {
        this.maximumPoints = maximumPoints;
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
                builder.append("/");
                builder.append(String.format(Locale.GERMAN, "%d", getMaximumPoints()));
                builder.append(" Punkte");
            }
        }
        return builder.toString();
    }
}
