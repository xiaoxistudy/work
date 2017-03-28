/*
**
** Copyright 2008, The Android Open Source Project
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
** limitations under the License.
*/

//#define LOG_NDEBUG 0
#define LOG_TAG "ICameraClient"
#include <utils/Log.h>
#include <stdint.h>
#include <sys/types.h>
#include <camera/ICameraClient.h>
#include <camera/ICameraRecordingProxy.h>

#include <media/hardware/MetadataBufferType.h>
namespace android {

enum {
    NOTIFY_CALLBACK = IBinder::FIRST_CALL_TRANSACTION,
    DATA_CALLBACK,
    DATA_CALLBACK_TIMESTAMP,
};

class BpCameraClient: public BpInterface<ICameraClient>
{
public:
    BpCameraClient(const sp<IBinder>& impl)
        : BpInterface<ICameraClient>(impl)
    {
    }

    // generic callback from camera service to app
    void notifyCallback(int32_t msgType, int32_t ext1, int32_t ext2)
    {
        ALOGV("notifyCallback");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraClient::getInterfaceDescriptor());
        data.writeInt32(msgType);
        data.writeInt32(ext1);
        data.writeInt32(ext2);
        remote()->transact(NOTIFY_CALLBACK, data, &reply, IBinder::FLAG_ONEWAY);
    }

    // generic data callback from camera service to app with image data
    void dataCallback(int32_t msgType, const sp<IMemory>& imageData,
                      camera_frame_metadata_t *metadata)
    {
        ALOGV("dataCallback");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraClient::getInterfaceDescriptor());
        data.writeInt32(msgType);
        data.writeStrongBinder(imageData->asBinder());
        if (metadata) {
            data.writeInt32(metadata->number_of_faces);
            data.write(metadata->faces, sizeof(camera_face_t) * metadata->number_of_faces);
        }
        remote()->transact(DATA_CALLBACK, data, &reply, IBinder::FLAG_ONEWAY);
    }

    // generic data callback from camera service to app with image data
    void dataCallbackTimestamp(nsecs_t timestamp, int32_t msgType, const sp<IMemory>& imageData)
    {
        ALOGV("dataCallback");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraClient::getInterfaceDescriptor());
        data.writeInt64(timestamp);
        data.writeInt32(msgType);
        data.writeStrongBinder(imageData->asBinder());
        uint8_t *addr = (uint8_t *)imageData->pointer();
        if (*(uint32_t*)addr == kMetadataBufferTypeGrallocSource) {
            size_t *offset = reinterpret_cast<size_t *>(addr + sizeof(uint32_t) + 2 * sizeof(buffer_handle_t*));
            *offset = ICameraRecordingProxy::getCommonBaseAddress();
            //ALOGD("dataCallbackTimestamp: offset[%p]=%p", offset, *offset);

            if (msgType == CAMERA_MSG_VIDEO_FRAME) {
                buffer_handle_t *pHandle = reinterpret_cast<buffer_handle_t*>(addr + sizeof(uint32_t));
                buffer_handle_t handle = reinterpret_cast<buffer_handle_t>((uint8_t*)(*pHandle) + ICameraRecordingProxy::getCommonBaseAddress());
                //uint32_t *addr_32 = (uint32_t*)(addr);
                //ALOGD("dataCallbackTimestamp: [0]=%p [1]=%p addr: [%p]=%p [%p]=%p [%p]=%p [%p]=%p", \
                //    *pHandle, handle, &addr_32[0], addr_32[0], &addr_32[1], addr_32[1], &addr_32[2], addr_32[2], &addr_32[3], addr_32[3]);
                if(NO_ERROR != data.writeNativeHandle(handle)) {
                    ALOGE("write NativeHandle %p error", handle);
                }
            }
        }
        remote()->transact(DATA_CALLBACK_TIMESTAMP, data, &reply, IBinder::FLAG_ONEWAY);
    }
};

IMPLEMENT_META_INTERFACE(CameraClient, "android.hardware.ICameraClient");

// ----------------------------------------------------------------------

status_t BnCameraClient::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case NOTIFY_CALLBACK: {
            ALOGV("NOTIFY_CALLBACK");
            CHECK_INTERFACE(ICameraClient, data, reply);
            int32_t msgType = data.readInt32();
            int32_t ext1 = data.readInt32();
            int32_t ext2 = data.readInt32();
            notifyCallback(msgType, ext1, ext2);
            return NO_ERROR;
        } break;
        case DATA_CALLBACK: {
            ALOGV("DATA_CALLBACK");
            CHECK_INTERFACE(ICameraClient, data, reply);
            int32_t msgType = data.readInt32();
            sp<IMemory> imageData = interface_cast<IMemory>(data.readStrongBinder());
            camera_frame_metadata_t *metadata = NULL;
            if (data.dataAvail() > 0) {
                metadata = new camera_frame_metadata_t;
                metadata->number_of_faces = data.readInt32();
                metadata->faces = (camera_face_t *) data.readInplace(
                        sizeof(camera_face_t) * metadata->number_of_faces);
            }
            dataCallback(msgType, imageData, metadata);
            if (metadata) delete metadata;
            return NO_ERROR;
        } break;
        case DATA_CALLBACK_TIMESTAMP: {
            ALOGV("DATA_CALLBACK_TIMESTAMP");
            CHECK_INTERFACE(ICameraClient, data, reply);
            nsecs_t timestamp = data.readInt64();
            int32_t msgType = data.readInt32();
            sp<IMemory> imageData = interface_cast<IMemory>(data.readStrongBinder());
            if(mImageHeap == 0) {
                mImageHeap = imageData->getMemory(NULL, NULL);
            }
            uint32_t *addr = (uint32_t*)(mImageHeap->base() + imageData->offset());
            if(msgType == CAMERA_MSG_VIDEO_FRAME && addr[0] == kMetadataBufferTypeGrallocSource) {
                buffer_handle_t *phandle = reinterpret_cast<buffer_handle_t*>(&addr[2]);
                phandle[0] = data.readNativeHandle();
                ALOGV("read NativeHandle [%p] = %p, addr: %p %p %p %p", \
                    phandle, phandle[0], addr[0], addr[1], addr[2], addr[3]);
                if(0 == phandle[0]) {
                    ALOGE("read NativeHandle error");
                }
                #if 0
                Rect bounds(1280, 720);
                void *vaddr = NULL;
                GraphicBufferMapper& mapper = GraphicBufferMapper::get();
                mapper.registerBuffer(phandle[0]);
#define RECORDING_GRALLOC_USAGE     (GraphicBuffer::USAGE_SW_READ_OFTEN | GraphicBuffer::USAGE_HW_VIDEO_ENCODER)
                mapper.lock(phandle[0], RECORDING_GRALLOC_USAGE, bounds, &vaddr);
                ALOGV("Get vaddr %p", vaddr);
                mapper.unlock(phandle[0]);
                mapper.unregisterBuffer(phandle[0]);
                native_handle_close(phandle[0]);
                native_handle_delete(const_cast<native_handle_t*>(phandle[0]));
                phandle[0] = 0;
                #endif
            }
            dataCallbackTimestamp(timestamp, msgType, imageData);
            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android

