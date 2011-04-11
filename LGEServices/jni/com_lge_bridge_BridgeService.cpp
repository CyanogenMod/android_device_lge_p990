/*
** Copyright 2007, The Android Open Source Project
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

#define LOG_TAG "BridgeServiceJNI"

#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <utils/threads.h>
#include <utils/String16.h>
#include <utils/Log.h>
#include <binder/Parcel.h>
#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include <linux/rtc.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <dirent.h>
#include <ctype.h>

// ----------------------------------------------------------------------------

using namespace android;

// ----------------------------------------------------------------------------

static char ok[3] = "ok";
static char error[6] = "error";

static int
mktime(const unsigned int year0, const unsigned int mon0,
       const unsigned int day, const unsigned int hour,
       const unsigned int min, const unsigned int sec)
{
	unsigned int mon = mon0, year = year0;

	/* 1..12 -> 11,12,1..10 */
	if (0 >= (int) (mon -= 2)) {
		mon += 12;	/* Puts Feb last since it has leap day */
		year -= 1;
	}

	return ((((int)
		  (year/4 - year/100 + year/400 + 367*mon/12 + day) +
		  year*365 - 719499
	    )*24 + hour /* now have hours */
	  )*60 + min /* now have minutes */
	)*60 + sec; /* finally seconds */
}

static int rtc_tm_to_time(struct rtc_time *tm, int *time)
{
	*time = mktime(tm->tm_year + 1900, tm->tm_mon + 1, tm->tm_mday,
			tm->tm_hour, tm->tm_min, tm->tm_sec);
	return 0;
}

static char* getCurrentTime()
{
    int time = 0;
    int fd;
    struct rtc_time rtc_tm;
    static char time_str[32];

    fd = open("/dev/rtc0", O_RDONLY);

    if (fd < 0)
    {
        LOGE("getCurrentTime(): /dev/rtc0 open error [%s]", strerror(errno));
        return error;
    }

    if (ioctl(fd, RTC_RD_TIME, &rtc_tm) < 0)
    {
        LOGE("getCurrentTime(): RTC_RD_TIME error");
        close(fd);
        return error;
    }

    rtc_tm_to_time(&rtc_tm, &time);

    close(fd);

    memset(time_str, 0, 32);
    sprintf(time_str, "%c%d%c", (char)0x02, time, (char)0x03);
    LOGD("getCurrentTime(): time = [%d], time_str = [%s]", time, time_str);

    return time_str;
}

#define MISC_PAGE_SIZE 2048
#define MISC_FACTORY_PAGE 7
/*
    0, 1, factory mode, AT%QEM
    1, 1, device test result, AT%DEVICETEST
    2, 240, device test result (for echo item), All Auto Test
    242, 1, factory reset status, AT%FRSTSTATUS
*/
#define MISC_DRM_PAGE 8
/*
    0, 160, divx drm memory
*/
#define MISC_SECURECLOCK_PAGE 9
/*
   0, 1, oma status
   32, 32, oma time
   64, 32, oma offset
   --
   128, 1, ms status
   160, 32, ms time
   192, 32, ms offset
   --
   256, 1, nitz status
   288, 16, nitz time
   320, 16, nitz offset
*/

static int read_misc_area(int offset, int size, char *buf)
{
    int read_size;
    int fd;

    fd = open("/dev/block/mmcblk0p3", O_RDWR);
    if (fd < 0) goto fail;

    lseek64(fd, offset, SEEK_SET);

    read_size = read(fd, buf, size);
    if (read_size != size) goto fail;

    close(fd);

    return 1;

fail:
    close(fd);

    return 0;
}

static int write_misc_area(int offset, int size, char *buf)
{
    int write_size;
    int fd;

    fd = open("/dev/block/mmcblk0p3", O_RDWR);
    if (fd < 0) goto fail;

    lseek64(fd, offset, SEEK_SET);

    write_size = write(fd, buf, size);
    if (write_size != size) goto fail;
    sync();

    close(fd);

    return 1;

fail:
    close(fd);

    return 0;
}

static char* readDrmMemory()
{
    static char memory[161];

    memset(memory, 0, 161);
    if (!read_misc_area(MISC_PAGE_SIZE * MISC_DRM_PAGE, 160, memory)) return error;

    LOGD("readDrmMemory(): memory[%d][%s]", strlen(memory), memory);

    return memory;
}

static char* writeDrmMemory(char *memory)
{
    LOGD("writeDrmMemory(): memory[%d][%s]", strlen(memory), memory);

    if (write_misc_area(MISC_PAGE_SIZE * MISC_DRM_PAGE, 160, memory)) return ok;
    else return error;
}

static char* getFactoryMode()
{
    static char factory_mode[2];

    memset(factory_mode, 0, 2);
    if (!read_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE, 1, factory_mode)) return error;

    LOGD("getFactoryMode(): factory_mode[%d][%s]", strlen(factory_mode), factory_mode);

    if (factory_mode[0] != '1') factory_mode[0] = '0';

    return factory_mode;
}

static char* setFactoryMode(char *factory_mode)
{
    LOGD("setFactoryMode(): factory_mode[%d][%s]", strlen(factory_mode), factory_mode);

    if (factory_mode == NULL || strlen(factory_mode) != 1 || (factory_mode[0] != '0' && factory_mode[0] != '1')) return error;

    if (write_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE, 1, factory_mode)) return ok;
    else return error;
}

static char* getDeviceTestResultDetail()
{
    static char device_test_result[241];

    memset(device_test_result, 0, 241);
    if (!read_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE + 2, 240, device_test_result)) return error;

    LOGD("getDeviceTestResultDetail(): device_test_result[%d][%s]", strlen(device_test_result), device_test_result);

    return device_test_result;
}

static char* setDeviceTestResultDetail(char *device_test_result)
{
    LOGD("setDeviceTestResultDetail(): device_test_result[%d][%s]", strlen(device_test_result), device_test_result);

    if (device_test_result == NULL || strlen(device_test_result) != 240) return error;

    if (write_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE + 2, 240, device_test_result)) return ok;
    else return error;
}

static char* getDeviceTestResult()
{
    static char device_test_result[2];

    memset(device_test_result, 0, 2);
    if (!read_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE + 1, 1, device_test_result)) return error;

    LOGD("getDeviceTestResult(): device_test_result[%d][%s]", strlen(device_test_result), device_test_result);

    if (device_test_result[0] != '1' && device_test_result[0] != '0') return error; // not tested yet

    return device_test_result;
}

static char* setDeviceTestResult(char *device_test_result)
{
    LOGD("setDeviceTestResult(): device_test_result[%d][%s]", strlen(device_test_result), device_test_result);

    if (device_test_result == NULL || strlen(device_test_result) != 1 || (device_test_result[0] != '0' && device_test_result[0] != '1')) return error;

    if (write_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE + 1, 1, device_test_result)) return ok;
    else return error;
}

static char* readDir(char *dir_path)
{
    struct dirent *entry;
    DIR *dir = opendir(dir_path);

    if (dir == NULL)
    {
        LOGE("readDir(): dir_path[%s] exception[%s]", dir_path, strerror(errno));
        return error;
    }

    LOGI("readDir(): dir_path[%s]", dir_path);
    while ((entry = readdir(dir)) != NULL)
    {
        LOGI("readDir(): d_name[%s] d_type[%d] d_reclen[%d] d_off[%lld] d_ino[%lld]",
                            entry->d_name, entry->d_type, entry->d_reclen, entry->d_off, entry->d_ino);
    }

    return ok;
}

static char* setFrstStatus(char *frst_status)
{
    if (frst_status != NULL && frst_status[0] >= '0' && frst_status[0] <= '6')
    {
        if (write_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE + 242, 1, frst_status)) return ok;
        else return error;
    }
    else
    {
        return error;
    }
}

static char* getFrstStatus()
{
    static char frst_status[2];

    memset(frst_status, 0, 2);
    if (!read_misc_area(MISC_PAGE_SIZE * MISC_FACTORY_PAGE + 242, 1, frst_status)) return error;

    if (!(frst_status[0] >= '1' && frst_status[0] <= '6')) frst_status[0] = '0'; // if not set

    return frst_status;
}

static int getSecureClockArea(char *event, int *offset, int *length)
{
    int base = MISC_PAGE_SIZE * MISC_SECURECLOCK_PAGE;

    LOGD("getSecureClockArea(): event[%s]", event);

    if (event[3] == '1') base += 0; // oma
    else if (event[3] == '2') base += 128; // ms
    else if (event[3] == '3') base += 256; // nitz
    else goto error;

    if (event[0] == '3' || event[0] == '4') { *offset = base + 0, *length = 1; } // status
    else if (event[0] == '5' || event[0] == '6') { *offset = base + 32, *length = 32; } // time
    else if (event[0] == '1' || event[0] == '2') { *offset = base + 64, *length = 32; } // offset
    else goto error;

    LOGD("getSecureClockArea(): offset[%d], length[%d]", *offset, *length);
    return 1;

error:
    return 0;
}

static char* getSecureClockValue(char *event) // event
{
    static char value[33];
    char buf[32];
    int offset;
    int length;

    if (!getSecureClockArea(event, &offset, &length)) return error;

    memset(value, 0, 33);
    if (!read_misc_area(offset, length, value)) return error;

    if ((event[0] == '3' || event[0] == '4') && strcmp(value, "1") != 0) // for uninitialized status
    {
        strcpy(value, "0");
        write_misc_area(offset, length, value);
    }

    LOGD("getSecureClockValue(): event[%s], value[%s]", event, value);

    return value;
}

static char* setSmplCount(char *smpl_count)
{
    char buf[33];

    if (smpl_count != NULL)
    {
        int length = strlen(smpl_count);

        memset(buf, 0, 33);
        memcpy(buf, smpl_count, length > 32 ? 32 : length);
        if (write_misc_area(0x1060, 32, buf)) return ok;
        else return error;
    }
    else
    {
        return error;
    }
}

static char* getSmplCount()
{
    static char smpl_count[33];

    memset(smpl_count, 0, 33);
    if (!read_misc_area(0x1060, 32, smpl_count)) return error;

    if (!isdigit((int)smpl_count[0]))
    {
        strcpy(smpl_count, "0"); // if not initialized
        setSmplCount("0");
    }

    return smpl_count;
}

static char* setSecureClockValue(char *args) // event,value
{
    char buf[32];
    char event[9];
    char value[33];
    char *ptr;
    int offset;
    int length;

    strcpy(buf, args);
    memset(event, 0, 8);
    memset(value, 0, 33);

    ptr = strtok(buf, ",");
    if (ptr == NULL) return error;
    strncpy(event, ptr, 8);
    LOGD("setSecureClockValue(): event[%s]", event);

    ptr = strtok(NULL, ",");
    if (ptr == NULL) return error;
    strncpy(value, ptr, 32);
    LOGD("setSecureClockValue(): value[%s]", value);

    if (!getSecureClockArea(event, &offset, &length)) return error;
    if (!write_misc_area(offset, length, value)) return error;

    return ok;
}

static jstring sendNativeCommand(JNIEnv *env, jobject thiz, jstring _command)
{
    String16 error_str("error");

    if (_command == NULL)
    {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return env->NewString((const jchar *)error_str.string(), error_str.size());
    }

    const char *command = env->GetStringUTFChars(_command, NULL);

    if (command == NULL)
    {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return env->NewString((const jchar *)error_str.string(), error_str.size());
    }

    LOGD("sendNativeCommand(): command[%s]", command);

    char *ret_val = "error";

    if (strcmp(command, "readDrmMemory") == 0)
        ret_val = readDrmMemory();
    else if (strncmp(command, "writeDrmMemory", strlen("writeDrmMemory")) == 0)
        ret_val = writeDrmMemory((char *)(command + strlen("writeDrmMemory") + 1));

    else if (strcmp(command, "getFactoryMode") == 0)
        ret_val = getFactoryMode();
    else if (strncmp(command, "setFactoryMode", strlen("setFactoryMode")) == 0)
        ret_val = setFactoryMode((char *)(command + strlen("setFactoryMode") + 1));

    else if (strcmp(command, "getDeviceTestResultDetail") == 0)
        ret_val = getDeviceTestResultDetail();
    else if (strncmp(command, "setDeviceTestResultDetail", strlen("setDeviceTestResultDetail")) == 0)
        ret_val = setDeviceTestResultDetail((char *)(command + strlen("setDeviceTestResultDetail") + 1));

    else if (strcmp(command, "getDeviceTestResult") == 0)
        ret_val = getDeviceTestResult();
    else if (strncmp(command, "setDeviceTestResult", strlen("setDeviceTestResult")) == 0)
        ret_val = setDeviceTestResult((char *)(command + strlen("setDeviceTestResult") + 1));

    else if (strcmp(command, "getCurrentTime") == 0)
        ret_val = getCurrentTime();

    else if (strncmp(command, "readDir", strlen("readDir")) == 0)
        ret_val = readDir((char *)(command + strlen("readDir") + 1));

    else if (strcmp(command, "getFrstStatus") == 0)
        ret_val = getFrstStatus();
    else if (strncmp(command, "setFrstStatus", strlen("setFrstStatus")) == 0)
        ret_val = setFrstStatus((char *)(command + strlen("setFrstStatus") + 1));

    else if (strncmp(command, "getSecureClockValue", strlen("getSecureClockValue")) == 0)
        ret_val = getSecureClockValue((char *)(command + strlen("getSecureClockValue") + 1));
    else if (strncmp(command, "setSecureClockValue", strlen("setSecureClockValue")) == 0)
        ret_val = setSecureClockValue((char *)(command + strlen("setSecureClockValue") + 1));

    else if (strcmp(command, "getSmplCount") == 0)
        ret_val = getSmplCount();
    else if (strncmp(command, "setSmplCount", strlen("setSmplCount")) == 0)
        ret_val = setSmplCount((char *)(command + strlen("setSmplCount") + 1));

    LOGD("sendNativeCommand(): returns[%d][%s]", strlen(ret_val), ret_val);

    String16 ret_str(ret_val);
    return env->NewString((const jchar *)ret_str.string(), ret_str.size());
}

// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] =
{
    { "sendNativeCommandInternal", "(Ljava/lang/String;)Ljava/lang/String;", (void *)sendNativeCommand }
};

static const char* const kClassPathName = "android/media/MediaPlayer";

// This function only registers the native methods
static int register_com_lge_bridge_BridgeService(JNIEnv *env)
{
    return AndroidRuntime::registerNativeMethods(env,
                "com/lge/bridge/BridgeService", gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (register_com_lge_bridge_BridgeService(env) < 0) {
        LOGE("ERROR: BridgeService native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
