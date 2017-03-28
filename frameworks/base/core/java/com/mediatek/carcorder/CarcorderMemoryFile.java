package com.mediatek.carcorder;

import java.io.FileDescriptor;
import java.io.IOException;

import android.util.Log;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

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
public final class CarcorderMemoryFile {

	private static final String TAG = "CarcorderMemoryFile";

    private long mAddress;
    private FileDescriptor mFd;

    //mPFD is created from mFd, and mPFd is only used for transferring Fd to Parcel
    // If we create the ParcelFileDescriptor object temporarily, and then pass it to Parcel
    // Then the Dalvik gc will release the ParcelFileDescriptor object, and the fd of FileDescriptor in ParcelFileDescriptor
    // will be release and close the fd
    // That is, the libcore.io.Posix.close will be invoked, and the native Posix_close() is invoked to release the fd
    private ParcelFileDescriptor mPFd;

    private int mSize;

    public CarcorderMemoryFile(String name, int size) throws IOException {
        openAshmem(name, size);
    }


	public CarcorderMemoryFile(FileDescriptor fd, int size) throws IOException {
		   openAshmem(fd, size);
	   }

	   public boolean openAshmem(FileDescriptor fd, int size) throws IOException {
		   mFd = CarcorderJni.nativeAshmemOpen(fd, size);
		   mPFd = new ParcelFileDescriptor(mFd);
		   mSize = size;
		   if (mFd.valid()) {
			   mAddress = CarcorderJni.nativeAshmemMmap(mFd, size, 0);
			   if (mAddress != 0) {
				   return true;
			   } else {
				   Log.w(TAG, "Failed to call nativeAshmemMap()");
			   }
		   } else {
			   Log.w(TAG, "Failed to openAshmem");
		   }

		   return false;
	   }

    public boolean openAshmem(String name, int size) throws IOException {
        if (name != null && size > 0) {
            mFd = CarcorderJni.nativeAshmemOpen(name, size);
            mPFd = new ParcelFileDescriptor(mFd);
            mSize = size;
            if (mFd.valid()) {
                mAddress = CarcorderJni.nativeAshmemMmap(mFd, size, 0);
                if (mAddress != 0) {
                    return true;
                } else {
                    Log.w(TAG, "Failed to call nativeAshmemMap()");
                }
            } else {
                Log.w(TAG, "Failed to openAshmem");
            }
        } else {
            Log.w(TAG, "Invalid arguments in openAshmem()");
        }

        return false;
    }

    public int writeBitmap(Bitmap bitmap) {
        int result = -1;
		if (!isDeactivated() && mFd.valid()) {
            result = CarcorderJni.nativeAshmemWriteBitmap(mFd, mAddress, bitmap);
        } else {
            Log.w(TAG, "Invalid arguments in writeBitmap()");
        }

        return result;
    }

    public int writeBytes(byte[] buffer, int srcOffset, int destOffset, int count) {
        int result = -1;
		if (!isDeactivated() && mFd.valid()) {
            result = CarcorderJni.nativeAshmemWriteBytes(mFd, mAddress, buffer, srcOffset, destOffset, count, false);
        } else {
            Log.w(TAG, "Invalid arguments in writeBytes()");
        }

        return result;
    }

	public int readBytes(byte[] buffer, int srcOffset, int destOffset, int count) throws IOException {
        return CarcorderJni.nativeAshmemReadBytes(mFd, mAddress, buffer, srcOffset, destOffset, count);
    }

    public void close() {
        if (!isDeactivated()) {
            try {
            CarcorderJni.nativeAshmemMunmap(mFd, mAddress, mSize);
            mAddress = 0;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFd.valid()) {
            // the fd is closed in native code
            CarcorderJni.nativeAshmemClose(mFd);
        }
    }

    public FileDescriptor getFileDescriptor() {
        return mFd;
    }

    public ParcelFileDescriptor getParcelFileDescriptor() {
        if (mPFd == null ) {
            Log.e(TAG, "Please check CarcorderMemoryFile methods, mPFd is null");
        }
        if(!mPFd.getFileDescriptor().valid()) {
            Log.w(TAG, "mPFd's fd isn't valid");
        }

        Log.d(TAG, "fd=" + mPFd.getFileDescriptor());

        return mPFd;
    }

    public int size() {
        return mSize;
    }

    /**
       * Checks whether the memory file has been deactivated.
       */
    private boolean isDeactivated() {
        return mAddress == 0;
    }
}
