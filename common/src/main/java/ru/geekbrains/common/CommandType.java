package ru.geekbrains.common;

public enum CommandType {
    SEND_FILE((byte) 25),
    SEND_FILE_LIST((byte) 26),
    RECEIVE_FILE((byte) 27),
    DELETE((byte) 28);

    private byte code;

    CommandType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
