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

#ifndef MPEG2TS_SMP_WRITER_H_

#define MPEG2TS_SMP_WRITER_H_

#ifdef MTK_AOSP_ENHANCEMENT
#include <utils/String8.h>
#include <media/stagefright/MediaBuffer.h>
#endif

#include <media/stagefright/foundation/ABase.h>
#include <media/stagefright/foundation/AHandlerReflector.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/MediaWriter.h>
#include <MPEG2TSFileCacheWriterSmp.h>

#include <stdio.h>

#include <MediaWriterSmp.h>
#include <utils/List.h>
#include <utils/threads.h>


namespace android {

#define USE_FILE_CACHE
#define SD_FULL_PROTECT
#define LOW_MEM_PROTECT_THRESHOLD 	70*1024*1024LL
#define CHECK_LOW_MEM_BY_MEM_FREE
#define USE_SECTION_SAVE_FILE 


struct ABuffer;

class MediaBuffer;
class MediaSource;
class MetaData;


struct MPEG2TSWriterSmp : public MediaWriterSmp{
    MPEG2TSWriterSmp(int fd);
    MPEG2TSWriterSmp(const char *filename);

    MPEG2TSWriterSmp(
            void *cookie,
            ssize_t (*write)(void *cookie, const void *data, size_t size));

    virtual status_t addSource(const sp<MediaSource> &source);
    virtual status_t start(MetaData *param = NULL);
    virtual status_t stop();
    virtual status_t pause();
    virtual bool reachedEOS();
    virtual status_t dump(int fd, const Vector<String16>& args);
    void onMessageReceived(const sp<AMessage> &msg);

////////////////////////////////////////////////////////////////////////////
    void    writeAllSource();
    bool    exceedsVideoDurationLimit();
    bool    exceedsAudioDurationLimit();
    size_t  findSourceToWrite();
    
    status_t    createNextFd();
    String8 createFilePath();
	String8 customizeFileName();
    void    startToWriterNextFile();
    
    void    setMotionDetect(int64_t isMotionDectect,int64_t durationUs);
    void    setSDFull(bool flag){mSDFullFlag = flag;}
    bool    getSDFull(){return mSDFullFlag;}

    void    setBeforeRecordStatus(bool recordingStatus);
    void    setFirstVideoTimeUs(int64_t timeUs);
    void    getFilePath(char * filePath);
    void    setLowMemoryLimit(int lowMemoryLimit);
    void    notify2MgrForStop();
    void    notify2MgrSdcardFull();
    void    notify2MgrForSDCardDamaged();
	void    notifyBitRate(int64_t bitRate);
    bool    getMuteAudioFlag();
    bool    setmAVSync(bool bAVSync);
    void    setKeyPointNfyFileFlag(bool bKeyPointNfyFileFlag);
    void    setVideoDuration();


    bool    mAudioFileEos;
    bool    mVideoFileEos;
    int     mLowMemoryLimit;
    bool    mKeyPointNfyFileFlag;
protected:
    virtual ~MPEG2TSWriterSmp();
	
private:
    enum {
        kWhatSourceNotify = 'noti',
        kWhatRecordStop = 'rs'    
    };

    struct SourceInfo;

    //FILE *mFile;
    int  mFd;
    int  mNextFd;
    void *mWriteCookie;
    ssize_t (*mWriteFunc)(void *cookie, const void *data, size_t size);
	//friend class MPEG2TSWriterEx;
    //MPEG2TSWriterEx *mWriterEx;

    sp<ALooper> mLooper;
    sp<AHandlerReflector<MPEG2TSWriterSmp> > mReflector;

    bool mStarted;

    Vector<sp<SourceInfo> > mSources;
    size_t   mNumSourcesDone;
    int64_t  mNumTSPacketsWritten;
    int64_t  mNumTSPacketsBeforeMeta;
    int64_t  mStartTimeUs;
    int64_t  mFirstVideoTimeUs;
    int      mPATContinuityCounter;
    int      mPMTContinuityCounter;
    uint32_t mCrcTable[256];
    char     mNextFilename[MTK_CARCORD_FILE_PATH_LENGTH];
    bool     mCreateNextFd;
    bool     mSDFullFlag;
    int32_t  mSyncFrame;
#ifdef USE_FILE_CACHE
    friend class MPEG2TSFileCacheWriterSmp;
    MPEG2TSFileCacheWriterSmp* mCacheWriter;
    size_t      mWriterCacheSize;
#endif

    bool     mMotionDetectFlag;
    bool     mBeforeStartingFlag;
    bool     mMotionNotIntegerTimesFlag;
    int64_t  mMotionFileDurationLimitUs;
    int64_t  mAudioDurationAdjust;
    int32_t  mDropAudioTimes;
	
    void init();

    void writeTS();
    void writeProgramAssociationTable();
    void writeProgramMap();
    void writeAccessUnit(int32_t sourceIndex, const sp<ABuffer> &buffer);
    void initCrcTable();
    uint32_t crc32(const uint8_t *start, size_t length);

    ssize_t internalWrite(const void *data, size_t size);
    status_t reset();

    DISALLOW_EVIL_CONSTRUCTORS(MPEG2TSWriterSmp);
};

}  // namespace android

#endif  // MPEG2TS_SMP_WRITER_H_
