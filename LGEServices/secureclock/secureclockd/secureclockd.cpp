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

#include <stdio.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/types.h>

#include <private/android_filesystem_config.h>
#include <secureclock/SecureClock.h>

#include <utils/Log.h>
#include <linux/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <errno.h>

#undef LOG_TAG
#define LOG_TAG "secureclockd"

int main(int argc, char **argv)
{
    long offset = 0;
    unsigned long diff_time = 0;
    int result = 0;
    int fd = 0;

    fd = open("/dev/drm", O_RDWR);
    if (fd < 0) goto fail;

    while (1)
    {
        result = ioctl(fd, 0, &diff_time); // ioctl to wait rtc event & get difference

        LOGD("/dev/drm ioctl returns[%d]", result);

        if (result == 0) offset = -diff_time;
        else if (result == 1) offset = diff_time;
        else goto fail;

        LOGD("RTC change event occur: offset = %ld", offset);

        // gunwoo1.kim@lge.com, fix WBT#170373 [START]
        if (!SecureClock_RtcChanged(offset)) { close(fd); goto fail; }
        // gunwoo1.kim@lge.com, fix WBT#170373 [END]
    }

    return 0;

fail:
    LOGE("exception occurs[%s], all secure clock invalidated", strerror(errno));

    OMADRM_SecureClock_SetStatus(0);
    OMADRM_SecureClock_SetNtpStatus(0);
    MSDRM_SecureClock_SetStatus(0);

    return 1;
}
