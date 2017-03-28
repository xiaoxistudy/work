package com.mediatek.carcorder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;

import android.media.CamcorderProfile;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.os.ParcelFileDescriptor;
import android.graphics.Bitmap;

/**
 * This class is used to manager the cameras in car. The design is reference
 * with both {@link android.hardware.Camera} and
 * {@link android.media.MediaRecorder}. It combines car customized preview,
 * record and other camera features, and more easy to use.
 * 
 * @see android.hardware.Camera
 * @see android.media.MediaRecorder
 */
public class CameraDevice {
	private static final String TAG = "CameraDevice";
	private final ICarCamDeviceUser mCameraDevice;
	private Object mLock = new Object();
	private Object mVideoFrameLock = new Object();
	private ADASCallback mADASCallback = null;
	private VideoCallback mVideoCallback = null;
	private AudioCallback mAudioCallback = null;
	private KeypointCallback mKeypointCallback = null;
	private MotionCallback mMotionCallback = null;
	private ShutterCallback mShutterCallback = null;
	private PictureCallback mPictureCallback = null;
	private RecordStatusCallback mRecordStatusCallback = null;
    private AeStatusCallback mAeStatusCallback = null;
	private ErrorCallback mErrorCallback = null;

    /**
        * define error type of startRecord
        */
    public static final int START_RECORD_SUCCESS = 0;       //no error ,call startRecord successful
	public static final int START_RECORD_CREATE_INSTANCE_FAIL = -1;  //create a record instance fail
	public static final int START_RECORD_SET_CAMERA_FAIL = -2;       //set camera or camera proxy fail
	public static final int START_RECORD_SET_OUTPUT_FAIL = -3;       //set or create output dir fail
	public static final int START_RECORD_PREPARE_FAIL = -4;          //carrecorder call prepare function to start fail
	public static final int START_RECORD_START_FAIL = -5;            //carrecorder call start function fail
	public static final int START_RECORD_EXCEPTION = -19;            //happend RemoteException ,maybe binder had died
	
	   
	public static final int CAMERA_STATUS_ERROR = 1;
	public static final int CAMERA_STATUS_SHUTTER = 2;
	public static final int CAMERA_STATUS_VIDEO = 3;
	public static final int CAMERA_STATUS_MOTION = 4;
	public static final int CAMERA_PICTURE_TAKEN = 5;
	public static final int RECORD_STATUS_CHANGED = 6;
    public static final int CAMERA_STATUS_AE = 7;
	public static final int CAMERA_ERROR_OCCURED = 8;
	
    public static final int DATA_TYPE_KEYPOINT = 502;
	
    private byte[] mVideoData=new byte[1];  //need to initiation
    private byte[] mAudioData=new byte[1];
	
	private final ICameraDeviceListener LISTENER = new ICameraDeviceListener.Stub() {
		@Override
		public void onStatusChanged(int event, int arg1, String arg2, int arg3)
				throws RemoteException {

			Log.d(TAG, "onStatusChanged is invoked: event " + event + ", arg1 "
					+ arg1 + ", arg2 " + arg2 + ", arg3 " + arg3);
			switch (event) {
			case CAMERA_STATUS_VIDEO:
				synchronized (mLock) {
					if (mVideoCallback != null) {
						mVideoCallback.onVideoTaken(arg1, arg3, arg2);
					} else {
						Log.w(TAG, "mVideoCallback is null");
					}
				}
				break;
			case CAMERA_STATUS_MOTION:
				synchronized (mLock) {
					if (mMotionCallback != null) {
						mMotionCallback.onMotionCallback(arg1);
					}
				}
				break;
			case CAMERA_STATUS_SHUTTER:
				synchronized (mLock) {
					if (mShutterCallback != null) {
						mShutterCallback.onShutter();
					}
				}
				break;
			case CAMERA_PICTURE_TAKEN:
				synchronized (mLock) {
					if (mPictureCallback != null) {
						mPictureCallback.onPictureTaken(arg2);
					}
				}
				break;
			case RECORD_STATUS_CHANGED:
				synchronized (mLock) {
					if (mRecordStatusCallback != null) {
						mRecordStatusCallback.onRecordStatusChanged(arg1,arg3);
					}
				}
                break;
            case CAMERA_STATUS_AE:
                synchronized (mLock) {
                    if (mAeStatusCallback != null) {
                        mAeStatusCallback.onStatusCallback(arg1);
                    }
                }
                break;
			case CAMERA_ERROR_OCCURED:
                synchronized (mLock) {
                    if (mErrorCallback != null) {
                        mErrorCallback.onError(arg1,arg3,CameraDevice.this);
                    }
                }
				break;
			default:
				break;
			}
		}

		@Override
		public void onADASCallback(ADASInfo info) throws RemoteException {
			synchronized (mLock) {
				if (mADASCallback != null) {
					mADASCallback.onADASCallback(info);
				}
			}
		}
		
		@Override
        public void onVideoFrame(ParcelFileDescriptor fd ,int dataType, int size) {
            synchronized (mVideoFrameLock) {
				switch(dataType){
                   case DATA_TYPE_KEYPOINT:
                      if (mKeypointCallback != null) {
					  	   if (mVideoData.length < size) {
                               mVideoData = new byte[size];
                           }
		                   if (size == readAshmemBytes(fd, size, mVideoData)) {
							   mKeypointCallback.onKeypointFrame(mVideoData,dataType, size);
		                   }
					   } else {
						  Log.w(TAG, "mKeypointCallback is null");
					   }
					  break;
				   default:
					  if (mVideoCallback != null) {
					  	   if (mVideoData.length < size) {
                               mVideoData = new byte[size];
                           }
		                   if (size == readAshmemBytes(fd, size, mVideoData)) {
							   mVideoCallback.onVideoFrame(mVideoData,dataType, size);
		                   }
					   } else {
						  Log.w(TAG, "mVideoCallback is null");
					   }
					  break;
				}
				closeFd(fd);
			}
        }
		
		@Override
        public void onAudioFrame(ParcelFileDescriptor fd ,int dataType, int size) {
            synchronized (mLock) {
                if (mAudioCallback != null) {
                    if (mAudioData.length < size) {
                        mAudioData = new byte[size];
                    }
                    if (size == readAshmemBytes(fd, size, mAudioData)) {
					    mAudioCallback.onAudioFrame(mAudioData,dataType, size);
                    }
				} else {
					Log.w(TAG, "mAudioCallback is null");
				}
				closeFd(fd);	
            }
		}
	};

	/**
	 * Interface definition for a callback to be invoked when camera post ADAS
	 * message.
	 */
	public interface ADASCallback {
		/**
		 * Notify the listener of the detected ADAS info in the preview frame.
		 * 
		 * @param info
		 *            the detected adas info
		 */
		void onADASCallback(ADASInfo info);
	}

	/**
	 * Interface definition for a callback to be invoked when motion be
	 * detected.
	 */
	public interface MotionCallback {
		/**
		 * Notify the listener of the detected motion in the preview frame.
		 * 
		 * @param motion
		 *            motion is detected when motion is 1
		 */
		void onMotionCallback(int motion);
	}

	/**
	 * Interface definition for a callback to be invoked when a recording video
	 * completed.
	 */

	public interface VideoCallback {
		public static final int VIDEO_EVENT_ADD_FILE_IN_GALLERY = 0;
		public static final int VIDEO_EVENT_DELETE_FILE_IN_GALLERY = 1;
		public static final int VIDEO_EVENT_SDCARD_FULL = 2;
		public static final int VIDEO_EVENT_RECORD_STATUS_START = 3;
		public static final int VIDEO_EVENT_RECORD_STATUS_STOP = 4;
		public static final int VIDEO_EVENT_RECORD_RECORDING_ERROR = 5;
		public static final int VIDEO_EVENT_RECORD_SDCARD_DAMAGED = 6;
        public static final int VIDEO_EVENT_LOWRES_KEYPOINT_START = 7;
		public static final int VIDEO_EVENT_LOWRES_KEYPOINT_STOP  = 8;
		public static final int VIDEO_EVENT_KEYPOINT_START  = 9;
		public static final int VIDEO_EVENT_KEYPOINT_STOP  = 10;
		/**
		 * Notify the callback that the recording video is completed.
		 * 
		 * @param videoname
		 *            the file name of the recording video
		 */
		void onVideoTaken(int eventType, int cameraId, String videoname);
        
        /**
              * Notify the video frame
              * 
              * @param data
              *            video frame data
              * @param size
              *            the size of video frame data
              */
        void onVideoFrame(byte[] data,int dataType, int size);
	}

	/**
	 * Interface definition for a callback to be invoked when audio data decode 
	 * completed.
	 */
	public interface AudioCallback {
			/**
			 * Notify the audio frame
			* 
			* @param data
			*		   audio frame data
			* @param size
			*		   the size of audio frame data
			*/
			void onAudioFrame(byte[] data,int dataType, int size);
	
	}
	
    public interface KeypointCallback {
		/**
		 * Notify the keypoint data 
		* @param data
		* 		   keypoint data,include video and audio
		* @param size
		* 		   the size of keypoint data
		*/
		void onKeypointFrame(byte[] data,int dataType, int size);

	}
    /**
	 * Interface definition for a record status,it will notify  onRecordStatusChanged if 
	 * record status had changed.
	 */
    public interface RecordStatusCallback{
        public static final int RECORD_STATUS_START		  = 0;
	    public static final int RECORD_STATUS_STOP		  = 1;
	    public static final int RECORD_STATUS_RECODING	  = 2;
		 
        void onRecordStatusChanged(int status,int cameraId);
    }
    
	public interface ErrorCallback{
        public static final int ERROR_CAMERA_DISCONNECTED = 1;
		public static final int ERROR_CAMERA_UNAVAILABLE =  2;
		
	   /**
		* Notify to app when use cameradevice error occured 
		*
		* @param error
		* 		   the type of error
		* @param cameraId
		* 		   the id of camera
		* @param camera
		* 		   the instance of CameraDevice
		*/
		void onError(int error,int cameraId,CameraDevice camera);
    }
	
	/**
	 * Registers a callback to be notified about the ADAS info detected in
	 * preview frame.
	 * 
	 * @param callback
	 *            the callback to notify
	 */
	public void setADASCallback(ADASCallback callback) {
		synchronized (mLock) {
			mADASCallback = callback;
		}
	}

	/**
	 * Registers a callback to monitor if video is completed.
	 * 
	 * @param callback
	 *            the callback to notify
	 * 
	 */
	public void setVideoCallback(VideoCallback callback) {
		synchronized (mLock) {
			mVideoCallback = callback;
		}
	}

	/**
	 * Registers a callback to monitor if audio data decode is completed.
	 * 
	 * @param callback
	 *            the callback to notify
	 * 
	 */
	public void setAudioCallback(AudioCallback callback) {
		synchronized (mLock) {
			mAudioCallback = callback;
		}
	}

	/**
	 * Registers a callback to monitor motion action.
	 * 
	 */
	public void setMotionCallback(MotionCallback callback) {
		synchronized (mLock) {
			mMotionCallback = callback;
		}
	}

    /**
	 * Registers a callback to be notify when record status changed.
	 * 
	 */
	public void setRecordStatusCallback(RecordStatusCallback callback){
        synchronized (mLock) {
			mRecordStatusCallback = callback;
		}
	}

	/**
	 * Registers a callback to receive keypoint data.
	 * 
	 */
	public void setKeypointCallback(KeypointCallback callback){
        synchronized (mLock) {
			mKeypointCallback = callback;
		}
	}

	/**
	 * Interface definition for a callback to be invoked to get the AeLv after queryAeLv() API is invoked 
	 */
    public interface AeStatusCallback {
        void onStatusCallback(int value);
    }


    /**
	 * Registers a callback to be notified after queryAeLv() API is invoked .
	 * 
	 */
    public void setAeStatusCallback(AeStatusCallback callback) {
        synchronized (mLock) {
            mAeStatusCallback = callback;
        }
    }

   /**
	* Registers a callback to be notified when using camera occure error,
      * 
	*/
   public void setErrorCallback(ErrorCallback callback){
        synchronized (mLock) {
            mErrorCallback = callback;
        }
   }
	
	public CameraDevice(ICarCamDeviceUser device) {
		mCameraDevice = device;
		try {
			mCameraDevice.addListener(LISTENER);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class Size {
		/**
		 * Sets the dimensions for pictures.
		 * 
		 * @param w
		 *            the photo width (pixels)
		 * @param h
		 *            the photo height (pixels)
		 */
		public Size(int w, int h) {
			width = w;
			height = h;
		}

		/**
		 * Compares {@code obj} to this size.
		 * 
		 * @param obj
		 *            the object to compare this size with.
		 * @return {@code true} if the width and height of {@code obj} is the
		 *         same as those of this size. {@code false} otherwise.
		 */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Size)) {
				return false;
			}
			Size s = (Size) obj;
			return width == s.width && height == s.height;
		}

		@Override
		public int hashCode() {
			return width * 32713 + height;
		}

		/** width of the picture */
		public int width;
		/** height of the picture */
		public int height;
	};

   /**
	*  definition of supported video file format
	*/
    public enum OutputFormat{
           MPEG_4    (2),
		   MPEG2TS   (8);

           OutputFormat(int format){
                this.format=format;
           	}
		   final int format;
	}

	public enum VideoFrameMode{
           DISABLE   (0),
		   SOURCE    (1),
		   PACKET    (2),
		   DUAL      (3);
		   
		   VideoFrameMode(int mode){
              this.mode=mode;
		   }
           final int mode;
	}
	
	/**
	 * Camera device settings.
	 * 
	 * <p>
	 * To make camera parameters take effect, applications have to call
	 * {@link CameraDevice#setParameters(Camera.Parameters)}. For example, after
	 * {@link CameraDevice.Parameters#setVideoProfile} is called, video profile
	 * is not actually changed until
	 * {@link CameraDevice#setParameters(Camera.Parameters)} is called with the
	 * changed parameters object.
	 * 
	 * <p>
	 * Different devices may have different camera capabilities, such as picture
	 * size or video size. The application should query the camera capabilities
	 * before setting parameters. For example, the application should call
	 * {@link CameraDevice.Parameters#getSupportedVideoSizes()} before calling
	 * {@link CameraDevice.Parameters#setVideoSize(int, int)}.
	 * 
	 */
	public class Parameters {
		private LinkedHashMap<String, String> mMap;

		private static final String SUPPORTED_VALUES_SUFFIX = "-values";

		private static final String TRUE = "true";
		private static final String FALSE = "false";

		// Parameter keys to communicate with the camera framework
		private static final String KEY_PREVIEW_SIZE = "preview-size";
		private static final String KEY_VIDEO_SIZE = "video-size";
		private static final String KEY_VIDEO_ROTATE_SIZE = "video-rotate-size";
		private static final String KEY_VIDEO_ROTATE_DURATION = "video-rotate-duration";
		private static final String KEY_VIDEO_MUTE_AIUDO = "mute-recording_audio";
		private static final String KEY_MOTION_DETECT_MODE = "motion_detect_mode";
		private static final String KEY_VIDEO_OUTPUT_FORMAT = "video-output-format";
		private static final String KEY_VIDEO_FRAME_RATE = "video-frame-rate";
		private static final String KEY_VIDEO_PARAM_ENCODING_BITRATE = "video-param-encoding-bitrate";
		private static final String KEY_VIDEO_ENCODER = "video-encoder";
		private static final String KEY_VIDEO_PARAM_CAMERA_ID = "video-param-camera-id";
		private static final String KEY_AUDIO_PARAM_ENCODING_BITRATE = "audio-param-encoding-bitrate";
		private static final String KEY_AUDIO_PARAM_NUMBER_OF_CHANNELS = "audio-param-number-of-channels";
		private static final String KEY_AUDIO_PARAM_SAMPLING_RATE = "audio-param-sampling-rate";
		private static final String KEY_AUDIO_ENCODER = "audio-encoder";
		private static final String KEY_VIDEO_OUTPUT_FILE = "video-output-file";
		private static final String KEY_VIDEO_OUTPUT_FILE_NAME = "video-output-file-name";
		private static final String KEY_VIDEO_LOCK_FILE = "video-lock-file";
		private static final String KEY_WATERMARK_OFFSET = "watermark-offset";
		private static final String KEY_WATERMARK_TIMER_OFFSET = "watermark-timer-offset";
		private static final String KEY_WATERMARK_ADDRESS = "watermark-address";
		private static final String KEY_WATERMARK_SIZE = "watermark-size";
		private static final String KEY_WATERMARK_DATA_PATH = "watermark-data-path";
		private static final String KEY_WATERMARK_OWNER_NAME = "watermark-owner-name";
		private static final String KEY_WHITE_BALANCE = "whitebalance";
		private static final String KEY_AUTO_WHITEBALANCE_LOCK = "auto-whitebalance-lock";
		private static final String KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
		private static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
		private static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
		private static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
		private static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
        private static final String KEY_FREE_SIZE_LIMIT= "free-size-limit";
        private static final String KEY_ADAS_SIZE = "adas-size";
		private static final String KEY_ADAS_END_POINT = "adas-end-point";
        private static final String KEY_ADAS_SCALE_FACTOR = "adas-scale-factor";
        private static final String KEY_ADAS_FOCAL_LENGTH = "adas-focal-length";
        private static final String KEY_MAIN_VIDEO_FRAME_ENABLED= "main-video-frame-enabled";
		private static final String KEY_USER_SET_VIDEO_ENCODEBITRATE= "user_set_video_encod_bitrate";
        private static final String KEY_VIDEO_PARAM_ENCODING_MINBITRATE = "video-param-encoding-minbitrate";
		private static final String KEY_VIDEO_PARAM_ENCODING_MAXBITRATE = "video-param-encoding-maxbitrate";
        private static final String KEY_KEYPOINT_SPAN_LIMIT = "keypoint-span-limit";
		
        //some setting for sub video data
        private static final String KEY_SUB_VIDEO_SIZE = "videocb-size";
		private static final String KEY_SUB_VIDEO_FRAME_RATE = "sub-video-frame-rate";
		private static final String KEY_SUB_VIDEO_FRAME_ENABLED= "sub-video-frame-enabled";
        private static final String KEY_SUB_VIDEO_PARAM_ENCODING_BITRATE = "sub-video-param-encoding-bitrate";

		//for new watermark function settings
		private static final String KEY_PREVIEW_WATERMARK_TEXT_MODE="preview-watermark-text-mode";
		private static final String KEY_RECORD_WATERMARK_TEXT_MODE="record-watermark-text-mode";
        private static final String KEY_PICTURE_WATERMARK_TEXT_MODE = "picture-watermark-text-mode";
        private static final String KEY_VIDEOCB_WATERMARK_TEXT_MODE = "videocb-watermark-text-mode";
		private static final String KEY_WATERMARK_AREA="watermark-area";
		private static final String KEY_WATERMARK_TEXT="watermark-text";
		private static final String KEY_WATERMARK_TEXT_SIZE="watermark-text-size";
		private static final String KEY_WATERMARK_TEXT_X="watermark-text-x";
		private static final String KEY_WATERMARK_TEXT_Y="watermark-text-y";
		private static final String KEY_WATERMARK_TEXT_COLOR="watermark-text-color";
		private static final String KEY_WATERMARK_TIMESTAMP_FORMAT="watermark-timestamp-format";
        private static final String KEY_PREVIEW_WATERMARK_IMG_MODE = "preview-watermark-img-mode";
        private static final String KEY_RECORD_WATERMARK_IMG_MODE = "record-watermark-img-mode";
        private static final String KEY_PICTURE_WATERMARK_IMG_MODE = "picture-watermark-img-mode";
        private static final String KEY_VIDEOCB_WATERMARK_IMG_MODE = "videocb-watermark-img-mode";
        private static final String KEY_WATERMARK_IMG_PATH = "watermark-img-path";
        private static final String KEY_WATERMARK_IMG_AREA = "watermark-img-area";
        private static final String KEY_WATERMARK_TEXT_MODE = "watermark-text-mode";
        private static final String KEY_WATERMARK_IMG_MODE = "watermark-img-mode";


		// Values for white balance settings
		public static final String WHITE_BALANCE_AUTO = "auto";
		public static final String WHITE_BALANCE_INCANDECSENT = "incandescent";
		public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
		public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
		public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
		public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
		public static final String WHITE_BALANCE_TWILIGHT = "twilight";
		public static final String WHITE_BALANCE_SHADE = "shade";
        public static final String WHITE_BALANCE_TUNGSTEN = "tungsten";

        // some apis of Parameters  from Camera.java
        private static final String KEY_PREVIEW_FPS_RANGE = "preview-fps-range";
        private static final String KEY_SCENE_MODE = "scene-mode";
		private static final String KEY_EIS_MODE = "eis-mode";
		private static final String KEY_VIDEO_STABILIZATION = "video-stabilization";
        private static final String KEY_VIDEO_STABILIZATION_SUPPORTED = "video-stabilization-supported";
		// 
        private static final String KEY_PICTURE_SIZE = "picture-size";
		private static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
        private static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
        private static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
        private static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
        private static final String KEY_JPEG_QUALITY = "jpeg-quality";
        private static final String KEY_RECORDING_HINT = "recording-hint";
        private static final String KEY_ROTATION = "rotation";
        private static final String KEY_VIDEO_ROTATION = "video-rotation";
        //
        private static final String KEY_GPS_LATITUDE = "gps-latitude";
        private static final String KEY_GPS_LONGITUDE = "gps-longitude";
        private static final String KEY_GPS_ALTITUDE = "gps-altitude";
        private static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
        private static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
		
        // HDR
        public static final String KEY_VIDEO_HDR = "video-hdr";
        public static final String KEY_VIDEO_HDR_MODE = "video-hdr-mode";
        public static final String VIDEO_HDR_MODE_IVHDR = "video-hdr-mode-ivhdr";
        public static final String VIDEO_HDR_MODE_MVHDR = "video-hdr-mode-mvhdr";


		private Parameters() {
			mMap = new LinkedHashMap<String, String>(128);
		}

		/**
		 * Takes a flattened string of parameters and adds each one to this
		 * Parameters object.
		 * <p>
		 * The {@link #flatten()} method does the reverse.
		 * </p>
		 * 
		 * @param flattened
		 *            a String of parameters (key-value paired) that are
		 *            semi-colon delimited
		 */
		public void unflatten(String flattened) {
			mMap.clear();

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(
					';');
			splitter.setString(flattened);
			for (String kv : splitter) {
				int pos = kv.indexOf('=');
				if (pos == -1) {
					continue;
				}
				String k = kv.substring(0, pos);
				String v = kv.substring(pos + 1);
				mMap.put(k, v);
			}
		}

		/**
		 * Creates a single string with all the parameters set in this
		 * Parameters object.
		 * <p>
		 * The {@link #unflatten(String)} method does the reverse.
		 * </p>
		 * 
		 * @return a String with all values from this Parameters object, in
		 *         semi-colon delimited key-value pairs
		 */
		public String flatten() {
			StringBuilder flattened = new StringBuilder(128);
			for (String k : mMap.keySet()) {
				flattened.append(k);
				flattened.append("=");
				flattened.append(mMap.get(k));
				flattened.append(";");
			}
			// chop off the extra semicolon at the end
			flattened.deleteCharAt(flattened.length() - 1);
			return flattened.toString();
		}

		public void remove(String key) {
            mMap.remove(key);
        }
		
        // Splits a comma delimited string to an ArrayList of String.
        // Return null if the passing string is null or the size is 0.
        private ArrayList<String> split(String str) {
            if (str == null) return null;

            TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<String> substrings = new ArrayList<String>();
            for (String s : splitter) {
                substrings.add(s);
            }
            return substrings;
		}

		public String get(String key) {
			return mMap.get(key);
		}

      /**
           * Returns the value of an integer parameter.
           *
           * @param key the key name for the parameter
           * @return the int value of the parameter
           */
        public int getInt(String key) {
            return Integer.parseInt(mMap.get(key));
        }

		/**
		 * Returns the value of a float parameter
		 */
		private float getFloat(String key, float defaultValue) {
			try {
				return Float.parseFloat(mMap.get(key));
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return defaultValue;
			}
		}

		/**
		 * Returns the value of an integer parameters.
		 */
		private int getInt(String key, int defaultValue) {
			try {
				return Integer.parseInt(mMap.get(key));
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return defaultValue;
			}
		}

		private void put(String key, String value) {
			/*
			 * Remove the key if it already exists.
			 * 
			 * This way setting a new value for an already existing key will
			 * always move that key to be ordered the latest in the map.
			 */
			mMap.remove(key);
			mMap.put(key, value);
		}

		public void set(String key, String value) {
			if (key.indexOf('=') != -1 || key.indexOf(';') != -1
					|| key.indexOf(0) != -1) {
				Log.e(TAG, "Key \"" + key
						+ "\" contains invalid character (= or ; or \\0)");
				return;
			}
			if (value.indexOf('=') != -1 || value.indexOf(';') != -1
					|| value.indexOf(0) != -1) {
				Log.e(TAG, "Value \"" + value
						+ "\" contains invalid character (= or ; or \\0)");
				return;
			}

			put(key, value);
		}

		/**
		 * Sets an integer parameter.
		 * 
		 * @param key
		 *            the key name for the parameter
		 * @param value
		 *            the int value of the parameter
		 */
		public void set(String key, int value) {
			put(key, Integer.toString(value));
		}


		/**
		 * Sets an integer parameter.
		 * 
		 * @param key
		 *            the key name for the parameter
		 * @param value
		 *            the float value of the parameter
		 */
		public void set(String key, float value) {
			put(key, Float.toString(value));
		}

		// Splits a comma delimited string to an ArrayList of Size.
		// Return null if the passing string is null or the size is 0.
		private ArrayList<Size> splitSize(String str) {
			if (str == null)
				return null;

			TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(
					',');
			splitter.setString(str);
			ArrayList<Size> sizeList = new ArrayList<Size>();
			for (String s : splitter) {
				Size size = strToSize(s);
				if (size != null)
					sizeList.add(size);
			}
			if (sizeList.size() == 0)
				return null;
			return sizeList;
		}

		// Parses a string (ex: "480x320") to Size object.
		// Return null if the passing string is null.
		private Size strToSize(String str) {
			if (str == null)
				return null;

			int pos = str.indexOf('x');
			if (pos != -1) {
				String width = str.substring(0, pos);
				String height = str.substring(pos + 1);
				return new Size(Integer.parseInt(width),
						Integer.parseInt(height));
			}
			return null;
		}

		private ArrayList<int[]> splitRange(String str) {
            if (str == null || str.charAt(0) != '('
                    || str.charAt(str.length() - 1) != ')') {
                Log.e(TAG, "Invalid range list string=" + str);
                return null;
            }

            ArrayList<int[]> rangeList = new ArrayList<int[]>();
            int endIndex, fromIndex = 1;
            do {
                int[] range = new int[2];
                endIndex = str.indexOf("),(", fromIndex);
                if (endIndex == -1) endIndex = str.length() - 1;
                splitInt(str.substring(fromIndex, endIndex), range);
                rangeList.add(range);
                fromIndex = endIndex + 3;
            } while (endIndex != str.length() - 1);

            if (rangeList.size() == 0) return null;
            return rangeList;
        }

		private void splitInt(String str, int[] output) {
            if (str == null) return;

            TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            int index = 0;
            for (String s : splitter) {
                output[index++] = Integer.parseInt(s);
            }
        }

		/**
		 * Gets the supported preview sizes.
		 * 
		 * @return a list of Size object. This method will always return a list
		 *         with at least one element.
		 */
		public List<Size> getSupportedPreviewSizes() {
			String values = get(KEY_PREVIEW_SIZE + SUPPORTED_VALUES_SUFFIX);
			return splitSize(values);
		}

		/**
		 * Sets the dimensions for preview pictures. If the preview has already
		 * started, applications should stop the preview first before changing
		 * preview size.
		 * 
		 * 
		 * @param width
		 *            the width of the pictures, in pixels
		 * @param height
		 *            the height of the pictures, in pixels
		 */
		public void setPreviewSize(int width, int height) {
			String v = Integer.toString(width) + "x" + Integer.toString(height);
			set(KEY_PREVIEW_SIZE, v);
		}

		/**
		 * Returns the dimensions setting for preview pictures.
		 * 
		 * @return a Size object with the width and height setting for the
		 *         preview picture
		 */
		public Size getPreviewSize() {
			String pair = get(KEY_PREVIEW_SIZE);
			return strToSize(pair);
		}


        /**
             * <p>Sets the dimensions for pictures.</p>
             *
             * <p>Applications need to consider the display orientation. See {@link
             * #setPreviewSize(int,int)} for reference.</p>
             *
             * @param width  the width for pictures, in pixels
             * @param height the height for pictures, in pixels
             * @see #setPreviewSize(int,int)
             *
             */
        public void setPictureSize(int width, int height) {
			String str = Integer.toString(width) + "x"
					+ Integer.toString(height);
			set(KEY_PICTURE_SIZE, str);
        }


        /**
             * Returns the dimension setting for pictures.
             *
             * @return a Size object with the height and width setting
             *          for pictures
             */
        public Size getPictureSize() {
            String str = get(KEY_PICTURE_SIZE);
            return strToSize(str);
        }

        /**
             * Gets the supported picture sizes.
             *
             * @return a list of supported picture sizes. This method will always
             *         return a list with at least one element.
             */
        public List<Size> getSupportedPictureSizes() {
            String str = get(KEY_PICTURE_SIZE + SUPPORTED_VALUES_SUFFIX);
            return splitSize(str);
        }
		
        /**
	         * <p>Sets the dimensions for EXIF thumbnail in Jpeg picture. If
	         * applications set both width and height to 0, EXIF will not contain
	         * thumbnail.</p>
	         *
	         * <p>Applications need to consider the display orientation. See {@link
	         * #setPreviewSize(int,int)} for reference.</p>
	         *
	         * @param width  the width of the thumbnail, in pixels
	         * @param height the height of the thumbnail, in pixels
	         * @see #setPreviewSize(int,int)
	         */
        public void setJpegThumbnailSize(int width, int height) {
            set(KEY_JPEG_THUMBNAIL_WIDTH, width);
            set(KEY_JPEG_THUMBNAIL_HEIGHT, height);
        }

        /**
	         * Returns the dimensions for EXIF thumbnail in Jpeg picture.
	         *
	         * @return a Size object with the height and width setting for the EXIF
	         *         thumbnails
	         */
        public Size getJpegThumbnailSize() {
            return new Size(getInt(KEY_JPEG_THUMBNAIL_WIDTH),
                            getInt(KEY_JPEG_THUMBNAIL_HEIGHT));
        }

        /**
	         * Gets the supported jpeg thumbnail sizes.
	         *
	         * @return a list of Size object. This method will always return a list
	         *         with at least two elements. Size 0,0 (no thumbnail) is always
	         *         supported.
	         */
        public List<Size> getSupportedJpegThumbnailSizes() {
            String str = get(KEY_JPEG_THUMBNAIL_SIZE + SUPPORTED_VALUES_SUFFIX);
            return splitSize(str);
        }

        /**
	         * Sets the quality of the EXIF thumbnail in Jpeg picture.
	         *
	         * @param quality the JPEG quality of the EXIF thumbnail. The range is 1
	         *                to 100, with 100 being the best.
	         */
        public void setJpegThumbnailQuality(int quality) {
            set(KEY_JPEG_THUMBNAIL_QUALITY, quality);
        }

        /**
	         * Returns the quality setting for the EXIF thumbnail in Jpeg picture.
	         *
	         * @return the JPEG quality setting of the EXIF thumbnail.
	         */
        public int getJpegThumbnailQuality() {
            return getInt(KEY_JPEG_THUMBNAIL_QUALITY);
        }

        /**
	         * Sets Jpeg quality of captured picture.
	         *
	         * @param quality the JPEG quality of captured picture. The range is 1
	         *                to 100, with 100 being the best.
	         */
        public void setJpegQuality(int quality) {
            set(KEY_JPEG_QUALITY, quality);
        }

        /**
	         * Returns the quality setting for the JPEG picture.
	         *
	         * @return the JPEG picture quality setting.
	         */
        public int getJpegQuality() {
            return getInt(KEY_JPEG_QUALITY);
        }
		
		/**
            * Sets the minimum and maximum preview fps. The minimum and
            * maximum preview fps must be one of the elements from {@link
            * #getSupportedPreviewFpsRange}.
            *
            * @param min the minimum preview fps (scaled by 1000).
            * @param max the maximum preview fps (scaled by 1000).
           */
        public void setPreviewFpsRange(int min, int max) {
            set(KEY_PREVIEW_FPS_RANGE, "" + min + "," + max);
        }
		 
		/**
		* Returns the current minimum and maximum preview fps. The values are
		* one of the elements returned by {@link #getSupportedPreviewFpsRange}.
		*
		* @return range the minimum and maximum preview fps (scaled by 1000).
		*/
		public void getPreviewFpsRange(int[] range) {
			if (range == null || range.length != 2) {
				throw new IllegalArgumentException(
								"range must be an array with two elements.");
				}
				splitInt(get(KEY_PREVIEW_FPS_RANGE), range);
		}

      /**
           * Gets the supported preview fps (frame-per-second) ranges. Each range
           * contains a minimum fps and maximum fps. If minimum fps equals to
           * maximum fps, the camera outputs frames in fixed frame rate. If not,
           * the camera outputs frames in auto frame rate. 
           *
           * @return a list of supported preview fps ranges. This method returns a
           *         list with at least one element. Every element is an int array
           *         of two values - minimum fps and maximum fps. The list is
           *         sorted from small to large (first by maximum fps and then
           *         minimum fps).
           */
        public List<int[]> getSupportedPreviewFpsRange() {
            String str = get(KEY_PREVIEW_FPS_RANGE + SUPPORTED_VALUES_SUFFIX);
            return splitRange(str);
        }

		/**
		 * Set the video size.
		 */
		public void setVideoSize(int width, int height) {
			String v = Integer.toString(width) + "x" + Integer.toString(height);
			set(KEY_VIDEO_SIZE, v);
		}

		/**
		 * Returns the dimensions setting for video pictures.
		 * 
		 * @return a Size object with the width and height setting for the video
		 *         picture
		 */
		public Size getVideoSize() {
			String pair = get(KEY_VIDEO_SIZE);
			return strToSize(pair);
		}

		/**
		 * Gets the supported preview sizes.
		 * 
		 * @return a list of Size object.
		 */
		public List<Size> getSupportedVideoSizes() {
			String values = get(KEY_VIDEO_SIZE + SUPPORTED_VALUES_SUFFIX);
			return splitSize(values);
		}

	  /**
           * Gets the current scene mode setting.
           *
           * @return one of SCENE_MODE_XXX string constant. null if scene mode
           *         setting is not supported.
           */
        public String getSceneMode() {
            return get(KEY_SCENE_MODE);
        }

       /**
            * Sets the scene mode. Changing scene mode may override other
            * parameters (such as flash mode, focus mode, white balance). For
            * example, suppose originally flash mode is on and supported flash
            * modes are on/off. In night scene mode, both flash mode and supported
            * flash mode may be changed to off. After setting scene mode,
            * applications should call getParameters to know if some parameters are
            * changed.
            *
            * @param value scene mode.
            * @see #getSceneMode()
            */
        public void setSceneMode(String value) {
            set(KEY_SCENE_MODE, value);
        }

       /**
            * Gets the supported scene modes.
            *
            * @return a list of supported scene modes. null if scene mode setting
            *         is not supported.
            * @see #getSceneMode()
            */
        public List<String> getSupportedSceneModes() {
            String str = get(KEY_SCENE_MODE + SUPPORTED_VALUES_SUFFIX);
            return split(str);
        }

	   
	   /**
		 * @hide
		 * Gets the current Eis mode setting (on/off)
		 * @ return one of EIS_MODE_xxx string constant.
		 */
		 public String getEisMode() {
			 return get(KEY_EIS_MODE);
		 }
	    /**
		 * @hide
		 */
		 public void setEisMode(String eis) {
			  set(KEY_EIS_MODE, eis);
		 }
		/**
		 * @hide
		 */
		 public List<String> getSupportedEisMode() {
			 String str = get(KEY_EIS_MODE + SUPPORTED_VALUES_SUFFIX);
			 return split(str);
		 }
        /**
             * <p>Enables and disables video stabilization. Use
             * {@link #isVideoStabilizationSupported} to determine if calling this
             * method is valid.</p>
             *
             * <p>Video stabilization reduces the shaking due to the motion of the
             * camera in both the preview stream and in recorded videos, including
             * data received from the preview callback. It does not reduce motion
             * blur in images captured with
             * {@link CameraDevice#takePicture takePicture}.</p>
             *
             * <p>Video stabilization can be enabled and disabled while preview or
             * recording is active, but toggling it may cause a jump in the video
             * stream that may be undesirable in a recorded video.</p>
             *
             * @param toggle Set to true to enable video stabilization, and false to
             * disable video stabilization.
             * @see #isVideoStabilizationSupported()
             * @see #getVideoStabilization()
             */
        public void setVideoStabilization(boolean toggle) {
            set(KEY_VIDEO_STABILIZATION, toggle ? TRUE : FALSE);
        }

        /**
             * Get the current state of video stabilization. See
             * {@link #setVideoStabilization} for details of video stabilization.
             *
             * @return true if video stabilization is enabled
             * @see #isVideoStabilizationSupported()
             * @see #setVideoStabilization(boolean)
             */
        public boolean getVideoStabilization() {
            String str = get(KEY_VIDEO_STABILIZATION);
            return TRUE.equals(str);
        }

        /**
             * Returns true if video stabilization is supported. See
             * {@link #setVideoStabilization} for details of video stabilization.
             *
             * @return true if video stabilization is supported
             * @see #setVideoStabilization(boolean)
             * @see #getVideoStabilization()
             */
        public boolean isVideoStabilizationSupported() {
            String str = get(KEY_VIDEO_STABILIZATION_SUPPORTED);
            return TRUE.equals(str);
        }

		/**
		 * Sets the maximum filesize (in bytes) of the recording video. When the
		 * recording video reach the maximum size, the video will save in a new
		 * file.
		 * 
		 * @param filesize_bytes
		 *            the maximum filesize in bytes (if zero or negative, use
		 *            the default video size)
		 * 
		 */
		public void setVideoRotateSize(int filesize_bytes) {
			set(KEY_VIDEO_ROTATE_SIZE, Integer.toString(filesize_bytes));
		}

		/**
		 * Sets the maximum duration (in ms) of the recording session. When the
		 * recording video reach the maximum duration, the video will save in a
		 * new file.
		 * 
		 * @param duration_ms
		 *            the maximum duration in ms
		 * 
		 */
		public void setVideoRotateDuration(int duration_ms) {
			set(KEY_VIDEO_ROTATE_DURATION, Integer.toString(duration_ms));
		}
		public void setProtectRecordingMode(boolean isMotionMode) {
			if (isMotionMode) {
				set(KEY_MOTION_DETECT_MODE, Integer.toString(1));
			} else {
				set(KEY_MOTION_DETECT_MODE, Integer.toString(0));
			}
		}
		public void setVideoEncodingBitRate(int mEncodingBitRate) {
			set(KEY_VIDEO_PARAM_ENCODING_BITRATE,Integer.toString(mEncodingBitRate));
            set(KEY_USER_SET_VIDEO_ENCODEBITRATE,Integer.toString(1));
		}
		public void setRecordingMuteAudio(boolean isMuteAudio) {
			if (isMuteAudio) {
				set(KEY_VIDEO_MUTE_AIUDO, Integer.toString(1));
			} else {
				set(KEY_VIDEO_MUTE_AIUDO, Integer.toString(0));
			}
		}

		/**
		 * Uses the settings from a CamcorderProfile object for recording.
		 * 
		 * @param profile
		 *            the CamcorderProfile to use
		 * @see android.media.CamcorderProfile
		 */
		public void setVideoProfile(CamcorderProfile profile) {
			// setOutputFormat(profile.fileFormat);
			set(KEY_VIDEO_OUTPUT_FORMAT, Integer.toString(profile.fileFormat));
			// setVideoFrameRate(profile.videoFrameRate);
			set(KEY_VIDEO_FRAME_RATE, Integer.toString(profile.videoFrameRate));

			String v = Integer.toString(profile.videoFrameWidth) + "x"
					+ Integer.toString(profile.videoFrameHeight);
			set(KEY_VIDEO_SIZE, v);
			// setVideoEncodingBitRate(profile.videoBitRate);
			set(KEY_VIDEO_PARAM_ENCODING_BITRATE,
					Integer.toString(profile.videoBitRate));
			// setVideoEncoder(profile.videoCodec);
			set(KEY_VIDEO_ENCODER, Integer.toString(profile.videoCodec));
			if (profile.quality >= CamcorderProfile.QUALITY_TIME_LAPSE_LOW
					&& profile.quality <= CamcorderProfile.QUALITY_TIME_LAPSE_QVGA) {
				// Nothing needs to be done. Call to setCaptureRate() enables
				// time lapse video recording.
			} else {
				set(KEY_AUDIO_PARAM_ENCODING_BITRATE,
						Integer.toString(profile.audioBitRate));
				set(KEY_AUDIO_PARAM_NUMBER_OF_CHANNELS,
						Integer.toString(profile.audioBitRate));
				set(KEY_AUDIO_PARAM_SAMPLING_RATE,
						Integer.toString(profile.audioSampleRate));

				// setAudioEncoder(profile.audioCodec);
				set(KEY_AUDIO_ENCODER, Integer.toString(profile.audioCodec));
			}
		}

		/**
		 * Uses the settings from a CamcorderProfile object for sub video data.
		 * such as video size,video frame rate,etc...
		 * @param profile
		 *            the CamcorderProfile to use
		 * @see android.media.CamcorderProfile
		 */
		public void setSubVideoProfile(CamcorderProfile profile) {
		
		    set(KEY_SUB_VIDEO_FRAME_RATE, Integer.toString(profile.videoFrameRate));
			
            String size = Integer.toString(profile.videoFrameWidth) + "x"
					+ Integer.toString(profile.videoFrameHeight);
			set(KEY_SUB_VIDEO_SIZE, size);
			set(KEY_SUB_VIDEO_PARAM_ENCODING_BITRATE,
					Integer.toString(profile.videoBitRate));
		}

		/**
		 * Sets the path of the video output file to be produced. Only take
		 * effect when called before {@link CameraDevice#startRecord()}. The
		 * default path is "/sdcard/DCIM/Camera".
		 * 
		 * @param path
		 *            The pathname to use.
		 */
		public void setOutputFile(String path) {
			set(KEY_VIDEO_OUTPUT_FILE, path);
		}

		/**
		 * Sets the path of the video output file name to be produced. Only take
		 * effect when called before {@link CameraDevice#startRecord()}. 
		 * 
		 * @param name
		 *            The filename to use.
		 */
		public void setOutputFileName(String name) {
			set(KEY_VIDEO_OUTPUT_FILE_NAME, name);
		}

		/**
		 * Sets the path of the video lock file to be produced. Only take
		 * effect when called before {@link CameraDevice#startRecord()}. The
		 * default path is "/sdcard/DCIM/Camera/protect".
		 * 
		 * @param path
		 *            The pathname to use.
		 */
		public void setLockFile(String path) {
			set(KEY_VIDEO_LOCK_FILE, path);
		}

		/**
		 * Sets the offset of water mark
		 * 
		 * @param offsetX
		 *            The offset of X-axis
		 * @param offsetY
		 *            The offset of Y-axis
		 */
		public void setWaterMarkOffset(int offsetX, int offsetY) {
			String str = Integer.toString(offsetX) + ","
					+ Integer.toString(offsetY);
			set(KEY_WATERMARK_OFFSET, str);
		}

		/**
		 * Sets the offset of water mark timer characters
		 * 
		 * @param offsetX
		 *            The offset of X-axis
		 * @param offsetY
		 *            The offset of Y-axis
		 */
		public void setWaterMarkTimerOffset(int offsetX, int offsetY) {
			String str = Integer.toString(offsetX) + ","
					+ Integer.toString(offsetY);
			set(KEY_WATERMARK_TIMER_OFFSET, str);
		}

		/**
		 * Sets the address of water mark data
		 * 
		 * @param address
		 *            The address of water mark data in the buffer
		 */
		public void setWaterMarkDataAddress(int address) {
			set(KEY_WATERMARK_ADDRESS, Integer.toString(address));
		}

		/**
		 * Sets the size of water mark bitmap
		 * 
		 * @param width
		 *            The width of water mark bitmap
		 * @param height
		 *            The height of water mark bitmap
		 */
		public void setWaterMarkSize(int width, int height) {
			String str = Integer.toString(width) + "x"
					+ Integer.toString(height);
			set(KEY_WATERMARK_SIZE, str);
		}

		/**
		 * Sets the file path to storing the water mark data
		 * 
		 * @param path
		 *            The file path of storing the water mark data
		 */
		public void setWaterMarkDataPath(String path) {
			set(KEY_WATERMARK_DATA_PATH, path);
		}

		/**
		 * Sets the owner who using water mark APP should set the ownerName to
		 * carcorderservice when APP opens the water mark AAnd should set the
		 * ownerName to null when APP closes the water mark
		 * 
		 * @param ownerName
		 *            owner name should be carcorderservice when opening water
		 *            mark owner name should be null when closing water mark
		 */
		public void setWaterMarkOwner(String ownerName) {
			set(KEY_WATERMARK_OWNER_NAME, ownerName);
		}

		/**
		 * Sets the white balance. Changing the setting will release the
		 * auto-white balance lock. It is recommended not to change white
		 * balance and AWB lock at the same time.
		 * 
		 * @param value
		 *            new white balance.
		 * @see #getWhiteBalance()
		 * @see #setAutoWhiteBalanceLock(boolean)
		 */
		public void setWhiteBalance(String value) {
			String oldValue = get(KEY_WHITE_BALANCE);
			if ((value == null && oldValue == null)
					|| (oldValue != null && oldValue.equals(value))) {
				return;
			}
			set(KEY_WHITE_BALANCE, value);
			set(KEY_AUTO_WHITEBALANCE_LOCK, FALSE);
		}

		/**
		 * Gets the current white balance setting.
		 * 
		 * @return current white balance. null if white balance setting is not
		 *         supported.
		 * @see #WHITE_BALANCE_AUTO
		 * @see #WHITE_BALANCE_INCANDESCENT
		 * @see #WHITE_BALANCE_FLUORESCENT
		 * @see #WHITE_BALANCE_WARM_FLUORESCENT
		 * @see #WHITE_BALANCE_DAYLIGHT
		 * @see #WHITE_BALANCE_CLOUDY_DAYLIGHT
		 * @see #WHITE_BALANCE_TWILIGHT
		 * @see #WHITE_BALANCE_SHADE
		 */
		public String getWhiteBalance() {
			return get(KEY_WHITE_BALANCE);
		}

		/**
              * Gets the supported white balance.
              *
              * @return a list of supported white balance. null if white balance
              *         setting is not supported.
              * @see #getWhiteBalance()
              */
        public List<String> getSupportedWhiteBalance() {
            String str = get(KEY_WHITE_BALANCE + SUPPORTED_VALUES_SUFFIX);
            return split(str);
        }

		/**
		 * Gets the current exposure compensation index
		 * 
		 * @return current exposure compensation index, The range is
		 *         {@link #getMinExposureCompensation} to
		 *         {@link #getMaxExposureCompensation}. 0 means exposure is not
		 *         adjusted.
		 */
		public int getExposureCompensation() {
			return getInt(KEY_EXPOSURE_COMPENSATION, 0);
		}

		/**
		 * Sets the auto-white balance lock state.
		 */
		public void setAutoWhiteBalanceLock(boolean toggle) {
			set(KEY_AUTO_WHITEBALANCE_LOCK, toggle ? TRUE : FALSE);
		}

		/**
		 * Sets the exposure compensation index.
		 * 
		 * @param value
		 *            exposure compensation index. The valid value range is from
		 *            {@link #getMinExposureCompensation} (inclusive) to
		 *            {@link #getMaxExposureCompensation} (inclusive). 0 means
		 *            exposure is not adjusted. Application should call
		 *            getMinExposureCompensation and getMaxExposureCompensation
		 *            to know if exposure compensation is supported.
		 */
		public void setExposureCompensation(int value) {
			set(KEY_EXPOSURE_COMPENSATION, value);
		}

		/**
		 * Get the maximum exposure compensation index.
		 * 
		 * @return maximum exposure compensation index (>= 0). If both this
		 *         method and {@link #getMinExposureCompensation} return 0,
		 *         exposure compensation is not supported.
		 */
		public int getMaxExposureCompensation() {
			return getInt(KEY_MAX_EXPOSURE_COMPENSATION, 0);
		}

		/**
		 * Get the minimum exposure compensation index.
		 * 
		 * @return minimum exposure compensation index (>= 0). If both this
		 *         method and {@link #getMaxExposureCompensation} return 0,
		 *         exposure compensation is not supported.
		 */
		public int getMinExposureCompensation() {
			return getInt(KEY_MIN_EXPOSURE_COMPENSATION, 0);
		}

		/**
		 * Get the exposure compensation step.
		 * 
		 * @return exposure compensation step.
		 */
		public float getExposureCompensationStep() {
			return getFloat(KEY_EXPOSURE_COMPENSATION_STEP, 0);
		}

		/**
		 * Sets the opened camera id.
		 * 
		 * @param value
		 *            the opened camera id
		 */
		public void setCameraId(int value) {
			set(KEY_VIDEO_PARAM_CAMERA_ID, value);
		}
		
	    /**
	     * Set Sdcard or TF card  free size limit
	     *
	     * @param limitSize   The unit is mega byte
	     */
		public void setFreeSizeLimit(int limitSize){
           
			set(KEY_FREE_SIZE_LIMIT, limitSize); 	
		}

		/**
		 * Sets the dimensions for adas video frame. If the adas has already
		 * started, applications should stop the adas first before changing
		 * video frame size.
		 * 
		 * @param width
		 *            the width of video frame for adas, in pixels
		 * @param height
		 *            the height of video frame for adas, in pixels
		 */
		public void setAdasSize(int width,int height){
           String v = Integer.toString(width) + "x" + Integer.toString(height);
		   set(KEY_ADAS_SIZE, v);
		}
		 
        /**
              * Sets the adas-end-point 
              *
              * @param value
              *
              */
        public void setAdasEndPoint(int value) {
            set(KEY_ADAS_END_POINT, value);
        }

        /**
              * Sets the adas-scale-factor 
              *
              * @param value
              *
              */
        public void setAdasScaleFactor(float value) {
            set(KEY_ADAS_SCALE_FACTOR, value);
        }

        /**
              * Sets the adas-focal-length 
              *
              * @param value
              *
              */
        public void setAdasFocalLength(float value) {
            set(KEY_ADAS_FOCAL_LENGTH, value);
        }

		/**
              * Set main video data mode how we use 
              * if isEnabled is false,mean just write data to a video file
              * if isEnabled is true, mean just notify data to app,app can get data from VideoCallback.
              * @param isEnabled  
              *
              */
		public void enableMainVideoFrame(boolean isEnabled){
		    if(isEnabled){
                 set(KEY_MAIN_VIDEO_FRAME_ENABLED,1);
		     }else{
                 set(KEY_MAIN_VIDEO_FRAME_ENABLED,0);
		     }
		}

		/**
              * Set sub video data mode how we use 
              * if isEnabled is false,mean just write data to a video file
              * if isEnabled is true, mean just notify data to app,app can get data from VideoCallback.
              * @param isEnabled  
              *
              */
		public void enableSubVideoFrame(boolean isEnabled){
		    if(isEnabled){
                 set(KEY_SUB_VIDEO_FRAME_ENABLED,1);
		     }else{
                 set(KEY_SUB_VIDEO_FRAME_ENABLED,0);
		     }
		}

		/**
             * set recording video file output format,such as mp4 or ts
             * @see #CameraDevice.OutputFormat
             * @param outputFormat, video file format 
             */
		public void setOutputFileFormat(OutputFormat outputFormat){
			set(KEY_VIDEO_OUTPUT_FORMAT,outputFormat.format);
		}

		/**
             * set recording video bitrate range 
             * 
             * @param minBitRate,  the minimum bitrate
             * @param maxBitRate, the maximum bitrate
             */
        public void setVideoBitRateRange(int minBitRate,int maxBitRate){
             set(KEY_VIDEO_PARAM_ENCODING_MINBITRATE,minBitRate);
			 set(KEY_VIDEO_PARAM_ENCODING_MAXBITRATE,maxBitRate);
        }
		
		/**
		  * whether show text watermark for preview, record vieo, picture(normal capture and vss) and video data callback or not
              * The below APIs {@link #setWatermarkArea}, {@link #setWatermarkTextSize}, {@link #setWatermarkTextSize}
              * {@link #setWatermarkTextPosition} should be invoked before
              * And either the {@link #setWatermarkTimestampFormat} or the {@link #setWatermarkText} should be invoked before
              * The {@link #setWatermarkTextColor} is optional
              *
		  * @param enable if true show watermark on preview pictures
		  */
		public void enableWatermarkText(boolean enable){
            if(enable){
				set(KEY_WATERMARK_TEXT_MODE,"on");
            }else{
                set(KEY_WATERMARK_TEXT_MODE,"off");
            }
		}
		
		/**
		  * whether show text watermark on preview  or not
              * The below APIs {@link #setWatermarkArea}, {@link #setWatermarkTextSize}, {@link #setWatermarkTextSize}
              * {@link #setWatermarkTextPosition} should be invoked before
              * And either the {@link #setWatermarkTimestampFormat} or the {@link #setWatermarkText} should be invoked before
              * The {@link #setWatermarkTextColor} is optional
              *
		  * @param enable if true show watermark on preview pictures
		  */
		public void enablePreviewWatermarkText(boolean enable){
            if(enable){
				set(KEY_PREVIEW_WATERMARK_TEXT_MODE,"on");
            }else{
                set(KEY_PREVIEW_WATERMARK_TEXT_MODE,"off");
            }
		}

		/**
		  * whether show text watermark on record video or not
              * The below APIs {@link #setWatermarkArea}, {@link #setWatermarkTextSize}, {@link #setWatermarkTextSize}
              * {@link #setWatermarkTextPosition} should be invoked before
              * And either the {@link #setWatermarkTimestampFormat} or the {@link #setWatermarkText} should be invoked before
              * The {@link #setWatermarkTextColor} is optional
              *
		  * @param enable if true show watermark on record video
		  */
		public void enableVideoWatermarkText(boolean enable){
			if(enable){
				set(KEY_RECORD_WATERMARK_TEXT_MODE,"on");
            }else{
                set(KEY_RECORD_WATERMARK_TEXT_MODE,"off");
            }
		}
		
        /**
              * Enable the text watermark on Capture Picture and Videosnapshot Picture or disable it
              * The below APIs {@link #setWatermarkArea}, {@link #setWatermarkTextSize}, {@link #setWatermarkTextSize}
              * {@link #setWatermarkTextPosition} should be invoked before
              * And either the {@link #setWatermarkTimestampFormat} or the {@link #setWatermarkText} should be invoked before
              * The {@link #setWatermarkTextColor} is optional
              *
              * @param enable
              *                       true means enable the text watermark, otherwise disable it
              */
        public void enablePictureWatermarkText(boolean enable) {
            set(KEY_PICTURE_WATERMARK_TEXT_MODE, enable ? "on" : "off");
        }
		
        /**
              * Enable the text watermark on video callback data or disable it
              * The below APIs {@link #setWatermarkArea}, {@link #setWatermarkTextSize}, {@link #setWatermarkTextSize}
              * {@link #setWatermarkTextPosition} should be invoked before
              * And either the {@link #setWatermarkTimestampFormat} or the {@link #setWatermarkText} should be invoked before
              * The {@link #setWatermarkTextColor} is optional
              *
              * @param enable
              *                       true means enable the text watermark, otherwise disable it
              */
        public void enableVideoCBWatermarkText(boolean enable) {
            set(KEY_VIDEOCB_WATERMARK_TEXT_MODE, enable ? "on" : "off");
        }
		
        /**
		  * set watermart showing area
		  * @param left 
		  * @param top
		  * @param right
		  * @param bottom 
		  */
		public void setWatermarkArea(int left,int top,int right,int bottom){
            String area="("+left+","+top+","+right+","+bottom+",1)";
			set(KEY_WATERMARK_AREA,area);
		}

		/**
		  * set watermark 
		  * The time string in the format to be set by {@link #setWatermarkTimestampFormat} is shown 
		  * when {@link #setWatermarkText} is NOT invoked
		  * 
		  * @param text watermark content
		  */
		public void setWatermarkText(String text){
            set(KEY_WATERMARK_TEXT,text);
		}

		/**
		  * set watermark text size
		  * @param size 
		  */
		public void setWatermarkTextSize(float size){
            set(KEY_WATERMARK_TEXT_SIZE,size);
		}

		/**
		  * set watermark text color
		  * The default text color is RED
		  *
		  * @param color  
		  */
		public void setWatermarkTextColor(int color){
			set(KEY_WATERMARK_TEXT_COLOR,color);
		}

		/**
		  * set watermark text color
		  * The default text color is RED
		  * @param a
		  * @param r
		  * @param g
		  * @param b
		  */
        public void setWatermarkTextColor(int a, int r, int g, int b){
            int color = ((a << 24) & 0xff000000) | ((r << 16) & 0xff0000) | ((g << 8) & 0xff00) | (b & 0xff);
			set(KEY_WATERMARK_TEXT_COLOR,color);
        };

		/**
		  * set watermark position offset on watermark area
		  *
		  * @param x  x offset on watermark area
		  * @param y  y offset on watermark area
		  */
		public void setWatermarkTextPosition(float x,float y){
            set(KEY_WATERMARK_TEXT_X,x);
			set(KEY_WATERMARK_TEXT_Y,y);
		}

		/**
		  * set watermark time format
		  * The default time format is "%Y%m%d-%H:%M:%S"
		  *
		  * @param format 
		  */
		public void setWatermarkTimestampFormat(String format){
            set(KEY_WATERMARK_TIMESTAMP_FORMAT,format);
		}

        /**
		  * whether show image watermark on preview, recording video, picture(normal capture and vss) and video data callback  or not
               * And both of the {@link #setWatermarkImgPath} and {@link #setWatermarkImgArea} should be invoked before
               *
		  * @param enable if true show watermark on preview pictures
		  */
		public void enableWatermarkImage(boolean enable){
            if(enable){
				set(KEY_WATERMARK_IMG_MODE,"on");
            }else{
                set(KEY_WATERMARK_IMG_MODE,"off");
            }
		}

        /**
		  * whether show image watermark on preview  or not
               * And both of the {@link #setWatermarkImgPath} and {@link #setWatermarkImgArea} should be invoked before
               *
		  * @param enable if true show watermark on preview pictures
		  */
		public void enablePreviewWatermarkImage(boolean enable){
            if(enable){
				set(KEY_PREVIEW_WATERMARK_IMG_MODE,"on");
            }else{
                set(KEY_PREVIEW_WATERMARK_IMG_MODE,"off");
            }
		}

		/**
		  * whether show image watermark on record video or not
               * And both of the {@link #setWatermarkImgPath} and {@link #setWatermarkImgArea} should be invoked before
               *
		  * @param enable if true show watermark on record video
		  */
		public void enableVideoWatermarkImage(boolean enable){
			if(enable){
				set(KEY_RECORD_WATERMARK_IMG_MODE,"on");
            }else{
                set(KEY_RECORD_WATERMARK_IMG_MODE,"off");
            }
		}

        /**
              * Enable the image watermark on Capture Picture and Videosnapshot Picture or disable it
              * And both of the {@link #setWatermarkImgPath} and {@link #setWatermarkImgArea} should be invoked before
              * @param enable
              *                       true means enable the image watermark, otherwise disable it
              */
        public void enablePictureWatermarkImage(boolean enable) {
            set(KEY_PICTURE_WATERMARK_IMG_MODE, enable ? "on" : "off");
        }

        /**
              * Enable the image watermark on video callback data or disable it
              * And both of the {@link #setWatermarkImgPath} and {@link #setWatermarkImgArea} should be invoked before
              * @param enable
              *                       true means enable the image watermark, otherwise disable it
              */
        public void enableVideoCBWatermarkImage(boolean enable) {
            set(KEY_VIDEOCB_WATERMARK_IMG_MODE, enable ? "on" : "off");
        }

        /**
              * Set the file path of watermark image 
              * It's effective only when {@link #enablePictureWatermarkImage} is invoked with true 
              * 
              * @param path
              *                   The watermark image's file path
              */
        public void setWatermarkImgPath(String path) {
            set(KEY_WATERMARK_IMG_PATH, path);
        }
        

        /**
              * Set the area of screen in which the watermark image is composed 
              * It's effective only when {@link #enablePictureWatermarkImage} is invoked with true
              *
              * @param left The position of the left edge of this area
              * @param top The position of the top edge of this area
              * @param right The position of the right edge of this area
              * @param bottom The position of the bottom edge of this area
              */
        public void setWatermarkImgArea(int left,int top,int right,int bottom) {
            String area="("+left+","+top+","+right+","+bottom+",1)";
            set(KEY_WATERMARK_IMG_AREA, area);
        }

		/*
             * set keypoint span time limit, if time interval of two video file
             * greater than the seconds what user set,keypoint video will not
             * use the video file which record before
             *
             * @seconds the unit of time interval
             */
		public void setKeypointSpanLimit(int seconds){
		   set(KEY_KEYPOINT_SPAN_LIMIT,seconds);
		}

        /**
             * Sets recording mode hint. This tells the camera that the intent of
             * the application is to record videos {@link
             * android.media.MediaRecorder#start()}, not to take still pictures
             * {@link #takePicture(CameraDevice.ShutterCallback, CameraDevice.PictureCallback,
             * CameraDevice.PictureCallback, CameraDevice.PictureCallback)}. Using this hint can
             * allow MediaRecorder.start() to start faster or with fewer glitches on
             * output. This should be called before starting preview for the best
             * result, but can be changed while the preview is active. The default
             * value is false.
             *
             * The app can still call takePicture() when the hint is true or call
             * MediaRecorder.start() when the hint is false. But the performance may
             * be worse.
             *
             * @param hint true if the apps intend to record videos using
             *             {@link android.media.MediaRecorder}.
             */
        public void setRecordingHint(boolean hint) {
            set(KEY_RECORDING_HINT, hint ? TRUE : FALSE);
        }
		
        /**
              * To enable or disable HDR
              * 
              * @param enabled
              *                        true, means the HDR is to be enabed, 
              *                        and hdr mode{@link #setHdrMode} should be set {@link #VIDEO_HDR_MODE_MVHDR}
              *
              *                        false, mean HDR is disabled
              */
        public void setHdr(boolean enabled) {
            set(KEY_VIDEO_HDR, enabled? "on" : "off");
        }

        /**
              * Get the setting of HDR
              * 
              * @return
              *            if returned string is on,  means HDR is enabled
              *            if returned string is off means HDR is disalbed
              *            if returned string is empty, means HDR isn't supported
              */
        public String getHdr() {
            String value = get(KEY_VIDEO_HDR);
            return value;
        }

        /**
              * Get the supported setting of HDR
              *
              * @return
              *            if the  setting list includes "on", then the vhdr is supported
              *            if the setting list only has "off", then the vhdr isn't supported
              *            if null is returned, then the vhdr isn't supported
              */
        public List<String> getSuportedHdr() {
            String str = get(KEY_VIDEO_HDR + SUPPORTED_VALUES_SUFFIX);
            return split(str);
        }

        /**
              * Set the HDR mode
              * 
              * @param mode
              *                    VIDEO_HDR_MODE_IVHDR: should never be set
              *                    VIDEO_HDR_MODE_MVHDR: is set when HDR is enabled
              */
        public void setHdrMode(String mode) {
            if (!mode.isEmpty() && (mode.equals(VIDEO_HDR_MODE_IVHDR) || mode.equals(VIDEO_HDR_MODE_MVHDR))) {
                set(KEY_VIDEO_HDR_MODE, mode);
            }
        }

        /**
         * Sets the clockwise rotation angle in degrees relative to the
         * orientation of the camera. This affects the pictures returned from
         * JPEG {@link PictureCallback}. The camera driver may set orientation
         * in the EXIF header without rotating the picture. Or the driver may
         * rotate the picture and the EXIF thumbnail. If the Jpeg picture is
         * rotated, the orientation in the EXIF header will be missing or 1 (row
         * #0 is top and column #0 is left side).
         *
         * <p>
         * If applications want to rotate the picture to match the orientation
         * of what users see, apps should use
         * {@link android.view.OrientationEventListener} and
         * {@link android.hardware.Camera.CameraInfo}. The value from
         * OrientationEventListener is relative to the natural orientation of
         * the device. CameraInfo.orientation is the angle between camera
         * orientation and natural device orientation. The sum of the two is the
         * rotation angle for back-facing camera. The difference of the two is
         * the rotation angle for front-facing camera. Note that the JPEG
         * pictures of front-facing cameras are not mirrored as in preview
         * display.
         *
         * <p>
         * For example, suppose the natural orientation of the device is
         * portrait. The device is rotated 270 degrees clockwise, so the device
         * orientation is 270. Suppose a back-facing camera sensor is mounted in
         * landscape and the top side of the camera sensor is aligned with the
         * right edge of the display in natural orientation. So the camera
         * orientation is 90. The rotation should be set to 0 (270 + 90).
         *
         * <p>The reference code is as follows.
         *
         * <pre>
         * public void onOrientationChanged(int orientation) {
         *     if (orientation == ORIENTATION_UNKNOWN) return;
         *     android.hardware.Camera.CameraInfo info =
         *            new android.hardware.Camera.CameraInfo();
         *     android.hardware.Camera.getCameraInfo(cameraId, info);
         *     orientation = (orientation + 45) / 90 * 90;
         *     int rotation = 0;
         *     if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
         *         rotation = (info.orientation - orientation + 360) % 360;
         *     } else {  // back-facing camera
         *         rotation = (info.orientation + orientation) % 360;
         *     }
         *     mParameters.setRotation(rotation);
         * }
         * </pre>
         *
         * @param rotation The rotation angle in degrees relative to the
         *                 orientation of the camera. Rotation can only be 0,
         *                 90, 180 or 270.
         * @throws IllegalArgumentException if rotation value is invalid.
         * @see android.view.OrientationEventListener
         * @see #getCameraInfo(int, CameraInfo)
         */
        public void setRotation(int rotation) {
            if (rotation == 0 || rotation == 90 || rotation == 180
                    || rotation == 270) {
                set(KEY_ROTATION, Integer.toString(rotation));
            } else {
                throw new IllegalArgumentException(
                        "Invalid rotation=" + rotation);
            }
        }

        
        /**
             * Sets the clockwise rotation angle in degrees relative to the orientation of the camera.
             * This affects the recording video and video callback data and preview callback data.
             * The camera hal rotates the recording video and video callback data and 
             * preview callback data according to this parameter.
             * Now the rotation angle only has two values: 0 and 180, other values are considered as 0
             */
        public void setVideoRotation(int rotation) {
            Log.d(TAG, "setVideoBufRotation: "  + rotation);
            set(KEY_VIDEO_ROTATION, rotation);
        }

		/*
		  * set main video data mode,it tell recordMgr how to handle video data
		  * @VideoFrameMode.DISABLE  just write to video file
		  * @VideoFrameMode.SOURCE  callback to app use h264 format 
		  * @VideoFrameMode.PACKET   callback to app use ts format 
		  * @VideoFrameMode.DUAL      write to video file and callback to app use ts format
		  * 
		 */
		public void setMainVideoFrameMode(VideoFrameMode frameMode){
             set(KEY_MAIN_VIDEO_FRAME_ENABLED,frameMode.mode);
		}
		
		/*
		  * set sub video data mode,it tell recordMgr how to handle video data
		  * @VideoFrameMode.DISABLE  just write to video file
		  * @VideoFrameMode.SOURCE  callback to app use h264 format 
		  * @VideoFrameMode.PACKET   callback to app use ts format 
		  * @VideoFrameMode.DUAL      write to video file and callback to app use ts format
		  * 
		 */
		public void setSubVideoFrameMode(VideoFrameMode frameMode){
             set(KEY_SUB_VIDEO_FRAME_ENABLED,frameMode.mode);
		}
		
		/**
		  * Sets GPS latitude coordinate. This will be stored in JPEG EXIF header. 
		  *
		  * @param latitude GPS latitude coordinate.
		 */
		 public void setGpsLatitude(double latitude) {
			  set(KEY_GPS_LATITUDE, Double.toString(latitude));
		 }
		
		/**
		  * Sets GPS longitude coordinate. This will be stored in JPEG EXIF header.
		  *
		  * @param longitude GPS longitude coordinate.
		 */
		 public void setGpsLongitude(double longitude) {
			   set(KEY_GPS_LONGITUDE, Double.toString(longitude));
		 }
		
		 /**
		   * Sets GPS altitude. This will be stored in JPEG EXIF header.
		   *
		   * @param altitude GPS altitude in meters.
		   */
		  public void setGpsAltitude(double altitude) {
			   set(KEY_GPS_ALTITUDE, Double.toString(altitude));
		  }
		
		  /**
		    * Sets GPS timestamp. This will be stored in JPEG EXIF header.
		    *
		    * @param timestamp GPS timestamp (UTC in seconds since January 1, 1970).
		    *				  
		    */
		  public void setGpsTimestamp(long timestamp) {
			  set(KEY_GPS_TIMESTAMP, Long.toString(timestamp));
		  }
		
		  /**
		    * Sets GPS processing method. It will store up to 32 characters
		    * in JPEG EXIF header.
		    *
		    * @param processing_method The processing method to get this location.
		    */
		   public void setGpsProcessingMethod(String processing_method) {
			   set(KEY_GPS_PROCESSING_METHOD, processing_method);
		   }
		
		  /**
		    * Removes GPS latitude, longitude, altitude, and timestamp from the parameters. 
		    */
		   public void removeGpsData() {
			  remove(KEY_GPS_LATITUDE);
			  remove(KEY_GPS_LONGITUDE);
			  remove(KEY_GPS_ALTITUDE);
			  remove(KEY_GPS_TIMESTAMP);
			  remove(KEY_GPS_PROCESSING_METHOD);
		   }
		  
	}

	/**
	 * Returns the current settings for this Camera device. If modifications are
	 * made to the returned Parameters, they must be passed to
	 * {@link #setParameters(CameraDevice.Parameters)} to take effect.
	 * 
	 * @see #setParameters(CameraDevice.Parameters)
	 */
	public Parameters getParameters() {
		Parameters params = new Parameters();
		String str = null;
		try {
			str = mCameraDevice.getParameters();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		params.unflatten(str);
		return params;
	}

	/**
	 * Changes the settings for this Camera service.
	 * 
	 * @param params
	 *            the Parameters to use for this Camera service
	 * @see #getParameters()
	 */
	public void setParameters(Parameters params) {
		try {
			mCameraDevice.setParameters(params.flatten());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Sets the {@link Surface} to be used for live preview.
	 * 
	 * <p>
	 * The surface can be surface view or surface texture. During the preview
	 * process, if the preview surface is destroyed, this method can be called
	 * to set a new surface, but the dimensions cannot change until
	 * {@link #stopPreview()} be called.
	 * 
	 * @param surface
	 *            used to display preview pictures
	 * 
	 */
	public boolean setPreviewSurface(Surface surface) {
		try {
			mCameraDevice.setPreviewTarget(0, 0, surface);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Starts capturing and drawing preview frames to the screen. Preview will
	 * not actually start until a surface is supplied with
	 * {@link #setPreviewSurface(Surface)}
	 * 
	 */
	public void startPreview() {
		try {
			mCameraDevice.startPreview();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Stops capturing and drawing preview frames to the surface, and resets the
	 * camera for a future call to {@link #startPreview()}.
	 */
	public void stopPreview() {
		try {
			mCameraDevice.stopPreview();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Starts the video recording, the video files will rotately save in
	 * filesystem. Method
	 * {@link CameraDevice.Parameters#setVideoProfile(CamcorderProfile)} must be
	 * called before this method.
	 * @return 0 success,otherwise fail
	 */
	public int startRecord() {
		try {
			return mCameraDevice.startRecord();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return START_RECORD_EXCEPTION;
	}

	/**
	 * Stops the video recording
	 */
	public void stopRecord() {
		try {
			mCameraDevice.stopRecord();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Starts ADAS (Advanced driver assistance systems)
	 */
	public void startADAS() {
		try {
			mCameraDevice.startADAS();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Stops ADAS
	 */
	public void stopADAS() {
		try {
			mCameraDevice.stopADAS();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Start motion detection
	 */
	public void startMotionDetection() {
		try {
			mCameraDevice.startMotionDetection();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Stop motion detection
	 */
	public void stopMotionDetection() {
		try {
			mCameraDevice.stopMotionDetection();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * start use sub video data,it will be write to a video file or notify to app
	 *
	 * @return 0 if start success,otherwise fail
	 */
     public int startSubVideoFrame(){
        try {
			return mCameraDevice.startSubVideoFrame();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		 return -1;
     }

	 /**
	   * stop use sub video data
	   *
	   * @return 0 if start success,otherwise fail
	   */
	 public int stopSubVideoFrame(){
        try {
			return mCameraDevice.stopSubVideoFrame();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		 return -1;
     }

	 
	 private int readAshmemBytes(ParcelFileDescriptor fd, int size, byte[] data) {
			 if (data.length < size) {
				 Log.w(TAG, "buffer size(" + size+ ") > data.length(" + data.length + ")");
				 return -1;
			 }
	 
			 try {
				 CarcorderMemoryFile client = new CarcorderMemoryFile(fd.getFileDescriptor(), size);
				 int length = 0;
				 if (client != null) {
					 length = client.readBytes(data, 0, 0, size);
					 
					 client.close();
					 return length;
				 } else {
					 Log.w(TAG, "ashmem client is null");
				 }
			 } catch (IOException e) {
				 e.printStackTrace();
			 }
			 return 0;
	}

	private void closeFd(ParcelFileDescriptor fd){
         try {
              if(fd!=null){
                 fd.close(); 
              }
		  } catch (IOException e) {
			 e.printStackTrace();
	      }
        
	}
	 
	private String mAshmemName = "AshmemCarcorder";
	private HashMap<Integer, CarcorderMemoryFile> mAshmemList = new HashMap<Integer, CarcorderMemoryFile>();

	public static final int ASHMEM_TYPE_WATERMARK = 0;

	/**
	 * Create an Ashmem file to share the memory with Carcorder Service
	 * 
	 * @param type
	 *            Corresponding to the created Ashmem
	 *            <ul>
	 *            <li>{@link ASHMEM_TYPE_WATERMARK}
	 *            </ul>
	 * @param size
	 *            The size of created the Ashmem
	 * @return true if it's successful to create Ashmem, false otherwise
	 */

	public boolean openAshmem(int type, int size) {
		try {
			CarcorderMemoryFile ashmem = mAshmemList.get(type);
			if (ashmem == null) {
				if (size > 0) {
					ashmem = new CarcorderMemoryFile(mAshmemName, size);
					mAshmemList.put(type, ashmem);
					mCameraDevice.shareAshmem(type, ashmem.getParcelFileDescriptor(), size);

					return true;
				} else {
					Log.w(TAG, "Ashmem size should more than 0");
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Write the bytes to the created ashmem memory corresponding to the type
	 * 
	 * @param type
	 *            Corresponding to the wrotten bytes, if the type is different,
	 *            the structure of bytes maybe different
	 * @param bytes
	 *            The data wrotten to ashmem file
	 * 
	 * @return true if success, fals otherwise
	 */
	public boolean writeAshmem(int type, byte[] bytes) {
		try {
			CarcorderMemoryFile ashmem = mAshmemList.get(type);
			if (ashmem == null) {
				Log.w(TAG,
						"Please invoke openAshmem() function to open ashmem for type("
								+ type + ") before");
				return false;
			} else if (!ashmem.getFileDescriptor().valid()) {
				int ashmemSize = ashmem.size();
				ashmem.close();
				// mCameraDevice.clearAshmem(type);

				// Try to new MemoryFile again
				ashmem = new CarcorderMemoryFile(mAshmemName, ashmemSize);
				mAshmemList.remove(type);
				mAshmemList.put(type, ashmem);
				mCameraDevice.shareAshmem(type,
						ashmem.getParcelFileDescriptor(),
						ashmemSize);
			}

			if (bytes != null && bytes.length <= ashmem.size()) {
				Log.d(TAG, "Writing bitmap to SharedMemory ");
				ashmem.writeBytes(bytes, 0, 0, bytes.length);
			} else {
				Log.d(TAG, "Message is too long. Message length("
						+ bytes.length + ") > " + ashmem.size());
				return false;
			}

			Log.d(TAG, "mSharedMemory's " + ashmem.getFileDescriptor());
			mCameraDevice.writeAshmem(type, bytes.length);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Write bitmap to the created ashmem memory corresponding to the type
	 * 
	 * @param type
	 *            Corresponding to the wrotten bytes, if the type is different,
	 *            the structure of bytes maybe different
	 * @param bytes
	 *            The data wrotten to ashmem file
	 * 
	 * @return true if success, fals otherwise
	 */
	public boolean writeAshmem(int type, Bitmap bitmap) {
		try {
			Log.d(TAG, "Enter into writeAshmem(" + type + ", " + bitmap + ")");
			CarcorderMemoryFile ashmem = mAshmemList.get(type);
			if (ashmem == null) {
				Log.w(TAG,
						"Please invoke openAshmem() function to open ashmem for type("
								+ type + ") before");
				return false;
			} else if (!ashmem.getFileDescriptor().valid()) {
				Log.d(TAG, "fd is invalid(), re-create it");
				int ashmemSize = ashmem.size();
				ashmem.close();
				// mCameraDevice.clearAshmem(type);

				// Try to new MemoryFile again
				ashmem = new CarcorderMemoryFile(mAshmemName, ashmemSize);
				mAshmemList.remove(type);
				mAshmemList.put(type, ashmem);
				mCameraDevice.shareAshmem(type, ashmem.getParcelFileDescriptor(), ashmemSize);
			}

			if (bitmap != null
					&& bitmap.getWidth() * bitmap.getHeight() <= ashmem.size()) {
				Log.d(TAG, "Writing bitmap to SharedMemory ");
				ashmem.writeBitmap(bitmap);
			} else if (bitmap != null){
				Log.d(TAG,
						"Message is too long. Message length("
								+ bitmap.getWidth() * bitmap.getHeight()
								+ ") > " + ashmem.size());
				return false;
			} else {
 				Log.d(TAG, "bitmap is null");
				return false;
			}

			Log.d(TAG, "mSharedMemory's " + ashmem.getFileDescriptor());
			mCameraDevice.writeAshmem(type,
					bitmap.getRowBytes() * bitmap.getHeight());
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Log.w(TAG, "Failed to new CarcorderMemoryFile");
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Release the created ashmem
	 * 
	 * @param type
	 *            The Ashmem file corresponding to this type is to close
	 */
	public void closeAshmem(int type) {
		try {
			CarcorderMemoryFile ashmem = mAshmemList.get(type);
			if (ashmem != null) {
				mAshmemList.remove(type);
				ashmem.close();

				mCameraDevice.clearAshmem(type);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	public void clearAshmemData(int type,int cleardata) {
		try {
			CarcorderMemoryFile ashmem = mAshmemList.get(type);
			if (ashmem != null) {
				mCameraDevice.clearWaterMarkData(cleardata);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	private static final int EVENT_LOCK_VIDEO = 0;
	private static final int EVENT_SET_VIDEO_ROTATE_DURATION = 1;
	private static final int EVENT_SET_RECORDING_MUTE_AUDIO = 2;
	private static final int EVENT_SET_RECORDING_SDCARD_PATH = 3;
	private static final int EVENT_SET_MOTIO_DETECT = 4;
	private static final int EVENT_ENABLE_SHUTTER_SOUND = 5;
	private static final int EVENT_SET_BITRATE_DYN = 6;

	/**
	 * Lock the current recording video in order to keep the video from being
	 * overridden.
	 */
	public void lockRecordingVideo(int duration) {
		notifyEvent(EVENT_LOCK_VIDEO, 1, duration, null);
	}
   /*
	* Lock the current recording video and notify data to app,need register
	* @link KeypointCallback to receive keypoint data
	* @param duration the time of protected video,unit is second 
	* @param protectedType  a flag of keypoint video,like "LowRes"
	*/
    public void lockRecordingVideo(int duration,String protectedType) {
		notifyEvent(EVENT_LOCK_VIDEO, 1, duration, protectedType);
	}
	
    public void unlockRecordingVideo(String filename) {
		notifyEvent(EVENT_LOCK_VIDEO, 0, 0, filename);
	}

	public void setVideoRotateDuration(int duration_ms) {
		notifyEvent(EVENT_SET_VIDEO_ROTATE_DURATION, duration_ms, 0, null);
	}

	public void setVideoBitraeDyn(int bitrate, int bAdjust) {
		notifyEvent(EVENT_SET_BITRATE_DYN, bitrate, bAdjust, null);
	}
	public void setRecordingMuteAudio(boolean isMuteAudio) {
		if (isMuteAudio) {
			notifyEvent(EVENT_SET_RECORDING_MUTE_AUDIO, 1, 0, null);
		} else {
			notifyEvent(EVENT_SET_RECORDING_MUTE_AUDIO, 0, 0, null);
		}
	}

	public void setProtectRecording(boolean ismotiondetect, boolean isrecordingstatus,int duration_ms) {
		if (ismotiondetect) {
			notifyEvent(EVENT_SET_MOTIO_DETECT, 1, isrecordingstatus? 1:0,
					Integer.toString(duration_ms));
		} else {
			notifyEvent(EVENT_SET_MOTIO_DETECT, 0, isrecordingstatus? 1:0,
					Integer.toString(duration_ms));
		}
	}

	public void setRecordingSdcardPath(String path) {
		if (path == null) {
			Log.w(TAG, "Invalid argument: path=" + path
					+ " in setRecordingSdcardPath");
			return;
		}
		notifyEvent(EVENT_SET_RECORDING_SDCARD_PATH, 0, 0, path);
	}

	private void notifyEvent(int eventId, int arg1, int arg2, String params) {
		try {
			mCameraDevice.notifyEvent(eventId, arg1, arg2, params);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Enable shutter sound
	 * 
	 * @param shutter
	 *            if false, the shutter sound will disabled, the default shutter
	 *            sound is enabled.
	 */
	public void enableShutterSound(boolean shutter) {
		try {
			mCameraDevice.notifyEvent(EVENT_ENABLE_SHUTTER_SOUND, shutter ? 1
					: 0, 0, "shuttersound");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Callback interface used to signal the moment of actual image capture.
	 * 
	 */
	public interface ShutterCallback {
		/**
		 * Called as near as possible to the moment when a photo is captured
		 * from the sensor. This is a good opportunity to play a shutter sound
		 * or give other feedback of camera operation. This may be some time
		 * after the photo was triggered, but some time before the actual data
		 * is available.
		 */
		void onShutter();
	}

	/**
	 * Callback interface used to supply JPEG pathname.
	 * 
	 */
	public interface PictureCallback {
		/**
		 * Called when image data is available after a picture is taken.
		 * 
		 * @param path
		 *            the taken JPEG pathname
		 */
		void onPictureTaken(String path);
	};

	/**
	 * Triggers an asynchronous image capture.
	 * 
	 * @param path
	 *            the following JPEG pathname, or null that the pathname is
	 *            auto-generate with timestamp
	 * @param shutter
	 *            the callback for image capture moment, or null
	 * @param jpeg
	 *            the callback for JPEG pathname, or null
	 * 
	 */
	public void takePicture(String path, ShutterCallback shutter,
			PictureCallback jpeg) {
		synchronized (mLock) {
			mShutterCallback = shutter;
			mPictureCallback = jpeg;
		}
		try {
			mCameraDevice.takePicture(path);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

    public void queryAeLv() {
        try {
            mCameraDevice.queryAeLv();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

	@Override
	@FindBugsSuppressWarnings("FI_EMPTY")
	protected void finalize() throws Throwable {
		super.finalize();
		mCameraDevice.removeListener(LISTENER);
	}
}
