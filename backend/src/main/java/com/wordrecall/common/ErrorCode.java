package com.wordrecall.common;

public final class ErrorCode {

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 40000;
    public static final int UNAUTHORIZED = 40100;
    public static final int FORBIDDEN = 40300;
    public static final int NOT_FOUND = 40400;
    public static final int CONFLICT = 40900;
    public static final int INTERNAL = 50000;
    public static final int WX_LOGIN_FAILED = 50001;

    private ErrorCode() {
    }
}
