package com.mediatek.carcorder;

import java.io.FileDescriptor;
import java.io.IOException;

import android.util.Log;
import android.graphics.Bitmap;

/**
 * <p>
 * A manager for detecting, characterizing, and connecting to carcorder service
 * that run in carcorderserver and register in {@link ServiceManager}. This call
 * is used to manager car related devices and events, such as cameras in car,
 * collision detection, reverse gear event and so on.
 * </p>
 *
 * <p>
 * You can get the singleton instance of this class by calling {@link #get()}
 * </p>
 *
 *
 */
public final class CarcorderJni {

	private static final String TAG = "CarcorderJni";

    static {
        try {
            System.loadLibrary("carcorderjni");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static native FileDescriptor nativeAshmemOpen(String name, int size) throws IOException;
    public static native FileDescriptor nativeAshmemOpen(FileDescriptor fd, int size) throws IOException;
    public static native long nativeAshmemMmap(FileDescriptor fd, int size, int prot) throws IOException;
    public static native void nativeAshmemMunmap(FileDescriptor fd, long address, int size) throws IOException;
    public static native int nativeAshmemWriteBitmap(FileDescriptor fd, long address, Bitmap bitmap);
    public static native int nativeAshmemWriteBytes(FileDescriptor fd, long address, byte[] bytes, int srcOffset, int destOffset, int count, boolean unpined);
    public static native int nativeAshmemReadBytes(FileDescriptor fd, long address, byte[] bytes, int srcOffset, int destOffset, int count) throws IOException;
    public static native int nativeAshmemClose(FileDescriptor fd);
}
