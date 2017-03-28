/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef MEDIA_WRITER_SMP_H_

#define MEDIA_WRITER_SMP_H_

#include <utils/RefBase.h>
#include <utils/String8.h>
#include <media/IMediaRecorderClient.h>
#include <media/stagefright/MediaWriter.h>

#define MTK_CARCORD_FILE_PATH_LENGTH 128

namespace android {

class MediaBuffer;
class MediaCodecSource;
class FileListener: virtual public RefBase
{
public:
    virtual void notify(int32_t msgType, const String8& params, int32_t ext2) = 0;
	virtual void dataCallback2Mgr(int32_t msgType, int32_t ext1, MediaBuffer *dataPtr) = 0;
	virtual void notifySdcardWritelimite(int32_t CameraId, int64_t bitrate) = 0;
};

struct MediaWriterSmp : public MediaWriter {

	enum WRITER_FLAG {
		kFlagNULL									  = 0,
        kFlagIsProtect                                = 1,
        kFlagRecordError                              = 2,
        kFlagSDCardFull                               = 4,
        kFlagStopRecord                               = 8,
        kFlagFileRotateRequested                      = 16,
        kFlagFileNtyData							  = 32,
        kFlagSetRecorderDuration                      = 64,
        kFlagFileNtyFile                              = 2048, 
    };
	
    MediaWriterSmp()
        : mRecordFlag(0),
          mRecordFlagParameter(0),
          bIsMuteAudio(0),
          bIsMotionDetect(0),
          bIsFilePathValid(true),
          mMotionFileDurationLimitUs(0),
          mCameraID(-1),
          mEndTimestampUs(0),
          mMaxFileDurationTemp(0),
          bNtfMuxerTimePoint(false),
          mPreVidDuration(0),
          mNextVidDuration(0),
          mContinuousLock(false),
          mNfyFileMode(0),
          mVideoBitrate(-1),
          mVideoMaxBitrate(-1),
          mVideoMinBitrate(-1){
          
          memset(mFilePath, 0, MTK_CARCORD_FILE_PATH_LENGTH);
    }

    virtual void  setBeforeRecordStatus(bool isrecordingstatus){}
    virtual void  setLowMemoryLimit(int lowmemoryLimit){}
    virtual void setMaxFileDuration(int64_t durationUs) { mMaxFileDurationLimitUs = durationUs; }
    virtual void setMaxFileDurationTemp(int64_t flag,int64_t durationUs) 
	{
		mRecordFlag |= flag;
		mMaxFileDurationLimitUsTemp = durationUs; 
	}
    virtual void setKeyPointNfyFileFlag(bool bKeyPointNfyFileFlag){}

	virtual void setEndTimestampUs(bool bEos, int64_t endTimestampUs)
	{
		if (bEos){
        mEndTimestampUs = endTimestampUs;
		ALOGE("mEndTimestampUs:%d", mEndTimestampUs);
	    }else{
	        mEndTimestampUs = 0;
	    }
	}
	
	virtual void setRecorderMuteAudio(int64_t isMuteAudio)
	{
	    if(isMuteAudio)
	    {
			bIsMuteAudio = true;
	    }
		else
		{
			bIsMuteAudio = false;
		}
	}
	virtual void setMotionDetect(int64_t isMotionDectect,int64_t durationUs)
	{
	    if(isMotionDectect)
	    {
			bIsMotionDetect = true;
	    }
		else
		{
			bIsMotionDetect = false;
		}
		mMotionFileDurationLimitUs = durationUs;
	}
	virtual void setCameraID(int32_t cameraId)
	{
		mCameraID = cameraId;
	}
    virtual void setRecorderFileListener(const sp<FileListener>& listener){
        mFileListener = listener;
    }
	virtual void setRecorderFilePath(char filePath[]){
        memset(mFilePath, 0, MTK_CARCORD_FILE_PATH_LENGTH);
        strncpy(mFilePath, filePath, strlen(filePath));
    }
	virtual void setRecorderFileName(char fileName[]){
		mhasSetFileName = true;
        memset(mCustomizeFileName, 0, MTK_CARCORD_FILE_PATH_LENGTH);
        strncpy(mCustomizeFileName, fileName, strlen(fileName));
		ALOGE("[%d]setRecorderFileName= %s",mCameraID, mCustomizeFileName);
    }
	
	virtual void setRecordFlag(int64_t flag, int64_t parameter, const String8 &param2){
		ALOGE("[%d]setRecordFlag flag:%lld,mRecordFlagParameter:%lld, parameter:%lld",
			   mCameraID, flag, mRecordFlagParameter, parameter);
		if((flag & kFlagIsProtect) && parameter){
			mPreVidDuration = (parameter>>16)&0xffff;
			mNextVidDuration = parameter & 0xffff;
			if(mPreVidDuration <= 0)
				mPreVidDuration = mNextVidDuration;
			
			mContinuousLock = false;
			if(mRecordFlag & kFlagIsProtect){
				if(!bNtfMuxerTimePoint){//continuous lock, and last lock finished;
					mRecordFlagParameter = parameter;
					ALOGE("line:110. parameter:%lld, mPreVidDuration:%d, mNextVidDuration:%d",parameter, mPreVidDuration, mNextVidDuration);
				}else{//continuous lock, and last lock not finished, add nextVideoDuration to FileDuration;
					ALOGE("line:112.continuous Lock mMaxFileDurationLimitUs:%lld, mPreVidDuration:%d, mNextVidDuration:%d",mMaxFileDurationLimitUs, mPreVidDuration, mNextVidDuration);
					//mMaxFileDurationLimitUs = mMaxFileDurationLimitUs + mNextVidDuration*1000LL*1000LL/2;
					bNtfMuxerTimePoint = false;
					mContinuousLock = true;
				}
			}else{//first lock
				mRecordFlagParameter = parameter;
				ALOGD("line:114. parameter:%lld, mPreVidDuration:%d, mNextVidDuration:%d",parameter, mPreVidDuration, mNextVidDuration);
			}
			mRecordFlag |= flag;
		}
		else if((flag & kFlagIsProtect) && !parameter){ //unlock
			if((mRecordFlag & kFlagIsProtect) && bNtfMuxerTimePoint && mMaxFileDurationTemp > 0){
				ALOGW("line:116. mMaxFileDurationTemp:%lld, mMaxFileDurationLimitUs:%lld", mMaxFileDurationTemp, mMaxFileDurationLimitUs);
				mMaxFileDurationLimitUs = mMaxFileDurationTemp;
				mMaxFileDurationTemp = 0;
			}
			mPreVidDuration = 0;
			mNextVidDuration = 0;
			mContinuousLock = false;
			mRecordFlag &= ~kFlagIsProtect;
			bNtfMuxerTimePoint = false;
		}else if(flag & kFlagFileNtyFile){
			mNfyFileMode = parameter;
			mRecordFlag |= flag;
		}
		else
			mRecordFlag |= flag;
		
		if(strlen(param2.string()) != 0){
             memset(mFilePath, 0, sizeof(mFilePath));
             strncpy(mFilePath, param2.string(), strlen(param2.string()));
             bIsFilePathValid = false;
		}
    }
//////	
    virtual void     addCodecSource(const sp<MediaCodecSource> &source){}
    virtual void setVideoBitRate(int32_t videoBitrate){
        ALOGE("setVideoBitRate: videoBitrate:%d", videoBitrate);
        mVideoBitrate = videoBitrate;
    }
    virtual void setVideoMaxBitRate(int32_t videoMaxBitrate){
        ALOGE("setVideoBitRate: videoMaxBitrate:%d", videoMaxBitrate);
        mVideoMaxBitrate = videoMaxBitrate;
    }
    virtual void setVideoMinBitRate(int32_t videoMinBitrate){
        ALOGE("setVideoBitRate: videoMinBitrate:%d", videoMinBitrate);
        mVideoMinBitrate = videoMinBitrate;
    }
/////

protected:
    int64_t mMaxFileDurationLimitUsTemp;
	int64_t mRecordFlag;
	int64_t mRecordFlagParameter;
	int64_t mEndTimestampUs;
	bool    bNtfMuxerTimePoint;
	int64_t mMaxFileDurationTemp;
    int32_t mPreVidDuration;
    int32_t mNextVidDuration;
	bool    mContinuousLock;
	bool    bIsMuteAudio;
	bool    mAVSync;
	bool    bIsMotionDetect;
    int64_t mMotionFileDurationLimitUs;
	int32_t mCameraID;
	int32_t mNfyFileMode;
    sp<FileListener> mFileListener;
	char mFilePath[MTK_CARCORD_FILE_PATH_LENGTH];
	char mCustomizeFileName[MTK_CARCORD_FILE_PATH_LENGTH];
	bool mhasSetFileName;
    bool    bIsFilePathValid;
////
    int32_t mVideoBitrate;
    int32_t mVideoMaxBitrate;
    int32_t mVideoMinBitrate;
///
	
    void notify2Mgr(int msg, const String8& params, int ext2) {
        if (mFileListener != NULL) {
            mFileListener->notify(msg, params, ext2);
        }
    }

	void notifyData2Mgr(int32_t msg, int32_t ext1, MediaBuffer *dataPtr) {
        if (mFileListener != NULL) {
            mFileListener->dataCallback2Mgr(msg, ext1, dataPtr);
        }
    }
	void notifyBitRate2Mgr(int32_t CameraId, int64_t bitrate) {
        if (mFileListener != NULL) {
            mFileListener->notifySdcardWritelimite(CameraId, bitrate);
        }
    }
private:
    MediaWriterSmp(const MediaWriterSmp &);
    MediaWriterSmp &operator=(const MediaWriterSmp &);
};

}  // namespace android

#endif  // MEDIA_WRITER_H_
