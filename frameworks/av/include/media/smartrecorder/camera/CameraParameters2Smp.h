/*
 * Copyright (C) 2014 The Android Open Source Project
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

#ifndef ANDROID_HARDWARE_CAMERA_PARAMETERS2_SMP_H
#define ANDROID_HARDWARE_CAMERA_PARAMETERS2_SMP_H

#include "CameraParametersSmp.h"
#include <camera/CameraParameters2.h>

namespace android {

/**
 * A copy of CameraParameters plus ABI-breaking changes. Needed
 * because some camera HALs directly link to CameraParameters and cannot
 * tolerate an ABI change.
 */
class CameraParameters2Smp: public CameraParameters2
{
public:
    CameraParameters2Smp();
    CameraParameters2Smp(const String8 &params) { unflatten(params); }
    ~CameraParameters2Smp();
	void setWaterMarkTimerSize(int width, int height);
	void getWaterMarkTimerSize(int *width, int *height) const;
	void setWaterMarkTimerOffset(int TimerOffsetX, int TimerOffsetY);
	void getWaterMarkTimerOffset(int *TimerOffsetX, int *TimerOffsetY) const;
	void setWaterMarkTimerDataAddr(char* timerdataaddr);
	int  getWaterMarkTimerDataAddr();
	void setWaterMarkOffset(int offsetX, int offsetY);
	void getWaterMarkOffset(int *offsetX, int *offsetY) const;
	const char *getWaterMarkDataPath() const;
	void setWaterMarkDataPath(const String8 format);
	const char* getWaterMarkDataOwner() const;
};
}; // namespace android

#endif
