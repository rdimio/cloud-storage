package ru.geekbrains.common;

import java.io.Serializable;
import java.util.List;

public class FileList implements Serializable {

    private List<FileMessage> fl;

    public FileList(List<FileMessage> fl) {
        this.fl = fl;
    }

    public List<FileMessage> getFl() {
        return fl;
    }
}
