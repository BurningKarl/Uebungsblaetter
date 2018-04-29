package com.karlwelzel.uebungsblaetter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class DownloadManagerSettings {
    public String managerName;
    public URL directoryURL;
    public File directoryFile;

    public Integer maximumPoints = 10;
    public String sheetRegex = "";
    public ArrayList<String> stickiedTitles = new ArrayList<>();
    public HashMap<String, String> titleMap = new HashMap<>();

    public DownloadManagerSettings(String managerName, URL directoryURL, File directoryFile) {
        this.managerName = managerName;
        this.directoryURL = directoryURL;
        this.directoryFile = directoryFile;
    }
}
