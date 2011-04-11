/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <stdint.h>
#include <sys/types.h>

#include <binder/Parcel.h>

#include <utils/Log.h>
#include <utils/String8.h>

#include <secureclock/ISecureClockService.h>

#undef LOG_TAG
#define LOG_TAG "ISecureClockService"

namespace android
{

enum
{
    GET_TIME = IBinder::FIRST_CALL_TRANSACTION,
    SET_TIME,
    GET_STATUS,
    SET_STATUS,
    RTC_CHANGED,
    READ_DRM_MEM,
    READ_HW_KEY,
    WRITE_DRM_MEM
};

class BpSecureClockService : public BpInterface<ISecureClockService>
{

public:

    BpSecureClockService(const sp<IBinder>& impl)
        : BpInterface<ISecureClockService>(impl)
    {
    }

    // sid means SecureClock ID (OMA = 0, MS = 1, NTP = 2)

    virtual int GetTime(int sid, int CurTime)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        data.writeInt32(sid);
        data.writeInt32(CurTime);
        remote()->transact(GET_TIME, data, &reply);

        reply.readInt32();
        return reply.readInt32();
    }

    virtual int SetTime(int sid, int CurTime, int SecureTime)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        data.writeInt32(sid);
        data.writeInt32(CurTime);
        data.writeInt32(SecureTime);
        remote()->transact(SET_TIME, data, &reply);

        reply.readInt32();
        return reply.readInt32();
    }

    virtual int GetStatus(int sid)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        data.writeInt32(sid);
        remote()->transact(GET_STATUS, data, &reply);

        reply.readInt32();
        return reply.readInt32();
    }

    virtual int SetStatus(int sid, int Status)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        data.writeInt32(sid);
        data.writeInt32(Status);
        remote()->transact(SET_STATUS, data, &reply);

        reply.readInt32();
        return reply.readInt32();
    }

    virtual void RtcChanged(int Offset)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        data.writeInt32(Offset);
        remote()->transact(RTC_CHANGED, data, &reply);
    }

    virtual const char* ReadDrmMemory()
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        remote()->transact(READ_DRM_MEM, data, &reply);

        reply.readInt32();
        return String8(reply.readString16()).string();
    }

    virtual const char* ReadHwKey()
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        remote()->transact(READ_HW_KEY, data, &reply);

        reply.readInt32();
        return String8(reply.readString16()).string();
    }

    virtual int WriteDrmMemory(const char* memory)
    {
        if (memory == NULL) return 0;

        Parcel data, reply;
        data.writeInterfaceToken(ISecureClockService::getInterfaceDescriptor());
        data.writeString16(String16(memory));
        remote()->transact(WRITE_DRM_MEM, data, &reply);

        reply.readInt32();
        return reply.readInt32();
    }
}; // class BpSecureClockService

IMPLEMENT_META_INTERFACE(SecureClockService, "com.lge.secureclock.ISecureClockService");

}; // namespace android
