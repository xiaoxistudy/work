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

#ifndef CAMERA_SOURCE_SMP_H_

#define CAMERA_SOURCE_SMP_H_


#include <smartrecorder/camera/CameraParametersSmp.h>
#include <stagefright/CameraSource.h>

namespace android {

class IMemory;
class Camera;
class Surface;
class CameraSource;

class CameraSourceSmp : public CameraSource {
public:
	virtual ~CameraSourceSmp();
    static CameraSourceSmp *Create(const String16 &clientName);
	static CameraSourceSmp *CreateFromCamera(const sp<ICamera> &camera,
                                          const sp<ICameraRecordingProxy> &proxy,
                                          int32_t cameraId,
                                          const String16& clientName,
                                          uid_t clientUid,
                                          Size videoSize,
                                          int32_t frameRate,
                                          const sp<IGraphicBufferProducer>& surface,
                                          bool storeMetaDataInVideoBuffers = false);
	virtual status_t start(MetaData *params = NULL);
    virtual status_t stop() { return reset(); }
	int32_t mWaterMarkWidth;
	int32_t mWaterMarkHeight;
	int32_t mlinepad;
	int32_t offsetX ; 
	int32_t offsetY;
	unsigned char *pWaterMarkData;
	unsigned char *pWaterMarkTimerData;
	int32_t mWaterMarkTimerWidth;
	int32_t mWaterMarkTimerHeight;
	int32_t offsetTimerX ; 
	int32_t offsetTimerY;
	bool  bIsShareMemory;
	bool  bNeedWaterMark;
    bool  bIsDecodeBmp;
	int   mFD;
	sp<ICamera> Icamera;
    int32_t mCameraID;
	bool mBmpDecThreadExit;
	int RGB2YUV_YR[256];
	int RGB2YUV_YG[256];
	int RGB2YUV_YB[256]; 
	int RGB2YUV_UR[256];
	int RGB2YUV_UG[256];
	int RGB2YUV_UBVR[256]; 
	int RGB2YUV_VG[256];
	int RGB2YUV_VB[256]; 
	Mutex mWaterMarkLock;

	void addWaterMarkToFrames(const sp<IMemory>& frame);
	uint8_t *  getWaterMarkdata();
	void	   initYUVTable();
	pthread_t  mBmpDecodeThread;
	bool	   mDone;
	unsigned long mCameDecodeThreadTid;
	friend void*  CamerasourceBmpdecThread(void* pData);
	status_t getWaterMarkParameters(const CameraParametersSmp& params);
	void	   addCustomerDefineDataToFrames(android_ycbcr *ycbcr,uint8_t* watermarkdatapoint, int32_t wmwidth,int32_t wmheight ,int32_t moffsetx ,int32_t moffsety);
	status_t   decodeWaterMarkDataByPath(String8 path,int32_t* w, int32_t* h,bool istimerdata);
	status_t   decodeWaterMarkData(CameraParametersSmp& mCamerasamp);
	//status_t   decodeWaterMarkData(CameraParameters& CameraParams,bool istimerdata);
	int 	   ConvertRGB2YUV(int w,int h,int32_t readcount, unsigned char *bmp,unsigned char*yuv,bool isDecodeBmp);


protected:
    class ProxyListenerSmp: public ProxyListener {
    public:
        ProxyListenerSmp(const sp<CameraSourceSmp>& source);
        virtual void dataCallbackTimestamp(int64_t timestampUs, int32_t msgType,
                const sp<IMemory> &data);

    private:
        sp<CameraSourceSmp> mSource;
    };

    CameraSourceSmp(const sp<ICamera>& camera, const sp<ICameraRecordingProxy>& proxy,
                 int32_t cameraId, const String16& clientName, uid_t clientUid,
                 Size videoSize, int32_t frameRate,
                 const sp<IGraphicBufferProducer>& surface,
                 bool storeMetaDataInVideoBuffers);
    
    virtual void dataCallbackTimestamp(int64_t timestampUs, int32_t msgType,
            const sp<IMemory> &data);
	virtual status_t startCameraRecording();
	void releaseCamera();
private:
    friend class CameraSourceListenerSmp;
	Mutex mBmpdecLock;
	status_t init(const sp<ICamera>& camera, const sp<ICameraRecordingProxy>& proxy,
                  int32_t cameraId, const String16& clientName, uid_t clientUid,
                  Size videoSize, int32_t frameRate, bool storeMetaDataInVideoBuffers);

    status_t initWithCameraAccess(
                  const sp<ICamera>& camera, const sp<ICameraRecordingProxy>& proxy,
                  int32_t cameraId, const String16& clientName, uid_t clientUid,
                  Size videoSize, int32_t frameRate, bool storeMetaDataInVideoBuffers);
	void stopCameraRecording();
	status_t reset();
    CameraSourceSmp(const CameraSourceSmp &);
    CameraSourceSmp &operator=(const CameraSourceSmp &);
	sp<IMemoryHeap> mImageHeap;

	typedef struct image_buffer
	{
		buffer_handle_t bufHdl;
		android_ycbcr   ycbcr;
	} image_buffer_t, *pimage_buffer_t;

	image_buffer_t  mImageBuffer[16];
};

}  // namespace android

#endif  // CAMERA_SOURCE_H_
