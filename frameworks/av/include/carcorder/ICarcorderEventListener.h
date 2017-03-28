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

#ifndef ANDROID_HARDWARE_ICARCORDEREVENT_LISTENER_H
#define ANDROID_HARDWARE_ICARCORDEREVENT_LISTENER_H

#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>

namespace android {

class ICarcorderEventListener : public IInterface
{
    /**
     * Keep up-to-date with ICarcorderEventListener.aidl in mediatek extended framework
     */
public:

    enum Event {
		EVENT_WEBCAM_PLUGGED,
		EVENT_REVERSE_GEAR,
		EVENT_EDOG,
    };

    DECLARE_META_INTERFACE(CarcorderEventListener);

    virtual void onEventChanged(Event event, int32_t arg1, int32_t arg2) = 0;
};

// ----------------------------------------------------------------------------

class BnCarcorderEventListener : public BnInterface<ICarcorderEventListener>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android

#endif
