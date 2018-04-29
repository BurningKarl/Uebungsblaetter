package com.karlwelzel.uebungsblaetter;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by karl on 19.10.17.
 */

public class DownloadDocument {
    public URL url;
    public File file;
    public String titleSuggestion;
    public String title;
    private Date date = null;
    private double points = -1;

    public static DownloadDocument fromManager(DownloadManager manager, URL url, File file,
                                               String titleSuggestion) {
        DownloadManagerSettings managerSettings = manager.getSettings();
        Pattern pattern = Pattern.compile(managerSettings.sheetRegex);
        Matcher matcher = pattern.matcher(titleSuggestion);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            Integer number = Integer.valueOf(matcher.group(1));
            titleSuggestion = MainActivity.getContext().getString(R.string.sheet_title_format, number);
        }
        if (managerSettings.titleMap.containsKey(titleSuggestion)) {
            return new DownloadDocument(url, file, titleSuggestion,
                    managerSettings.titleMap.get(titleSuggestion));
        } else {
            return new DownloadDocument(url, file, titleSuggestion, titleSuggestion);
        }
    }

    public DownloadDocument(URL url, File file, String titleSuggestion, String title) {
        this.url = url;
        this.file = file;
        this.titleSuggestion = titleSuggestion;
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
