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
package com.mediatek.carcorder;

import android.hardware.camera2.utils.BinderHolder;
//import com.mediatek.carcorder.BinderHolder;
import com.mediatek.carcorder.ICarcorderEventListener;
import com.mediatek.carcorder.ICarCamDeviceUser;
import com.mediatek.carcorder.CameraInfo;


/** @hide */
interface ICarcorderService
{
    int getNumberOfCameras();

    int addListener(ICarcorderEventListener listener);

    int removeListener(ICarcorderEventListener listener);

    int connectDevice(int cameraId, out BinderHolder device);

    int disconnectDevice(int cameraId);

    int sendCommand(int cmd, int arg1, int arg2);

    void queryCarStatus();

    void unlockProtectedFile(String filename);
    
    int queryCarMotionState();

    int queryCarEngineState();

    void setDefaultAccOffBehavior(boolean enabled);

    int postIpodCommand(String cmd,String params);
    
    int setCollisionParameters(int paramType,String params);
    
    String getCollisionParameters(int paramType);

    int getCameraInfo(int cameraId, out CameraInfo cameraInfo);
}
