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

import android.view.Surface;
import com.mediatek.carcorder.ICameraDeviceListener;
import android.os.ParcelFileDescriptor;

/** @hide */
interface ICarCamDeviceUser
{
	 /**
     * Keep up-to-date with mediatek extended frameworks/av/include/carcorder/ICarCamDeviceUser.h
     *
     */

	// ints here are status_t
    int setPreviewTarget(int width, int height, in Surface surface);
	String getParameters();
	void setParameters(String params);
	int startPreview();
	int stopPreview();
	int startRecord();
	int stopRecord();
	int startMotionDetection();
	int stopMotionDetection();
	int startADAS();
	int stopADAS();
    oneway void notifyEvent(int eventId, int arg1, int arg2, in String params);
    int addListener(ICameraDeviceListener listener);
    int removeListener(ICameraDeviceListener listener);

    void shareAshmem(int type, in ParcelFileDescriptor fd, int size);
    void clearAshmem(int type);
    void writeAshmem(int type, int size);
    void clearWaterMarkData(int cleardata);

    void takePicture(String path);

    int startSubVideoFrame();
    int stopSubVideoFrame();
    int queryAeLv();

}
