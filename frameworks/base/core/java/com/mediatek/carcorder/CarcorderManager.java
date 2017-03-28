package com.mediatek.carcorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.IOException;

import android.hardware.camera2.utils.BinderHolder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

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
public final class CarcorderManager implements IBinder.DeathRecipient {

	private static final String CARCORDER_SERVICE_BINDER_NAME = "mtk.carcorder";
	private static final String TAG = "CarcorderManager";

	private Object mLock = new Object();
	private ICarcorderService mCarcorderService = null;
	private ArrayList<CameraAvailableCallback> mAvailableCallbacks = new ArrayList<CameraAvailableCallback>();
	private HashSet<CollisionCallback> mCollisionCallbacks = new HashSet<CollisionCallback>();
    private ArrayList<CarReverseCallback> mCarReverseCallbacks = new ArrayList<CarReverseCallback>();
    private ArrayList<CarcorderDeathCallback> mServiceDeathCallbacks = new ArrayList<CarcorderDeathCallback>();
    private ArrayList<CarEngineChangedCallback> mCarEngineChangedCallbacks = new ArrayList<CarEngineChangedCallback>();

	/**
	 * Interface definition for a callback to be invoked when a camera was added
	 * or removed.
	 */
	public interface CameraAvailableCallback {

		public static final int STATUS_CAMERA_ADDED = 1;
		public static final int STATUS_CAMERA_REMOVED = 0;

		/**
		 * Called when a CVBS or USB camera was added or removed.
		 * 
		 * @param cameraid
		 *            the camera id
		 * @param status
		 *            camera status
		 *            <ul>
		 *            <li>{@link #STATUS_CAMERA_ADDED}
		 *            <li>{@link #STATUS_CAMERA_REMOVED}
		 *            </ul>
		 * @param extra
		 *            an extra code, specific to the error type
		 */
		void onAvailable(int cameraid, int status);
	}

	/**
	 * Interface definition for a callback to be invoked when car collision
	 * happened.
	 */
	public interface CollisionCallback {

		public static final int COLLISION_UNRELIABLE = 0;
		public static final int COLLISION_LOW = 1;
		public static final int COLLISION_MEDIUM = 2;
		public static final int COLLISION_HIGH = 3;

		/**
		 * Called when car collision happened.
		 * 
		 * @param COLLISION
		 *            <ul>
		 *            <li>{@link #COLLISION_LOW}
		 *            <li>{@link #COLLISION_MEDIUM}
		 *            <li>{@link #COLLISION_HIGH}
		 *            </ul>
		 * @status status reserved
		 */
		void onCollison(int COLLISION, int status);

	}

    public interface CarReverseCallback {

        public static final int CAR_STATUS_REVERSE = 0;
        public static final int CAR_STATUS_NORMAL = 1;

        /**
              * Called when car starts to reverse or starts to drive
              *
              * @param status
		 *            <ul>
		 *            <li>{@link #CAR_STATUS_REVERSE}
		 *            <li>{@link #CAR_STATUS_NORMAL}
		 *            </ul>
              */
        void onReverse(int status);
    }

    public interface CarEngineChangedCallback {
        
        public static final int CAR_ENGINE_FLAMEOUT = 0;
        public static final int CAR_ENGINE_WORKING = 1;

        /**
              * Called when car's engine status is changed to flameout from working 
              * or is changed to working from flameout
              *
              * This callback is available only when smart platform is special device and
              * {@link #setDefaultAccOffBehavior(boolean)} is invoked to disable the default ACC off behavior
              *
              * @param status
		 *            <ul>
		 *            <li>{@link #CAR_ENGINE_FLAMEOUT}
		 *            <li>{@link #CAR_ENGINE_WORKING}
		 *            </ul>
              */
        void onEngineChanged(int status);
    }

    public interface CarcorderDeathCallback {

        /**
              * Called when carcorder service is dead
              *
              * @param arg1 reserved
              * @param arg2 reserved
              */
        void onDeath(int arg1, String arg2);
    };

	// Singleton instance
	private static final CarcorderManager gCarcorderManager = new CarcorderManager();

	// Singleton, don't allow construction
	private CarcorderManager() {
	}

	/**
	 * @return the CarcorderManager singleton instance
	 */
	public static CarcorderManager get() {
		return gCarcorderManager;
	}

	private ICarcorderService getCarcorderService() {
		if (mCarcorderService == null) {
			IBinder carcorderServiceBinder = ServiceManager
					.getService(CARCORDER_SERVICE_BINDER_NAME);
			if (carcorderServiceBinder == null) {
				Log.e(TAG, CARCORDER_SERVICE_BINDER_NAME + " is null");
				return null;
			}
			try {
				carcorderServiceBinder.linkToDeath(this, 0);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mCarcorderService = ICarcorderService.Stub
					.asInterface(carcorderServiceBinder);
			try {
				mCarcorderService.addListener(mEventListener);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return mCarcorderService;
	}

	/**
	 * Returns the number of available cameras. Since the CVBS or USB camera
	 * device can be removed, the return value will change when camera be added
	 * or removed.
	 * 
	 * @return the number of available cameras.
	 */
	public int getNumberOfCameras() {
		int num = 0;
		try {
			num = getCarcorderService().getNumberOfCameras();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return num;
	}

    public int getCameraInfo(int cameraId, CameraInfo cameraInfo) {
        int status = 0;
        try {
            status = getCarcorderService().getCameraInfo(cameraId, cameraInfo);
        }catch (RemoteException e) {
            e.printStackTrace();
        }
        return status;
    }

	private ICarcorderEventListener mEventListener = new ICarcorderEventListener.Stub() {

		public static final int EVENT_WEBCAM_HOTPLUG = 0;
        // Should keep the same definition with the one in carReverHandler.h
		public static final int EVENT_CAR_REVERSE = 1;
		public static final int EVENT_COLLISION = 2;
        public static final int EVENT_CAR_ENGINE_CHANGED = 3;

		@Override
		public void onEventChanged(int event, int arg1, int arg2)
				throws RemoteException {
			synchronized (mLock) {
				switch (event) {
				case EVENT_WEBCAM_HOTPLUG:
					for (CameraAvailableCallback callback : mAvailableCallbacks) {
						if (callback != null) {
							callback.onAvailable(arg1, arg2);
						}
					}
					break;
				case EVENT_CAR_REVERSE:
                    for (CarReverseCallback callback : mCarReverseCallbacks) {
                        if (callback != null) {
                            callback.onReverse(arg1);
                        }
                    }
					break;
				case EVENT_COLLISION:
					for (CollisionCallback callback : mCollisionCallbacks) {
						if (callback != null) {
							callback.onCollison(arg1, arg2);
						}
					}
					break;

                case EVENT_CAR_ENGINE_CHANGED:
                    for (CarEngineChangedCallback callback : mCarEngineChangedCallbacks) {
                        if (callback != null) {
                            callback.onEngineChanged(arg1);
                        }
                    }

				default:
					break;
				}
			}
		}
	};

	/**
       * Register a callback to be invoked when car starts to drive or start to reverse
       *
       * @param callback
       *            the callback that will be run
       */
    public void addCarReverseCallback(final CarReverseCallback callback) {
        synchronized(mLock) {
            if (callback != null) {
                mCarReverseCallbacks.add(callback);
            }
        }
    }


    /**
       * Unregister a callback to be invoked when car starts to drive or start to reverse
       *
       * @param callback
       *            the callback that will no longer be invoked
       */

    public void removeCarReverseCallback(final CarReverseCallback callback) {
        synchronized(mLock) {
            if (callback != null) {
                mCarReverseCallbacks.remove(callback);
            }
        }
    }

	/**
       * Register a callback to receive the status of car engine on special device
       *
       * @param callback
       *            the callback that will be run
       */
    public void addCarEngineChangedCallback(final CarEngineChangedCallback callback) {
        synchronized(mLock) {
            if (callback != null) {
                mCarEngineChangedCallbacks.add(callback);
            }
        }
    }


    /**
       * Unregister a callback of receiving the status of car engine on special device
       *
       * @param callback
       *            the callback that will no longer be invoked
       */

    public void removeCarEngineChangedCallback(final CarEngineChangedCallback callback) {
        synchronized(mLock) {
            if (callback != null) {
                mCarEngineChangedCallbacks.remove(callback);
            }
        }
    }

	/**
	 * Register a callback to be invoked when a camera was added or removed.
	 * 
	 * @param callback
	 *            the callback that will be run
	 */
	public void addCameraAvailableCallback(
			final CameraAvailableCallback callback) {
			Log.i(TAG, "cyt, addCameraAvailableCallback: " + callback);
		synchronized (mLock) {
			if (callback != null) {
				mAvailableCallbacks.add(callback);
			}
		}
	}

	/**
	 * Unregister the installed callback
	 * 
	 * @param callback
	 *            the callback that will no longer be invoked
	 * 
	 */
	public void removeCameraAvailableCallback(
			final CameraAvailableCallback callback) {
		synchronized (mLock) {
			mAvailableCallbacks.remove(callback);
		}
	}

	private static final int CMD_COLLISION = 0;
	private static final int CMD_COLLISION_INIT_EVENTQUEUE = 1;
	private static final int CMD_COLLISION_REMOVE_EVENTQUEUE = 2;
    private static final int CMD_COLLISION_SET_NORMAL_STATUS = 3;
    private static final int CMD_COLLISION_SET_SUSPEND_STATUS = 4;
    private static final int CMD_COLLISION_SET_NORMAL_SENSITY = 5;
    private static final int CMD_COLLISION_SET_SUSPEND_SENSITY = 6;
    private static final int CMD_COLLISION_GET_NORMAL_STATUS = 7;
    private static final int CMD_COLLISION_GET_SUSPEND_STATUS = 8;
    private static final int CMD_COLLISION_GET_NORMAL_SENSITY = 9;
    private static final int CMD_COLLISION_GET_SUSPEND_SENSITY = 10;
    private static final int CMD_SET_BT_WAKEUP_SUPPORT         = 11;
    private static final int CMD_GET_BT_WAKEUP_SUPPORT         = 12;

	private static final int COLLISION_SET_CARCORDER_THRESHOLD = 0;
    private static final int COLLISION_GET_CARCORDER_THRESHOLD = 1;

	private static final int COLLISION_SET_GSENSOR_EVENTRATE   = 4;
	private static final int COLLISION_GET_GSENSOR_EVENTRATE   = 5;

	/**
	 * Set car collision threshold. The
	 * {@link CollisionCallback#onCOLLISION(int, int)} callback only triggered
	 * when the detected collision value >= threshold. If this method never
	 * invoked, the default threshold is
	 * {@link CollisionCallback#COLLISION_MEDIUM}
	 * 
	 * @param threshold
	 *            <ul>
	 *            <li>{@link CollisionCallback#COLLISION_LOW}
	 *            <li>{@link CollisionCallback#COLLISION_MEDIUM}
	 *            <li>{@link CollisionCallback#COLLISION_HIGH}
	 *            </ul>
	 */
	public void setCollisionThreshold(int threshold) {
		try {
			getCarcorderService().sendCommand(CMD_COLLISION, threshold, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Enable or disable collision in normal situation
	 * 
	 * @param fgEnable
	 *            true means the collision function is enabled in normal case;
	 *            otherwise, which means this function is disabled
	 * 
	 */
	public void setNormalCollision(boolean fgEnable) {
		try {
			getCarcorderService().sendCommand(CMD_COLLISION_SET_NORMAL_STATUS, (fgEnable ? 1 : 0), -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Enable or disable collision in suspend situation
	 * 
	 * @param fgEnable
	 *            true means the collision function is enabled in suspend case;
	 *            otherwise, which means this function is disabled
	 * 
	 */
	public void setSuspendCollision(boolean fgEnable) {
		try {
			getCarcorderService().sendCommand(CMD_COLLISION_SET_SUSPEND_STATUS, (fgEnable ? 1 : 0), -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Set car collision threshold in normal case. The
	 * {@link CollisionCallback#onCOLLISION(int, int)} callback only triggered
	 * when the detected collision value >= threshold. If this method never
	 * invoked, the default threshold is
	 * {@link CollisionCallback#COLLISION_MEDIUM}
	 * 
	 * @param threshold
	 *            <ul>
	 *            <li>{@link CollisionCallback#COLLISION_LOW}
	 *            <li>{@link CollisionCallback#COLLISION_MEDIUM}
	 *            <li>{@link CollisionCallback#COLLISION_HIGH}
	 *            </ul>
	 */
	public void setNormalCollisionSensity(int level) {
		try {
			getCarcorderService().sendCommand(CMD_COLLISION_SET_NORMAL_SENSITY, level, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Set car collision threshold in suspend case. The
	 * {@link CollisionCallback#onCOLLISION(int, int)} callback only triggered
	 * when the detected collision value >= threshold. If this method never
	 * invoked, the default threshold is
	 * {@link CollisionCallback#COLLISION_MEDIUM}
	 * 
	 * @param threshold
	 *            <ul>
	 *            <li>{@link CollisionCallback#COLLISION_LOW}
	 *            <li>{@link CollisionCallback#COLLISION_MEDIUM}
	 *            <li>{@link CollisionCallback#COLLISION_HIGH}
	 *            </ul>
	 */
	public void setSuspendCollisionSensity(int level) {
		try {
			getCarcorderService().sendCommand(CMD_COLLISION_SET_SUSPEND_SENSITY, level, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get car collision status in normal case. 
	 * 
	 * @return true means collision is enabled for normal case,
	 *             otherwise, which means collision is disabled for normal case
	 */
	public boolean getNormalCollision() {
	    int status = 0;
		try {
			status = getCarcorderService().sendCommand(CMD_COLLISION_GET_NORMAL_STATUS, -1, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return (status == 1) ? true : false;
	}

	/**
	 * Get car collision status in suspend case. 
	 * 
	 * @return true means collision is enabled for suspend case,
	 *             otherwise, which means collision is disabled for suspend case
	 */
	public boolean getSuspendCollision() {
	    int status = 0;
		try {
			status = getCarcorderService().sendCommand(CMD_COLLISION_GET_SUSPEND_STATUS, -1, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return (status == 1) ? true : false;
	}

	/**
	 * Get car collision sensity in normal case. 
	 * 
	 * @return the setting of collision sensity for normal case.
	 *            <ul>
	 *            <li>{@link CollisionCallback#COLLISION_LOW}
	 *            <li>{@link CollisionCallback#COLLISION_MEDIUM}
	 *            <li>{@link CollisionCallback#COLLISION_HIGH}
	 *            </ul>
	 */
	public int getNormalCollisionSensity() {
	    int sensity = 0;
		try {
			sensity = getCarcorderService().sendCommand(CMD_COLLISION_GET_NORMAL_SENSITY, -1, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return sensity;
	}

	/**
	 * Get car collision sensity in suspend case. 
	 * 
	 * @return the setting of collision sensity for suspend case.
	 *            <ul>
	 *            <li>{@link CollisionCallback#COLLISION_LOW}
	 *            <li>{@link CollisionCallback#COLLISION_MEDIUM}
	 *            <li>{@link CollisionCallback#COLLISION_HIGH}
	 *            </ul>
	 */
	public int getSuspendCollisionSensity() {
	    int sensity = 0;
		try {
			sensity = getCarcorderService().sendCommand(CMD_COLLISION_GET_SUSPEND_SENSITY, -1, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return sensity;
	}

	/**
	 * Set car collision threshold. 
	 * @param x   the threshold of Gx on the x-axis
       * @param y   the threshold of Gy on the y-axis
       * @param z   the threshold of Gz on the z-axis
       * the value of x,y,z must be greater than 0
       * @param level, like a flag,its value is the same as 
       * {@link CollisionCallback#onCOLLISION(int COLLISION, int status)}  COLLISION
	 * @return 0 if success,otherwise fail
	 */
	public int setCollisionThreshold(float x,float y,float z,int level){
        try {
			String params=Float.toString(x)+","+Float.toString(y)+","+Float.toString(z)+","+Integer.toString(level);
			return getCarcorderService().setCollisionParameters(COLLISION_SET_CARCORDER_THRESHOLD, params);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Get car collision threshold. 
	 * @return float[] values
	 * values[0]: the threshold of Gx on the x-axis that be set
	 * values[1]: the threshold of Gy on the y-axis that be set
	 * values[2]: the threshold of Gz on the z-axis that be set
	 * values will be null if getCollisionThreshold fail
	 */
	public float[] getCollisionThreshold(){
        try {
			String params = getCarcorderService().getCollisionParameters(COLLISION_GET_CARCORDER_THRESHOLD);
            if(params==null||params.equals("")){
                Log.e(TAG,"params is null or empty");
				return null;
            }
			String str[]=params.split(",");
			if(str.length !=4){
               Log.e(TAG,"str.length("+str.length+") is bad value.");
			   return null;
			}
			float[] result=new float[3];
			result[0]=Float.parseFloat(str[0]);
			result[1]=Float.parseFloat(str[1]);
			result[2]=Float.parseFloat(str[2]);
            return result;
		} catch (Exception e) {
			e.printStackTrace();
		}  
         return null;
	}

    /**
	 * Set gsensor event report interval
	 *
	 * @param delayMs the delay time of two gsensor event 
	 * and unit is millisecond
	 */
	public int setGsensorEventRate(int delayMs){
		 try {
			  String params=Integer.toString(delayMs);
			  return getCarcorderService().setCollisionParameters(COLLISION_SET_GSENSOR_EVENTRATE, params);
		 } catch (RemoteException e) {
			  e.printStackTrace();
		 }
		 return -1;
	 }
	/**
	 * Get gsensor event report interval
	 *
	 * @return the interval of gsensor event, 
	 * the value should more than 0 if success   
	 */
	 public int getGsensorEventRate(){
		 try {
			  String params=getCarcorderService().getCollisionParameters(COLLISION_GET_GSENSOR_EVENTRATE);
			  if(params==null||params.equals("")){
				  Log.e(TAG,"getGsensorEventRate params is null or empty");
				  return -1;
			  }
			  return Integer.parseInt(params);
		  } catch (RemoteException e) {
			   e.printStackTrace();
		  }
		  return -1;
	   }

	/**
	 * Register a callback to be invoked when car collision happened.
	 * 
	 * @param callback
	 *            the callback that will be run
	 */
	public void addCollisionCallback(CollisionCallback callback) {
		if (mCollisionCallbacks.isEmpty()) {
			try {
				getCarcorderService().sendCommand(CMD_COLLISION_INIT_EVENTQUEUE,
						-1, -1);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		synchronized (mLock) {
			if (callback != null) {
				mCollisionCallbacks.add(callback);
			}
		}
	}

	/**
	 * Unregister the installed callback
	 * 
	 * @param callback
	 *            the callback that will no longer be invoked
	 * 
	 */
	public void removeCollisionCallback(CollisionCallback callback) {
		synchronized (mLock) {
			if (callback != null) {
				mCollisionCallbacks.remove(callback);
			}
		}
		if (mCollisionCallbacks.isEmpty()) {
			try {
				getCarcorderService().sendCommand(
						CMD_COLLISION_REMOVE_EVENTQUEUE, -1, -1);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/**
	 * Register a callback to be invoked when carcorder service is dead.
	 * 
	 * @param callback
	 *            the callback that will be run
	 */
	public void addCarcorderDeathCallback(CarcorderDeathCallback callback) {
		synchronized (mLock) {
			if (callback != null) {
				mServiceDeathCallbacks.add(callback);
			}
		}
	}

	/**
	 * Unregister the installed callback
	 * 
	 * @param callback
	 *            the callback that will no longer be invoked
	 * 
	 */
	public void removeCarcorderDeathCallback(CarcorderDeathCallback callback) {
		synchronized(mLock) {
            if (callback != null) {
                mServiceDeathCallbacks.remove(callback);
            }
        }
	}

	/**
	 * Helper for openning a connection to a camera with the given ID. The
	 * newly-created camera device will never disconnect unless
	 * {@link #closeCameraDevice(int)} invoked or carcorder service died.
	 * 
	 * @param cameraId
	 *            The unique identifier of the camera device to open
	 * @return A handle to the newly-created camera device.
	 */
	public CameraDevice openCameraDevice(int cameraId) {
		BinderHolder holder = new BinderHolder();
        int status = -1;
		try {
			status = getCarcorderService().connectDevice(cameraId, holder);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if (status != 0) {
            Log.d(TAG, "Failed to open camera " + cameraId);
            return null;
        }
		ICarCamDeviceUser device = ICarCamDeviceUser.Stub.asInterface(holder
				.getBinder());
		if (device == null) {
            Log.d(TAG, "Failed to get CarCamDeviceUser of camera " + cameraId);
			return null;
		}
        
		return new CameraDevice(device);
	}

	/**
	 * Disconnect camera device with the given ID.
	 *
	 * @param cameraId
	 *            The opened camera id
	 */
	public void closeCameraDevice(int cameraId) {
		try {
			getCarcorderService().disconnectDevice(cameraId);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public void unlockProtectedFile(String filename) {
        try {
            getCarcorderService().unlockProtectedFile(filename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /*
       * Get the car status. Just sending the command to driver, 
       * and the status is feedbacking in the {@link #onReverse(int)}
       * 
       */
    public void queryCarStatus() {
        try {
            getCarcorderService().queryCarStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

  	/*
     * the flag that car motion or engine in unknow state
     */
    public static final int CAR_STATE_UNKNOWN=-1;

    /*
     * the flag that car is reversing
     */
   public static final int CAR_MOTION_REVERSE=0;

	/*
   * the flag that car is driving normally
   */
  public static final int CAR_MOTION_NORMAL=1;

  /*
   * the flag that car engine is flameout
   */
	public static final int CAR_ENGINE_FLAMEOUT=0;

	/*
   * the flag that car engine is working
   */
	public static final int CAR_ENGINE_WORKING=1;
	
	/**
	 * Get the car motion state
	 *@return the status of car motion
	 *
	 **/
	public int queryCarMotionState(){
       try {
            return getCarcorderService().queryCarMotionState();
       } catch (RemoteException e) {
            e.printStackTrace();
       }
		return CAR_STATE_UNKNOWN;
	}

	/**
	 * Get the car engine state
	 *@return the status of car engine
	 **/
	public int queryCarEngineState(){
       try {
            return getCarcorderService().queryCarEngineState();
        } catch (RemoteException e) {
            e.printStackTrace();
       }
		 return CAR_STATE_UNKNOWN;
	}
	
    /*
	 * To control the default behavior when ACC is to off or is to On on special device.
	 * The default behavior is that the carcorder service sends broadcast message to system ui when 
	 * Acc is to off or is to on on Special device
	 * 
	 * @param enable
	 *            true means the default behavior is working, and the {@link #onEngineChanged(int)} callback isn't available
	 *            false means the default behavior isn't available and the {@link #onEngineChanged(int)} callback is working
      **/
    public void setDefaultAccOffBehavior(boolean enable) {
        try {
            getCarcorderService().setDefaultAccOffBehavior(enable);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return;
    }

    /**
	 * enable or disable bluetooth wakeup support feature
	 * @param fpEnable if true enable this featrue,false to disable
	 * @return return 0 on success,less than 0 on failure
	 **/
	public int setBTRCwakeup(boolean fgEnable){
        int result = -1;
		try {
			result = getCarcorderService().sendCommand(CMD_SET_BT_WAKEUP_SUPPORT, (fgEnable ? 1 : 0), -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Get the state of bluetooth wakeup support feature
	 * @return if this feature is enable will return true,otherwise false
	 **/
	public boolean getBTRCwakeup(){
        int result = 0;
		try {
			result = getCarcorderService().sendCommand(CMD_GET_BT_WAKEUP_SUPPORT, -1, -1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return (result == 1) ? true : false;
	}
	
	/**
       *  send cmd to ipod ,it could like to call ipod function
       */
	protected int postIpodCommand(String cmd,String params){
       try {
            return getCarcorderService().postIpodCommand(cmd,params);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
		return -1;
	}

	/**
	 *  get a IpodProxy,we can set param to ipod by this proxy
	 */
	public IpodProxy getIpodProxy(){

	   return IpodProxy.getInstance(gCarcorderManager);
    }
	
	@Override
	public void binderDied() {
		mCarcorderService = null;
        synchronized (mLock) {
            for (CarcorderDeathCallback callback : mServiceDeathCallbacks) {
    			if (callback != null) {
    				callback.onDeath(0, null);
    			}
    		}
        }
	}
}
