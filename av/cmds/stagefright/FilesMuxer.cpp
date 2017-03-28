/*
 * Copyright (C) 2013 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "filesmuxer"
#include <inttypes.h>
#include <utils/Log.h>

#include <binder/ProcessState.h>
#include <media/IMediaHTTPService.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/AString.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaCodec.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaMuxer.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/NuMediaExtractor.h>
#include <media/smartrecorder/MediaMuxerSmp.h>

static void usage(const char *me) {
    fprintf(stderr, "usage: %s [-a] [-v] [-s <trim start time>]"
                    " [-e <trim end time>] [-o <output file>]"
                    " <input video file>\n", me);
    fprintf(stderr, "       -h help\n");
    fprintf(stderr, "       -a use audio\n");
    fprintf(stderr, "       -v use video\n");
    fprintf(stderr, "       -s Time in milli-seconds when the trim should start\n");
    fprintf(stderr, "       -e Time in milli-seconds when the trim should end\n");
    fprintf(stderr, "       -o output file name. Default is /sdcard/muxeroutput.mp4\n");

    exit(1);
}

using namespace android;

static int muxing(
        const android::sp<android::ALooper> &looper,
        const char *outputFileName,
        const char *path,
        int trimStartTimeMs,
        int trimEndTimeMs,
        int rotationDegrees) {
    sp<NuMediaExtractor> extractor = new NuMediaExtractor;
    
    if (outputFileName == NULL) {
        outputFileName = "/sdcard/muxeroutput.mp4";
    }

    ALOGE("input file %s, output file %s", path, outputFileName);

    sp<MediaMuxerSmp> muxer = new MediaMuxerSmp(outputFileName, 2);

    char filename[128]="";
    KeyedVector<size_t, ssize_t> trackIndexMap;
    size_t bufferSize = 1 * 1024 * 1024;  // default buffer size is 1MB.

    int64_t trimStartTimeUs = trimStartTimeMs * 1000;
    int64_t trimEndTimeUs = trimEndTimeMs * 1000;
    
    int64_t trimOffsetTimeUs = 0;
    
    strncpy(filename, path, 128-1);
    
    bool muxerDone = false;
    bool sawInputEOS = false;
    bool haveAudio = false;
    bool haveVideo = false;
    sp<ABuffer> newBuffer = new ABuffer(bufferSize);
    
    int64_t muxerStartTimeUs = ALooper::GetNowUs();

    ALOGE("--filename %s", filename);
    if (extractor->setDataSource(NULL /* httpService */, filename) != OK) {
        fprintf(stderr, "unable to instantiate extractor. %s\n", filename);
        return 1;
    }

    size_t trackCount = extractor->countTracks();
    // Map the extractor's track index to the muxer's track index.
    for (size_t i = 0; i < trackCount; ++i) {
        sp<AMessage> format;
        status_t err = extractor->getTrackFormat(i, &format);
        CHECK_EQ(err, (status_t)OK);
        ALOGE("extractor getTrackFormat: %s", format->debugString().c_str());

        AString mime;
        CHECK(format->findString("mime", &mime));

        bool isAudio = !strncasecmp(mime.c_str(), "audio/", 6);
        bool isVideo = !strncasecmp(mime.c_str(), "video/", 6);

        if (!haveAudio && isAudio) {
            haveAudio = true;
        } else if (!haveVideo && isVideo) {
            haveVideo = true;
        } else {
            continue;
        }

        if (isVideo) {
            int width , height;
            CHECK(format->findInt32("width", &width));
            CHECK(format->findInt32("height", &height));
            bufferSize = width * height * 4;  // Assuming it is maximally 4BPP
        }

        int64_t duration;
        CHECK(format->findInt64("durationUs", &duration));

        // Since we got the duration now, correct the start time.
        if (trimStartTimeUs > duration) {
            fprintf(stderr, "Warning: trimStartTimeUs > duration,"
                            " reset to 0\n");
            trimStartTimeUs = 0;
        }

        ALOGE("selecting track %d", i);

        err = extractor->selectTrack(i);
        CHECK_EQ(err, (status_t)OK);

        ssize_t newTrackIndex = muxer->addTrack(format);
        CHECK_GE(newTrackIndex, 0);
        trackIndexMap.add(i, newTrackIndex);
    }

    muxer->setOrientationHint(rotationDegrees);
    muxer->start();
    
    while(!muxerDone){
        size_t trackIndex = -1;
        bool trimStarted = false;
        bool bSeek = true;
        int64_t timeUs = -1;

        //muxer->start();
        if (sawInputEOS){
            for (size_t i = 0; i < trackCount; ++i) {
                status_t err = extractor->unselectTrack(i);
                CHECK_EQ(err, (status_t)OK);
            }
            memset(filename,0,128);
            strncpy(filename, "/sdcard/muxerfiles.mp4", 128-1);
            ALOGE("--filename %s", filename);
            if (extractor->setDataSource(NULL /* httpService */, filename) != OK) {
                fprintf(stderr, "unable to instantiate extractor. %s\n", filename);
                return 1;
            }
            for (size_t i = 0; i < trackCount; ++i) {
                status_t err = extractor->selectTrack(i);
                CHECK_EQ(err, (status_t)OK);
            }
            muxerDone = true;
            sawInputEOS = false;
        }
        
        while (!sawInputEOS) {
            
            status_t err = extractor->getSampleTrackIndex(&trackIndex);
            if (err != OK) {
                ALOGE("saw input eos, err %d", err);
                sawInputEOS = true;
                muxer->setEndTimestampUs(true, timeUs-trimOffsetTimeUs);
                ALOGE("input eos, setEndTimestampUs:%lld", timeUs-trimOffsetTimeUs);
                break;
            } else {
                err = extractor->readSampleData(newBuffer, bSeek, trimStartTimeUs);
                CHECK_EQ(err, (status_t)OK);
                bSeek = false;
                err = extractor->getSampleTime(&timeUs);
                CHECK_EQ(err, (status_t)OK);
                ALOGE("--timeUs: %lld,track:%d", timeUs,trackIndex);

                sp<MetaData> meta;
                err = extractor->getSampleMeta(&meta);
                CHECK_EQ(err, (status_t)OK);

                uint32_t sampleFlags = 0;
                int32_t val;
                if (1)//meta->findInt32(kKeyIsSyncFrame, &val) && val != 0) 
                {
                    // We only support BUFFER_FLAG_SYNCFRAME in the flag for now.
                    sampleFlags |= MediaCodec::BUFFER_FLAG_SYNCFRAME;

                    // We turn on trimming at the sync frame.
                    if (timeUs > trimStartTimeUs) {
                        if (trimStarted == false) {
                            trimOffsetTimeUs = timeUs;
                        }
                        trimStarted = true;
                    }
                }
                // Trim can end at any non-sync frame.
                if (0 >= trimEndTimeUs) {
                    //trimStarted = true;
                }

                if (trimStarted) {
                    ALOGE("writer sample--timeUs: %lld, %lld", timeUs, trimOffsetTimeUs);
                    err = muxer->writeSampleData(newBuffer,
                                                 trackIndexMap.valueFor(trackIndex),
                                                 timeUs - trimOffsetTimeUs, sampleFlags);
                }

                extractor->advance();
            }
        }
        //muxer->stop();
    }
    muxer->setEndTimestampUs(true, 0);
    muxer->stop();
    newBuffer.clear();
    trackIndexMap.clear();

    int64_t elapsedTimeUs = ALooper::GetNowUs() - muxerStartTimeUs;
    fprintf(stderr, "SUCCESS: muxer generate the video in %" PRId64 " ms\n",
            elapsedTimeUs / 1000);

    return 0;
}

int main(int argc, char **argv) {
    const char *me = argv[0];

    bool useAudio = false;
    bool useVideo = false;
    char *outputFileName = NULL;
    int trimStartTimeMs = -1;
    int trimEndTimeMs = -1;
    int rotationDegrees = 0;
    // When trimStartTimeMs and trimEndTimeMs seems valid, we turn this switch
    // to true.
    bool enableTrim = false;

    int res;
    while ((res = getopt(argc, argv, "h?avo:s:e:r:")) >= 0) {
        switch (res) {
            case 'a':
            {
                useAudio = true;
                break;
            }

            case 'v':
            {
                useVideo = true;
                break;
            }

            case 'o':
            {
                outputFileName = optarg;
                break;
            }

            case 's':
            {
                trimStartTimeMs = atoi(optarg);
                break;
            }

            case 'e':
            {
                trimEndTimeMs = atoi(optarg);
                break;
            }

            case 'r':
            {
                rotationDegrees = atoi(optarg);
                break;
            }

            case '?':
            case 'h':
            default:
            {
                usage(me);
            }
        }
    }

    argc -= optind;
    argv += optind;

    if (argc != 1) {
        usage(me);
    }

    ProcessState::self()->startThreadPool();

    // Make sure setDataSource() works.
    DataSource::RegisterDefaultSniffers();

    sp<ALooper> looper = new ALooper;
    looper->start();

    int result = muxing(looper, outputFileName, argv[0],
                         25000, trimEndTimeMs, rotationDegrees);

    looper->stop();

    return result;
}
