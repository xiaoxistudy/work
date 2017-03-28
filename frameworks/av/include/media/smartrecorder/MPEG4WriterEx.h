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

#ifndef MPEG4_WRITEREX_H_

#define MPEG4_WRITEREX_H_

#include <stdio.h>

#include <smartrecorder/MediaWriterSmp.h>
#include <utils/List.h>
#include <utils/threads.h>

#ifdef MTK_AOSP_ENHANCEMENT
#include <utils/String8.h>
#include <media/stagefright/MediaBuffer.h>
#endif

#define USE_SECTION_SAVE_FILE

#ifdef USE_SECTION_SAVE_FILE
namespace android {

#ifdef MTK_AOSP_ENHANCEMENT

#define USE_FILE_CACHE_EX
#define SD_FULL_PROTECT_EX
#define LOW_MEM_PROTECT_THRESHOLD_EX 	70*1024*1024LL
#define CHECK_LOW_MEM_BY_MEM_FREE_EX


//make the method to record the  start time offset of two tracks optional
//#define WRITER_ENABLE_EDTS_BOX
#ifdef USE_FILE_CACHE_EX
class MPEG4FileCacheWriter;
class MPEG4FileCacheWriterEx;
#endif
#endif

class MediaBuffer;
class MediaSource;
class MetaData;

class MPEG4WriterEx {
public:
	MPEG4WriterEx();
    MPEG4WriterEx(int fd,int64_t maxFileSize,int64_t minStreamFileSize,int metaheaderRevertByte);
	void setOwner(MPEG4WriterSmp *owner);
    //virtual status_t start_ex(MetaData *param = NULL);
    virtual status_t start_ex();
	virtual ~MPEG4WriterEx();
	bool needCreateNextMfd();
	String8 createFilePath();
	String8 customizeFileName();
	void release_ex();
	status_t reset_ex();
	void     instance_ex();
	void     create_next_fd();
    void  close_next_fd();
	void     init_ex();
	//MetaData * mMeta;
	int32_t mfileType;
	//bool kinitAudioTrackPrameter;
	//bool kinitVideoTrackPrameter;
	//bool kbreakAudioTrack;
	//bool kbreakVideoTrack;
	
	char* mNextFilename;
	//char* mCurrentFilename;
protected:
	
private:
    bool have_create_next_mfd;
	bool have_reset;
	int  next_mfd;
	int  current_mfd;
    MPEG4WriterEx(const MPEG4WriterEx &);
    MPEG4WriterEx &operator=(const MPEG4WriterEx &);
	void writeFtypBoxEx(int32_t fileType);
	MPEG4WriterSmp* mOwner;
	//char* mNextFilename;
	//char* mCurrentFilename;
};

}  // namespace android
#endif
#endif  // MPEG4_WRITER_H_

