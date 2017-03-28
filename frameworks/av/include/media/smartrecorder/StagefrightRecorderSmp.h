/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

#ifndef STAGEFRIGHT_RECORDER_SMP_H_

#define STAGEFRIGHT_RECORDER_SMP_H_

//#include <smartrecorder/MediaRecorderBaseSub.h>
#include <smartrecorder/MediaSourceEx.h>
#include <smartrecorder/camera/CameraParametersSmp.h>
#include <StagefrightRecorder.h>

#define MTK_CARCORD_FEATURE_SUPPORT 1
#include <smartrecorder/MediaWriterSmp.h>


namespace android {

class CameraSourceSmp;
class CameraSourceTimeLapseSmp;
struct MediaSourceEx;//zhen
struct MediaWriterSmp;

struct StagefrightRecorderSmp : public StagefrightRecorder{
    StagefrightRecorderSmp();
    virtual ~StagefrightRecorderSmp();
	virtual status_t start();
    virtual status_t setOutputFile(const char *path);
	virtual status_t setOutputFile(int fd, int64_t offset, int64_t length);
	virtual status_t setOutputFileName(const char* fileName);
#if MTK_CARCORD_FEATURE_SUPPORT
	virtual void setAudSourceEx(sp<MediaSourceEx> &audioSource);
	virtual status_t setRecorderMgrListener(const sp<FileListener>& listener);
	virtual void setRecordFlag(int64_t flag, int64_t parameter, const String8 &param2);
    virtual void  setBeforeRecordingStatus(bool isrecordingstatus);
	virtual void changeEncoderBitRate(int64_t bitrate);
    virtual status_t setParameters(const String8 &params);
    virtual status_t setLowMemoryLimit(int lowmemoryLimit);
    int64_t mRecordMrgFlag;
	int64_t mRecordParameter;
	String8 mRecordFlagParam2;
#endif
#if  1//MTK_CARCORD_NFY_DATA_SUPPORT 
	virtual void useMetaData(void);
	virtual void getVidOutSourceEx(bool isAudio,sp<MediaSource> &videoSource);	
	virtual void setVidInSourceEx(sp<MediaSourceEx> &videoSource,bool isAudio);//zhen
#endif


    virtual status_t reset();
private:
    sp<MediaWriterSmp> mWriter;
	int64_t mMuteRecordingAudio;
    int32_t mVideoMinBitRate;
    int32_t mVideoMaxBitRate;
	int32_t mVideoBitRateSetByUser;
	int64_t mAutoBitrate;
    int32_t mPreVideoBitRate;
#if MTK_CARCORD_FEATURE_SUPPORT
	sp<MediaSourceEx> mAudEncSource;
	sp<FileListener> mRecMgrListener;
	char mOutputPath[MTK_CARCORD_FILE_PATH_LENGTH];
	char* mCustomizeFileName;
	int  mLowMemoryLimit;
#endif
#if  1//MTK_CARCORD_NFY_DATA_SUPPORT 
	int32_t  mSubCameraData;//zhen	
	int32_t  mSubAudData;//zhen	
     bool mNfyDataFlag;//zhen
	sp<MediaSourceEx> mVidNfyInSource;//zhen
	sp<MediaSourceEx> mAudNfyInSource;//zhen
	sp<MediaSource> mVidNfyOutSource;
	sp<MediaSource> mAudNfyOutSource;
    bool mNfyFileFlag;
	int32_t mNfyFileMode;
#endif
    sp<CameraSourceTimeLapseSmp> mCameraSourceTimeLapse;
    status_t setupMPEG4orWEBMRecording();
    status_t setupMPEG2TSRecording();
    status_t setupCameraSource(sp<CameraSourceSmp> *cameraSource);
    status_t setupAudioEncoder(const sp<MediaWriterSmp>& writer);
    status_t setParameter(const String8 &key, const String8 &value);
	status_t setParamMaxFileDurationUsTemp(int32_t durationUs);
	status_t setParamMuteRecordingAudio(int32_t mute_recording_audio);
    status_t setParamVideoEncodingMinBitRate(int32_t minbitRate);
    status_t setParamVideoEncodingMaxBitRate(int32_t maxbitRate);
	status_t setParamVideoEncodingBitRateSetByUser(int32_t VideoBitRateSetByUser);

    
    void clipVideoBitRate();
	status_t prepareInternal();
	status_t checkVideoEncoderCapabilities(
		bool *supportsCameraSourceMetaDataMode);
	
	status_t setupMediaSource(sp<MediaSource> *mediaSource);

    StagefrightRecorderSmp(const StagefrightRecorderSmp &);
    StagefrightRecorderSmp &operator=(const StagefrightRecorderSmp &);

};

}  // namespace android

#endif  // STAGEFRIGHT_RECORDER_H_

