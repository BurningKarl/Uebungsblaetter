package com.karlwelzel.uebungsblaetter;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;

/**
 * Created by karl on 19.10.17.
 */

public class DownloadDocument implements Serializable {
    public final URL url;
    public final File file;
    public final String title;
    private Date date;

    public DownloadDocument(URL url, File file, String title) {
        this.url = url;
        this.file = file;
        this.title = title;
    }

    public static DownloadDocument deserialize(String s) {
        if (s.equals("")) return null;
        byte[] data;
        try {
            data = Base64.decode(s, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            Log.e("DownloadDocument", "deserialize: IllegalArgumentException");
            e.printStackTrace();
            return null;
        }
        ObjectInputStream objectInputStream;
        try {
            objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
            DownloadDocument object = (DownloadDocument) objectInputStream.readObject();
            objectInputStream.close();
            return object;
        } catch (IOException | ClassNotFoundException e) {
            Log.e("DownloadDocument", "deserialize: Error");
            e.printStackTrace();
            return null;
        }
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String serialize() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
        } catch (IOException e) {
            Log.e("DownloadDocument", "serialize: Error");
            e.printStackTrace();
            return "";
        }
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

}
