/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef ISECURECLOCKSERVICE_H
#define ISECURECLOCKSERVICE_H

#include <binder/IInterface.h>
#include <utils/String16.h>

namespace android 
{

class ISecureClockService : public IInterface
{
public:
    DECLARE_META_INTERFACE(SecureClockService);

    virtual int GetTime(int sid, int CurTime) = 0; // return < 0 means error
    virtual int SetTime(int sid, int CurTime, int SecureTime) = 0; // return < 0 means error
    virtual int GetStatus(int sid) = 0; // return < 0 means error
    virtual int SetStatus(int sid, int Status) = 0; // return < 0 means error
    virtual void RtcChanged(int Offset) = 0;

    virtual const char* ReadDrmMemory() = 0;
    virtual const char* ReadHwKey() = 0;
    virtual int WriteDrmMemory(const char* memory) = 0; // return 0 means error
};

}; // namespace android

#endif // ISECURECLOCKSERVICE_H
