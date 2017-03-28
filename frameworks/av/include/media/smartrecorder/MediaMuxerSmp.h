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

#ifndef MEDIA_MUXERSMP_H_
#define MEDIA_MUXERSMP_H_

#include <utils/Errors.h>
#include <utils/RefBase.h>
#include <utils/Vector.h>
#include <utils/threads.h>
#include <stagefright/MediaMuxer.h>
#include <stagefright/MediaWriter.h>
#include <MPEG4WriterSmp.h>
#include <MPEG2TSWriterSmp.h>

namespace android {

struct MediaMuxerSmp : public MediaMuxer {
public:
    // Construct the muxer with the output file path.
    MediaMuxerSmp(const char *path, int format);

    // Construct the muxer with the file descriptor. Note that the MediaMuxer
    // will close this file at stop().
    MediaMuxerSmp(int fd);

    virtual ~MediaMuxerSmp();

	status_t clearTrackInfo();

	void setEndTimestampUs(bool bEos, int64_t endTimestampUs);

	status_t startMuxerFiles(const char *path,
					        const char *muxerPath,
					        int64_t trimStartTimeMs,
					        int64_t trimEndTimeMs);
    
    status_t startMPEG4MuxerFiles(const char *path,
                             const char *muxerPath,
                             int64_t trimStartTimeMs,
                             int64_t trimEndTimeMs);
    
    
    status_t startTSMuxerFiles(const char *path,
                             const char *muxerPath,
                             int64_t trimStartTimeMs,
                             int64_t trimEndTimeMs);
    
    status_t writeDataFromOffset(const char *path);

    MediaMuxerSmp(const MediaMuxerSmp &);
    MediaMuxerSmp &operator=(const MediaMuxerSmp &);
    int   getFileCreateTime(const char* fileName);
    void  setFileSpan(int64_t fileSpan);

	//DISALLOW_EVIL_CONSTRUCTORS(MediaMuxerSmp);
private:
    char* mSrcFilePath;
    char* mDesFilePath;
    long  mOffset;
    int64_t   mFileSpan;
};

}  // namespace android

#endif  // MEDIA_MUXERSMP_H_

