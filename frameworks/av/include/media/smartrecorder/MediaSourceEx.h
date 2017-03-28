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

#ifndef MEDIA_SOURCE_EX_H_
#define MEDIA_SOURCE_EX_H_

#include <media/stagefright/MediaSource.h>
#include <utils/List.h>
#include <media/stagefright/MediaBuffer.h>
#include <utils/threads.h>
#if 1//MTK_CARCORD_NFY_DATA_SUPPORT
#include <camera/ICamera.h>
#include <camera/ICameraRecordingProxyListener.h>
#include <camera/CameraParameters.h>
#include <camera/Camera.h>
#include <camera/CameraParameters.h>
#include <utils/RefBase.h>
#include <utils/String16.h>
#include <binder/IMemory.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/AString.h>

#endif
#include <media/stagefright/foundation/ABuffer.h>


namespace android {
#if 1//MTK_CARCORD_NFY_DATA_SUPPORT
class IMemory;
class Camera;
#endif

struct MediaSourceEx : public MediaSource, public MediaBufferObserver {
	public:
		MediaSourceEx();

	    virtual status_t 			setBuffers(MediaBuffer *dataPtr);
		virtual status_t 			start(MetaData *params = NULL);
	    virtual status_t 			stop();
	    virtual sp<MetaData> 		getFormat();
	    virtual status_t 			read(MediaBuffer **buffer, const ReadOptions *options = NULL);
		void 						sendSignal();
		status_t                    setFormat(sp<AMessage> format);
#if 1//MTK_CARCORD_NFY_DATA_SUPPORT
		virtual status_t            reset();
		status_t                    dataCallbackTimeStamp(int64_t timestampUs,int32_t msgType, const sp<IMemory> &data);
        status_t                    setVideoSize(int32_t width, int32_t height);
		status_t                    setVideoFrameRate( int32_t frames_per_second);
		status_t                    setCamera2Ex(const sp<Camera> &camera);
		status_t                    isCameraColorFormatSupported(const CameraParameters& params);
		sp<Camera>                  mCamera;
		sp<MetaData>                mMeta;
		int32_t                     mColorFormat;
		int32_t                     mVideoWidth;
		int32_t                     mVideoHeight;
		int32_t                     mVideoFrameRate;
#endif
#if 1//MTK_CARCORD_LOW_RES_KP_SUPPORT
		status_t                    dataCallbackTimeStamp(int64_t timestampUs, const sp<ABuffer> &data);
        status_t                    setFlag(int32_t flag);
		status_t                    setInputNotify(const sp<AMessage> &msg);

#endif
		// from MediaBufferObserver
    	virtual void 				signalBufferReturned(MediaBuffer *buffer);
	   enum SOURCE_FLAG {
	    kFlagNULL		= 0,
	    kFlagHaveFormat = 1, 
	    kFlagVfromMDP	= 2,							  
	   };

	protected:
		virtual 					~MediaSourceEx();
		
	private:
		Mutex 						mLock;
		Condition 					mFrameAvailableCondition;
		List<MediaBuffer * > 		mFramesReceived;
		MediaSourceEx(const MediaSourceEx &);
	    MediaSourceEx &operator=(const MediaSourceEx &);
#if 1//MTK_CARCORD_NFY_DATA_SUPPORT	
               List<sp<IMemory> >          mFramesBeingEncoded;
        List<MediaBuffer *>          mAbufFramesBeingEncoded;
		int32_t                   getColorFormat(const char* colorFormat);
               Condition                   mFrameCompleteCondition;
		void                        releaseQueuedFrames();
		int32_t                    mNumFramesReceived;
		int64_t                     mFirstFrameTimeUs;
		bool                 mCodecConfigReceived;
		int64_t              mStartTimeUs;
		bool                 mStarted;
		bool                 mStoped;
		int32_t              mFlags;
		sp<AMessage> mNotify;
		void     adjustIncomingANWBuffer(IMemory* data);
		void     adjustOutgoingANWBuffer(IMemory* data);
#endif 
};

}  // namespace android

#endif  // MEDIA_SOURCE_H_
