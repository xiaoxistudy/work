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

#ifndef ANDROID_HARDWARE_PHOTOGRAPHY_ICARCAMDEVICEUSER_H
#define ANDROID_HARDWARE_PHOTOGRAPHY_ICARCAMDEVICEUSER_H

#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <gui/IGraphicBufferProducer.h>



namespace android {

class Surface;
class ICameraDeviceListener;

enum {
    NO_IN_FLIGHT_REPEATING_FRAMES = -1,
};

class ICarCamDeviceUser : public IInterface
{
    /**
     * Keep up-to-date with ICarCamDeviceUser.aidl in mediatek extended framework
     */
public:
    DECLARE_META_INTERFACE(CarCamDeviceUser);

    virtual status_t        setPreviewTarget(
            int width, int height,
            const sp<IGraphicBufferProducer>& bufferProducer) = 0;

	virtual String16        getParameters() = 0;
	virtual void            setParameters(const String16& params) = 0;
	virtual status_t        startPreview() = 0;
	virtual status_t        stopPreview() = 0;
	virtual status_t        startRecord() = 0;
	virtual status_t        stopRecord() = 0;
	virtual status_t        startMotionDetection() = 0;
	virtual status_t        stopMotionDetection() = 0;
	virtual status_t        startADAS() = 0;
	virtual status_t        stopADAS() = 0;
    virtual void            notifyEvent(int eventId, int arg1, int arg2, const String16& params) = 0;

    // Returns 'OK' if operation succeeded
    // - Errors: ALREADY_EXISTS if the listener was already added
    virtual status_t addListener(const sp<ICameraDeviceListener>& listener)
                                                                            = 0;
    // Returns 'OK' if operation succeeded
    // - Errors: BAD_VALUE if specified listener was not in the listener list
    virtual status_t removeListener(const sp<ICameraDeviceListener>& listener)
                                                                            = 0;
    virtual void shareAshmem(int type, int fd, size_t size) = 0;
    virtual void clearAshmem(int type) = 0;
    virtual void clearWaterMarkData(int cleardata) = 0;
    virtual void writeAshmem(int type, size_t size) = 0;

    virtual void takePicture(const String16& jpgname) = 0;

    virtual status_t startSubVideoFrame() = 0 ;
	virtual status_t stopSubVideoFrame() = 0 ;

    virtual status_t queryAeLv() = 0;
};

// ----------------------------------------------------------------------------

class BnCarCamDeviceUser: public BnInterface<ICarCamDeviceUser>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android

#endif
