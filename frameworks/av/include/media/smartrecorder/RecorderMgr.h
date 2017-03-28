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

#ifndef ANDROID_MEDIARECORDER_MGR_H
#define ANDROID_MEDIARECORDER_MGR_H

#include <utils/Log.h>

#include <utils/List.h>
#include <utils/Errors.h>
#include <utils/RefBase.h>

#include <media/IMediaRecorderClient.h>
//#include <media/IMediaDeathNotifier.h>
//#include <smartrecorder/MediaRecorderBaseSub.h>
//#include <media/MediaRecorderBase.h>
#include <smartrecorder/StagefrightRecorderSmp.h>
#include <smartrecorder/mediarecorderSmp.h>
#include "MediaSourceEx.h"
#include <media/stagefright/foundation/AHandler.h>
#include <media/stagefright/NuMediaExtractor.h>
#include <media/stagefright/MediaCodec.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/IMediaHTTPService.h>
#include <media/IMediaPlayerService.h>
#include <media/ICrypto.h>
#include <gui/Surface.h>

#define MTK_CARCORD_NFY_DATA_SUPPORT 1//zhen add
#define MTK_CARCORD_LOW_RES_KP_SUPPORT 1 //zhen add for low res key point

namespace android {

class Surface;
class IMediaRecorder;
class ICamera;
class ICameraRecordingProxy;
class IGraphicBufferProducer;
class Surface;
class AHandlerRecMgr;
class DecoderMgr;

typedef unsigned int       VAL_UINT32_T;       ///< unsigned int type definition

#if MTK_CARCORD_NFY_DATA_SUPPORT 
const int CAMERA_NUM = 2;//zhen
#endif

class InfoListener: virtual public RefBase
{
public:
	enum {
        INFO_LISTENER_TYPE_GALLERY = 0,
		INFO_LISTENER_TYPE_RECORD_STATUS = 1,
		INFO_LISTENER_MOTION_DETECT_STATUS = 2,
    };

	enum {
	    NTF_ADDED_FILE_NAME			= 0,
	    NTF_DELETED_FILE_NAME		= 1,
	};

    enum {
	    NTF_RECORD_STATUS_START		  = 0,
	    NTF_RECORD_STATUS_STOP		  = 1,
	    NTF_RECORD_STATUS_RECODING	  = 2,
        NTF_RECORD_STATUS_ERROR       = 3,
        NTF_RECORD_STATUS_SDCARD_FULL = 4,
        NTF_RECORD_STATUS_SDCARD_DAMAGED = 5,
        #if MTK_CARCORD_LOW_RES_KP_SUPPORT
        NTF_RECORD_STATUS_LOWRES_KEYPOINT_START =6,//zhen
        NTF_RECORD_STATUS_LOWRES_KEYPOINT_STOP =7,
        #endif
        NTF_RECORD_STATUS_KEYPOINT_START =8,
        NTF_RECORD_STATUS_KEYPOINT_STOP =9,
    };
    virtual void notify(int msg, const String16& params, int ext2) = 0;
	virtual void callbackdata(void* data, int32_t dataType,size_t length,int type)= 0;
};

class RecorderMgr: public RefBase{
public:
	static RecorderMgr* getInstance();
	RecorderMgr();
    virtual ~RecorderMgr();

	enum camera_id {
	    FRONT_CAMERA = 0,
	    BACK_CAMERA = 1,
#if MTK_CARCORD_NFY_DATA_SUPPORT 
	    FRONT_CAMERA_SUB = FRONT_CAMERA + CAMERA_NUM,//zhen
#endif
		USB_CAMERA = 3,
        CAMERA_MAX,
	};

	enum RECORD_FLAG {
		kFlagNULL									  = 0,
        kFlagIsProtect                                = 1,
        kFlagRecordError                              = 2,
        kFlagSDCardFull                               = 4,
        kFlagStopRecord                               = 8,
        kFlagFileRotateRequested                      = 16,
        kFlagFileNtyData							  = 32,
        kFlagSetRecorderDuration                      = 64,
		kFlagSetRecorderMuteAudio					  = 128,
		kFlagSetRecorderDynPath					  = 256,
		kFlagSetMotionDetect					  = 512,
		kFlagAutoBitrate                          =1024,
	#if MTK_CARCORD_LOW_RES_KP_SUPPORT
	    kFlagFileNtyFile                              =2048,
	#endif
    };
	enum OutputFormat {
        OUTPUT_FORMAT_MPEG_4 = 0,
        OUTPUT_FORMAT_WEBM   = 1,
        OUTPUT_FORMAT_TS     = 2,
        OUTPUT_FORMAT_LIST_END // must be last - used to validate format type
    };
	
	status_t    createRecordInst(camera_id CameraId);
	status_t    destroyRecordInst(camera_id CameraId);
	void        setRecordAction(int32_t cameraId, RECORD_FLAG recordFlag);
	void        clearRecordAction(int32_t cameraId, RECORD_FLAG recordFlag);
    void        died();
    status_t    initCheck(camera_id CameraId);
    status_t    setCamera(camera_id CameraId, const sp<ICamera>& camera, const sp<ICameraRecordingProxy>& proxy);
    status_t    setVideoSource(camera_id CameraId, int vs);
    status_t    setAudioSource(camera_id CameraId, int as);
    status_t    setOutputFormat(camera_id CameraId, int of);
    status_t    setVideoEncoder(camera_id CameraId, int ve);
    status_t    setAudioEncoder(camera_id CameraId, int ae);
    status_t    setOutputFile(camera_id CameraId, const char* filePath);
    status_t    setOutputFile(camera_id CameraId, int fd, int64_t offset, int64_t length);
    status_t    setLockFile(camera_id CameraId, const char* lockPath);
    status_t    setVideoSize(camera_id CameraId, int width, int height);
    status_t    setVideoFrameRate(camera_id CameraId, int frames_per_second);
    status_t    setParameters(camera_id CameraId, const String8& params);
    status_t    setLowMemoryLimit(int lowmemoryLimit);
    status_t    setListener(camera_id CameraId, const sp<IMediaRecorderClient>& listener);
	//status_t    setAudioListener(const sp<MediaRecorderListener>& listener);
	
	status_t    unlockFile(String8 params);
	status_t    setInfoListener(camera_id CameraId, const sp<InfoListener>& listener);
    status_t    setClientName(camera_id CameraId, const String16& clientName);
    status_t    prepare(camera_id CameraId);
    status_t    getMaxAmplitude(camera_id CameraId, int* max);
	int         getCameraCurrentState(camera_id CameraId);
    status_t    start(camera_id CameraId);
    status_t    stop(camera_id CameraId);
    status_t    startRecord(camera_id CameraId);
	status_t    stopRecord(camera_id CameraId);
    status_t    reset(camera_id CameraId);
    status_t    init(camera_id CameraId);
    status_t    closeRecord(camera_id CameraId);
    status_t    release();
    void        notify(int msg, const String8& params, int ext2);
    void        notify(int msg, int ext1, int ext2);
	void        recordMgrNotify(int32_t cameraId, char *param, int32_t msgType, int32_t status);
	void        configEncodRate(int32_t CameraId, int64_t bitrate);
	void        postData2Source(int32_t msgType, int32_t ext1, MediaBuffer *dataPtr);
    void        setBeforeRecordingStatus(camera_id CameraId,bool isrecordingstatus);
	void 		setRecordFlag(camera_id CameraId,int64_t flag, int64_t parameter, const String8 &param2);
    void        reMoveXml();
	void 		setProtectFileSpan(camera_id CameraId, int seconds);

    status_t    setParametersExtra(camera_id CameraId, const String8 &key, const String8 &value);

    friend void *RecorderMgrFileThread(void *pData);

	virtual void onFirstRef();
	void 		onMessageReceived(const sp<AMessage> &msg);
#if MTK_CARCORD_NFY_DATA_SUPPORT 
	status_t     setVidInSourceEx(camera_id CameraId,sp<MediaSourceEx> &videoSource,bool isAudio);
    status_t     setCameraEx(camera_id CameraId, const sp<Camera>& camera);
    sp<MediaSourceEx> mVidNfyInSource[2*CAMERA_NUM];
	sp<MediaSourceEx> mAudNfyInSource[2*CAMERA_NUM];
#endif
#if MTK_CARCORD_LOW_RES_KP_SUPPORT
	status_t stopLowResMux(int32_t cameraid);
	status_t ReleaseLowResMux();
	sp<AMessage> mLowResNotify;
#endif
    status_t setOutputFileName(camera_id CameraId, const char* fileName);


protected:
#if MTK_CARCORD_NFY_DATA_SUPPORT 
	class OutTrack;//zhen
#endif

public:
	status_t                constructSDcardFiles();
	pthread_t               mRecMrgConstructSDThrd;
	Condition               mConstructSDThread;
	bool                    mConstructSDThreadExist;
	Mutex                   mConstructSDLock;
private:
	
	enum {
        kWhatRecordInit                 = 'rint',
        kWhatRecordPrepare              = 'rpre',
        kWhatRecordSetOutputFile        = 'rOfl',
        kWhatRecordSetOutputFormat      = 'rOfm',
        kWhatRecordSetVideoEnc          = 'rEnc',
        kWhatRecordSetAudioEnc          = 'rAud',
        kWhatRecordSetVideoFrt          = 'rvft',
        kWhatRecordSetVideoSize         = 'rvsz',
        kWhatRecordStart                = 'rstr',
        kWhatRecordSetParam             = 'rpar',
        kWhatRecordSetVideoSource       = 'rvsr',
        kWhatRecordSetAudioSource       = 'rasr',
        kWhatRecordStop                 = 'rstp',
        kWhatRecordReset                = 'rrst',
        kWhatRecordSetCamera            = 'rscm',
        kWhatRecordSetCameraClient      = 'rscc',
        kWhatRecordCreateInst           = 'rcst',
        kWhatRecordDeleteInst           = 'rdst',
        kWhatRecordHandleXML            = 'hxml',
        kWhatRecordUpdateXML            = 'uxml',
        kWhatRecordDeleteFiles          = 'dxml',
        kWhatRecordReplaceFile          = 'rxml',
        kWhatRecordNotify               = 'rntf',
        kWhatRecordCallback             = 'rclb',
        kWhatRecordNotifyFrontPath      = 'rnfp',
        kWhatRecordNotifyBackPath       = 'rnbp',
        kWhatRecordUnlockFile           = 'rukf',
        kWhatRecordHandleProtectFiles   = 'hptf',
        #if 1//MTK_CARCORD_NFY_DATA_SUPPORT
        kWhatRecordSetSubCamVidInSrc    = 'scvs',
        #endif
		kWhatRecordMuxerProtectFiles  = 'rmpf',
		kWhatRecordGeneKPLowResFiles  = 'rlpf',//MTK_CARCORD_LOW_RES_KP_SUPPORT
		kWhatRecordStartKPLowResFiles  = 'rslf',
		kwhatRecordKPLowResNotify = 'rlsn',
        kWhatRecordRename				= 'rrnm',
        kWhatRecordCheckSdcard          ='rcsd',
    };

	enum media_protect_mode {
	    FILE_PROTECT_MODE                 		= 0,
	    KEY_POINT_PROTECT_MODE                  = 1 ,
	    #if MTK_CARCORD_LOW_RES_KP_SUPPORT
	    LOW_RES_KEY_POINT_PROTECT_MODE          = 2,//zhen MTK_CARCORD_LOW_RES_KP_SUPPORT
	    #endif
	};
	sp<AHandlerRecMgr> 		mReflector;
	sp<ALooper> 			mRecMgrLooper;
	sp<AHandlerRecMgr>      mRecMgrBrotherHandler;
	sp<ALooper> 			mRecMgrBrotherLooper;
	String8                 customizeFileName(camera_id CameraId, const char* path,const char* name);
	String8                 createFilePath(camera_id CameraId, const char* path);
    void                    doCleanUp(camera_id CameraId);
    status_t                doReset(camera_id CameraId);
	status_t                handleProtectedFiles();
	status_t                handleUnlockFile(const char *pNodeName);
	status_t    			setRecorderMgrListener(int32_t CameraId);
	status_t                HandleXmlFile(const char *pNodeName, int32_t fileDuration);
	status_t                createDefaultXML(const char *defaultXmlFile);
	status_t				updataXmlFile(const char *XmlFilePath, const char *pNodeName, int32_t fileDuration = 0, bool bProtect = false, bool bDelete = false);
	status_t                delOrReplaceFile(const char *XmlFilePath, const char *pNodeName, bool bDelete = false);
	status_t                delFileForCycleRecording();
	status_t                compareNode(char* pStr1, char* pStr2);

	bool	 				listenSDCardCap(char *s);
	bool                    hasNodeInXML();
	status_t				muxerFiles(const char *outputFileName,
							        const char *path,
							        const char *muxerPath,
							        int64_t trimStartTimeMs,
							        int64_t trimEndTimeMs);
	#if MTK_CARCORD_NFY_DATA_SUPPORT 
        StagefrightRecorderSmp      *mRecorder[2*CAMERA_NUM];
	List<OutTrack *>             mOutTracks;//zhen
	sp<InfoListener>            mInfoListener[2*CAMERA_NUM];
	#else
    StagefrightRecorderSmp      		*mRecorder[2];
	sp<InfoListener>            mInfoListener[2];
	#endif
	#if MTK_CARCORD_LOW_RES_KP_SUPPORT
	sp<DecoderMgr>                 mDecMgr;
	#endif
	volatile int32_t 			Recorder_count ;
    bool                        mBstopByuser;
    struct
    {
	   bool 			front_notify;
       bool             back_notify;
    }motion_notify;
    sp<MediaRecorderListener>   mListener;
    sp<MediaSourceEx>           mAudioSource;
	

	pthread_t					mRecMrgFileThrd;
    
	VAL_UINT32_T 				mRecMrgThrdTid;
	VAL_UINT32_T 				mRecMrgFileThrdTid;

    // Reference to IGraphicBufferProducer
    // for encoding GL Frames. That is useful only when the
    // video source is set to VIDEO_SOURCE_GRALLOC_BUFFER
    //sp<IGraphicBufferProducer>  mSurfaceMediaSource;
	struct{
	    media_recorder_states       mCurrentState;
	    bool                        mIsAudioSourceSet;
	    bool                        mIsVideoSourceSet;
	    bool                        mIsAudioEncoderSet;
	    bool                        mIsVideoEncoderSet;
	    bool                        mIsOutputFileSet;
		bool						mIsMotionDetectMode;
   	}recordStatus[2*CAMERA_NUM];//MTK_CARCORD_NFY_DATA_SUPPORT

	struct{
	    char						*prevFileName;
		char						*curFileName;
        char                        *protectFileName;
		char                        *customizeFileName;//zhen
		char                        *lockFilePath;
		uint32_t					recordFlag;
		int  						mFd;
        bool                        mProtectFlag;
        bool                        mLastProtectFileFlag;
		media_protect_mode       	fileProtectMode;
		int64_t        				muxerTimePiont;
		int64_t						mFileDuration;
		char						*keyPointFileName;
		int64_t						protectFileSpan;
		int                         mCameraID;
   	}mgrParam[2*CAMERA_NUM];//MTK_CARCORD_NFY_DATA_SUPPORT

	List<MediaBuffer * > mBufferInfo;
	
	uint32_t 					mFlags;
	//char 						mRecordFilePath[128];
	char						*mRecordFilePath;
    Mutex                       mLock;
    Mutex                       mNotifyLock;

	Condition 					mProtectFileThread;
    
	bool						mFileThreadExist;
	Mutex 						mMsgLock;
    Condition 					mMsgCondition;
	//RecorderMgr(const RecorderMgr &);
    //RecorderMgr &operator=(const RecorderMgr &);
	
	static RecorderMgr*         spSingleIns;
	int mOutputFormat;
    
	DISALLOW_EVIL_CONSTRUCTORS(RecorderMgr);

};

};  // namespace android

#endif // ANDROID_MEDIARECORDER_MGR_H
