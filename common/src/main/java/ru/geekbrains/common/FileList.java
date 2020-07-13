package ru.geekbrains.common;

import java.io.Serializable;
import java.util.List;

public class FileList implements Serializable {

    private List<FileMessage> list;
    private String url;

    public FileList(List<FileMessage> list, String url) {
        this.list = list;
        this.url =  url;
    }

    public List<FileMessage> getList() {
        return list;
    }

    public String getUrl() {
        return url;
    }
}
