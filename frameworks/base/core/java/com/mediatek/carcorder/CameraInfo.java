package com.mediatek.carcorder;

import android.os.Parcel;
import android.os.Parcelable;

public class CameraInfo implements Parcelable {
    
    /**
     * The facing of the camera is opposite to that of the screen.
     */
    public static final int CAMERA_FACING_FRONT = 0;
    
    /**
     * The facing of the camera is the same as that of the screen.
     */
    public static final int CAMERA_FACING_BACK_CVBS = 1;

    /**
     * The facing of the camera is the same as that of the screen.
     * That means this camera is the 2nd CVBS camera
     */
    public static final int CAMERA_FACING_BACK_CVBS_2 = 2;

    /**
      * The facing of the camera is the same as that of the screen.
      * That means this camera is the usb camera.
      */
    public static final int CAMERA_FACING_BACK_USB    = 3;
    
    /**
     * The direction that the camera faces. It should be
     * CAMERA_FACING_BACK or CAMERA_FACING_FRONT.
     */
    public int facing;
    
    /**
     * <p>The orientation of the camera image. The value is the angle that the
     * camera image needs to be rotated clockwise so it shows correctly on
     * the display in its natural orientation. It should be 0, 90, 180, or 270.</p>
     *
     * <p>For example, suppose a device has a naturally tall screen. The
     * back-facing camera sensor is mounted in landscape. You are looking at
     * the screen. If the top side of the camera sensor is aligned with the
     * right edge of the screen in natural orientation, the value should be
     * 90. If the top side of a front-facing camera sensor is aligned with
     * the right of the screen, the value should be 270.</p>
     *
     * @see #setDisplayOrientation(int)
     * @see Parameters#setRotation(int)
     * @see Parameters#setPreviewSize(int, int)
     * @see Parameters#setPictureSize(int, int)
     * @see Parameters#setJpegThumbnailSize(int, int)
     */
    public int orientation;

    

    
    /**
     * The facing of the camera is opposite to that of the screen.
     */
    public static final int CAMERA_MAIN_SENSOR = 0;
    
    /**
     * The facing of the camera is the same as that of the screen.
     */
    public static final int CAMERA_SUB_SENSOR = 1;

    /**
     * The facing of the camera is the same as that of the screen.
     * That means this camera is the 2nd CVBS camera
     */
    public static final int CAMERA_CVBS_DUAL_SENSOR = 2;

    /**
      * The facing of the camera is the same as that of the screen.
      * That means this camera is the usb camera.
      */
    public static final int CAMERA_USB_CAMERA    = 3;
    /**
      * The camera type
      */
    public int type;

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(facing);
        out.writeInt(orientation);
        out.writeInt(type);
    }

    public void readFromParcel(Parcel in) {
        facing = in.readInt();
        orientation = in.readInt();
        type = in.readInt();
    }

    public static final Parcelable.Creator<CameraInfo> CREATOR =
            new Parcelable.Creator<CameraInfo>() {
        @Override
        public CameraInfo createFromParcel(Parcel in) {
            CameraInfo info = new CameraInfo();
            info.readFromParcel(in);

            return info;
        }

        @Override
        public CameraInfo[] newArray(int size) {
            return new CameraInfo[size];
        }
    };

}
