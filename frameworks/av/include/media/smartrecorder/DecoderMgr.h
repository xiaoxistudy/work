/*
 * Copyright 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#ifndef MEDIA_MUXERSMP_H_
//#define MEDIA_MUXERSMP_H_


#include <media/stagefright/foundation/AHandler.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/AString.h>
#include <media/stagefright/NuMediaExtractor.h>
#include <media/stagefright/MediaCodec.h>
#include <smartrecorder/MediaSourceEx.h>
#include <smartrecorder/RecorderMgr.h>
#include <utils/Log.h>
#include <utils/List.h>
#include <utils/Errors.h>
#include <utils/RefBase.h>
#include <media/MediaProfiles.h>  // for cameraid




namespace android {

class RecorderMgr;
class MediaSourceEx;

const int TrackCount=2;


class DecoderMgr: public AHandler
	{
	
	  public:
		DecoderMgr(RecorderMgr *owner,
					 const sp<AMessage> &notify,
					int32_t cameraid);
		
		virtual ~DecoderMgr();
		status_t init(const char *outputFileName,const char *path);
	
		status_t  start(int64_t trimStartTimeMs,
						int64_t trimEndTimeMs);
		status_t stop();
		status_t setParam(sp<AMessage> format);
		int32_t mSubCameraId;
		bool mIsAudio;
		sp<MediaSource> mVideoOutSource;
		//status_t setDrainedBuffer(MediaBuffer *buffer,bool isDone);
		//sp<InfoListener> mInfoListener;
		//status_t setInfoListener(const sp<InfoListener>& listener); 
		DefaultKeyedVector<String8,String8>    mdecMap;
		 typedef struct {
		sp<AMessage> format;
		size_t	 trackNum;
		bool isAudio;
		} TRACKMAP_S;
		TRACKMAP_S	 mTrackInfo[TrackCount];
		
		sp<AMessage> mVideoformat;
		sp<AMessage> mAudioformat;
		sp<NuMediaExtractor> mextractor;
		sp<MediaSourceEx> mVideoInSource;
		sp<MediaSourceEx> mAudioInSource;	
		virtual void onMessageReceived(const sp<AMessage> &msg);
		bool IsDecMgrDoing(){return mStarted;};
	    void Releaseframe(int32_t index);
	
	private:
		
		 RecorderMgr *mRecorderMgr;
		 sp<MediaCodec> mCodec;
		 Vector<sp<ABuffer> > mInBuffers;
		 Vector<sp<ABuffer> > mOutBuffers;
		 bool mSignalledInputEOS;
		 bool mSawOutputEOS;
		 int64_t mNumBuffersDecoded;
		 int64_t mNumBytesDecoded;
		 //sp<MediaSource> mSource;
	
		 status_t startExtractor(const char *path,
								int64_t trimStartTimeMs,
								int64_t trimEndTimeMs);
		 status_t startDecoder(sp<AMessage> format);
		 status_t startEncoder();
		 
		 pthread_t		 mDecMgrDecodeTrd;//zhen
		 static void *ThreadWrapper(void *me);
		 void signalThreadExit();
		 void waitThreadExit();
		 void NotifyErr2Rec(status_t err);
		 Mutex		 mLock;
		 Condition	 mThreadExitCondition;
		 bool		 mThreadExit;
	
		 
		 enum {
				 kWhatDecStart	= 'Dstat',
				 kWhatDecInit = 'DInit',
				 kWhatDecStop = 'Dstop', 
				 kWhatGetOutBuf = 'DGetO',
				 kWhatSetParam ='SPrm',
				 kWhatReleasebuf = 'Relb'
			 };
		 
	    struct{
	    int32_t                     mVidFrm;
	    int32_t                     mVidEnc;
		int32_t                     mAudEnc;
		int32_t                     mOutFmt;
		int32_t                     mWidth;
		int32_t                     mHeight;
   	    }mEncParam;//MTK_CARCORD_NFY_DATA_SUPPORT
   	
		 sp<ALooper>			mDecMgrLooper;
		 //sp<AMessage>	 mCodecActivityNotify;
		 sp<AMessage> mNotify;
		 sp<ALooper> mCodecLooper;
		 void ScheduleGetOutput();
		 void signalEOS();
		
		 status_t threadEntry();
		 volatile bool mStarted;
		 volatile bool mStoped;
		 char *mOutPutFileName;
		 char *mSourcePath;
		 char*  mVidbitrate; 
		 
	};


};
//#endif

