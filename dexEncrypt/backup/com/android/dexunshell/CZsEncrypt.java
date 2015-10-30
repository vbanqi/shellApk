package com.android.dexunshell;

/**
 * Created by lvqiang on 15/3/16.
 */
public class CZsEncrypt {
    static {
        System.loadLibrary("endex");
    }

    public static native byte[] d(byte[] _byte,int offset,int len);

    public static native byte[] e(byte[] _byte,int offset,int len);
}
