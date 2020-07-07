package ru.geekbrains.common;

public enum CommandType {
    SEND_FILE((byte) 25),
    SEND_FILE_LIST((byte) 26),
    RECEIVE_FILE((byte) 27),
    AUTHORIZATION((byte) 28),
    AUTH_OK((byte) 30),
    AUTH_ERR((byte) 33),
    REGISTER((byte) 35),
    REG_OK((byte) 43),
    REG_ERR((byte) 44),
    RENAME((byte) 36),
    DELETE((byte) 39),
    CHANGE_PASS((byte) 45),
    CHANGE_PASS_OK((byte) 46),
    CHANGE_PASS_ERR((byte) 45),
    CREATE_DIR((byte) 41);

    private byte code;

    CommandType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
