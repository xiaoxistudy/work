/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_HARDWARE_ICARCORDERSERVICE_H
#define ANDROID_HARDWARE_ICARCORDERSERVICE_H

#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <camera/Camera.h>


namespace android {

class String16;
class ICarcorderEventListener;
class ICarCamDeviceUser;


class ICarcorderService : public IInterface
{
public:
    /**
     * Keep up-to-date with ICarcorderService.aidl in mediatek extended frameworks
     */
   	enum {
    	GET_NUMBER_OF_CAMERAS = IBinder::FIRST_CALL_TRANSACTION,
    	ADD_LISTENER,
    	REMOVE_LISTENER,
    	CONNECT_DEVICE,
    	DISCONNECT_DEVICE,
    	SEND_COMMAND,
    	QUERY_CAR_STATUS,
    	UNLOCK_PROTECTED_FILE,
    	QUERY_CAR_MOTION_STATE,
		QUERY_CAR_ENGINE_STATE,
		SET_DEFAULT_ACC_OFF_BEHAVIOR,
		POST_IPOD_COMMAND,
		SET_COLLISION_PARAMETER,
		GET_COLLISION_PARAMETER,
    	GET_CAMERA_INFO,

        // Please keep this item as the last one
    	NOTIFY_LISTENER,
	};
    enum {
        USE_CALLING_UID = -1
    };

    enum {
        API_VERSION_1 = 1,
        API_VERSION_2 = 2,
    };

    enum {
        CAMERA_HAL_API_VERSION_UNSPECIFIED = -1
      };
public:
    static int mRealCameraNum;

public:
    DECLARE_META_INTERFACE(CarcorderService);

    virtual int32_t  getNumberOfCameras() = 0;
#if 0
    virtual status_t getCameraInfo(int cameraId,
            /*out*/
            struct CameraInfo* cameraInfo) = 0;
#endif

    // Returns 'OK' if operation succeeded
    // - Errors: ALREADY_EXISTS if the listener was already added
    virtual status_t addListener(const sp<ICarcorderEventListener>& listener)
                                                                            = 0;
    // Returns 'OK' if operation succeeded
    // - Errors: BAD_VALUE if specified listener was not in the listener list
    virtual status_t removeListener(const sp<ICarcorderEventListener>& listener)
                                                                            = 0;
	virtual void notifyListener(int event, int arg1, int arg2) = 0;

    virtual status_t connectDevice(
            int cameraId,
            /*out*/
            sp<ICarCamDeviceUser>& device) = 0;
	virtual status_t disconnectDevice(int cameraId) = 0;
	virtual int32_t sendCommand(int cmd, int arg1, int arg2) = 0;
  virtual void queryCarStatus() = 0;
  virtual void unlockProtectedFile(const String16& filename) = 0;
  	
	virtual int queryCarMotionState() = 0;
	virtual int queryCarEngineState() = 0;

    virtual void setDefaultAccOffBehavior(bool enabled) = 0;

    virtual int postIpodCommand(const String16& cmd, const String16& params) = 0;
    virtual int setCollisionParameters(int paramType, const String16& params) = 0;
    virtual String16 getCollisionParameters(int paramType) = 0;
    virtual status_t getCameraInfo(int cameraId, struct CameraInfo* cameraInfo) = 0;
    //only for native
};

// ----------------------------------------------------------------------------

class BnCarcorderService: public BnInterface<ICarcorderService>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android

#endif
