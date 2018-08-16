package com.karlwelzel.uebungsblaetter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class DownloadManagerSettings {
    private String name;
    private URL directoryURL;
    private File parentDirectory;
    private File directory;
    private String managerID;

    public Integer maximumPoints = 10;
    public String sheetRegex = "";
    public ArrayList<String> stickiedTitles = new ArrayList<>();
    public HashMap<String, String> titleMap = new HashMap<>();
    public String username = "";
    public String password = "";

    public DownloadManagerSettings(String name, URL directoryURL, File parentDirectory) {
        this.name = name;
        this.directoryURL = directoryURL;
        this.parentDirectory = parentDirectory;
        this.managerID = directoryURL.toString().replaceAll("[^\\w\\s]", "");
        this.directory = new File(parentDirectory, managerID);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getDirectoryURL() {
        return directoryURL;
    }

    public void setDirectoryURL(URL directoryURL) {
        this.directoryURL = directoryURL;
        this.managerID = directoryURL.toString().replaceAll("[^\\w\\s]", "");
        this.directory = new File(parentDirectory, managerID);
    }

    public File getParentDirectory() {
        return parentDirectory;
    }

    public void setParentDirectory(File parentDirectory) {
        this.parentDirectory = parentDirectory;
        this.directory = new File(parentDirectory, managerID);
    }

    public File getDirectory() {
        return directory;
    }

    public String getManagerID() {
        return managerID;
    }
}
