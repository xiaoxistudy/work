/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 ** Copyright (C) 2008 The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 **
 ** limitations under the License.
 */

#ifndef ANDROID_MEDIARECORDER_SMP_H
#define ANDROID_MEDIARECORDER_SMP_H

#include <media/mediarecorder.h>

namespace android {

// The "msg" code passed to the listener in notify.
enum media_smart_recorder_event_type {
    // Track related event types
    MEDIA_RECORDER_FILE_EVENT_INFO               = 102,
    MEDIA_RECORDER_FILE_EVENT_DATA               = 103,    
    MEDIA_RECORDER_MOTION_FILE_EVENT_INFO        = 104,
    MEDIA_RECORDER_FILE_PROTECT_INFO        	 = 105,   
    MEDIA_RECORDER_FILE_EVENT_KEY_POINT          = 106,
};

enum media_smart_recorder_event_param {
    // Track related event types
    MEDIA_RECORDER_FILE_NAME               = 1,
    MEDIA_RECORDER_RECORD_ERROR            = 2,    
    MEDIA_RECORDER_SDCARD_FULL        	   = 3,
    MEDIA_RECORDER_MOTION_DETECT_STOP  	   = 4,
    MEDIA_RECORDER_PROTECT_PREVIOUS_FILE   = 5,
    MEDIA_RECORDER_SDCARD_DAMAGED          = 6,
    MEDIA_RECORDER_KEYPOINT_START          = 7,
    MEDIA_RECORDER_KEYPOINT_END            = 8,
    MEDIA_RECORDER_EVENT_PARAM_MAX         = 15,
};

enum media_smart_recorder_data_type {
    MEDIA_RECORDER_VIDEO_CALLBACK_DATA  = 501,    
    MEDIA_RECORDER_KEY_POINT_NTF_DATA   = 502,    
};

enum media_notify_file_mode{
	MEDIA_NOTIFY_FILE_DISABLE = 0,
	MEDIA_NOTIFY_FILE_SOURCE = 1,
	MEDIA_NOTIFY_FILE_PACKET = 2,
	MEDIA_NOTIFY_FILE_DUAL = 3,
};


};  // namespace android

#endif // ANDROID_MEDIARECORDER_H
