/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef ANDROID_HARDWARE_ICAMERADEVICE_LISTENER_H
#define ANDROID_HARDWARE_ICAMERADEVICE_LISTENER_H

#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <string.h>

namespace android {

struct ADASInfo;

class ICameraDeviceListener : public IInterface
{
    /**
     * Keep up-to-date with ICameraDeviceListener.aidl in mediatek extended framework
     */
public:

    enum Status {
		CAMERA_STATUS_ERROR = 0x0001,
    	CAMERA_STATUS_SHUTTER = 0x0002,
    	CAMERA_STATUS_VIDEO = 0x0003,
    	CAMERA_STATUS_MOTION = 0x0004,
    	CAMERA_PICTURE_TAKEN = 0x0005,
    	RECORD_STATUS_CHANGED = 0x0006,
    };

    DECLARE_META_INTERFACE(CameraDeviceListener);

    virtual void onStatusChanged(Status status, int arg1, String16 arg2, int arg3) = 0;
	virtual void onADASCallback(ADASInfo* info) = 0;
    virtual void onVideoFrame(int fd, int dataType,int size) = 0;
	virtual void onAudioFrame(int fd, int dataType,int size) = 0;
};

// ----------------------------------------------------------------------------

class BnCameraDeviceListener : public BnInterface<ICameraDeviceListener>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android

#endif
