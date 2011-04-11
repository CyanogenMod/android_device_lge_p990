#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <linux/rtc.h>

#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <binder/Parcel.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <binder/IPCThreadState.h>
#include <utils/TextOutput.h>

#include <secureclock/ISecureClockService.h>
#include <secureclock/SecureClock.h>

#include <private/android_filesystem_config.h>

#include <utils/Log.h>

#undef LOG_TAG
#define LOG_TAG "libsecureclock"

#define MISC_IO

#ifdef MISC_IO
#include <Bridge.h>
#endif

namespace android
{

static sp<ISecureClockService> gService;

static bool init()
{
    if (gService != NULL) return true;

    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> binder = sm->getService(String16("secureclock"));
    gService = interface_cast<ISecureClockService>(binder);

    if (gService != NULL) return true;
    else return false;
}

}; // namespace android

#define LEAPS_THRU_END_OF(y) ((y)/4 - (y)/100 + (y)/400)
#define LEAP_YEAR(year) ((!(year % 4) && (year % 100)) || !(year % 400))

static const unsigned char rtc_days_in_month[] =
{
	31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
};

static const unsigned short rtc_ydays[2][13] =
{
	/* Normal years */
	{ 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365 },
	/* Leap years */
	{ 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366 }
};

static int rtc_year_days(unsigned int day, unsigned int month, unsigned int year)
{
	return rtc_ydays[LEAP_YEAR(year)][month] + day-1;
}

static int rtc_month_days(unsigned int month, unsigned int year)
{
	return rtc_days_in_month[month] + (LEAP_YEAR(year) && month == 1);
}

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

static void rtc_time_to_tm(int time, struct rtc_time *tm)
{
	unsigned int month, year;
	int days;

	days = time / 86400;
	time -= (unsigned int) days * 86400;

	/* day of the week, 1970-01-01 was a Thursday */
	tm->tm_wday = (days + 4) % 7;

	year = 1970 + days / 365;
	days -= (year - 1970) * 365
		+ LEAPS_THRU_END_OF(year - 1)
		- LEAPS_THRU_END_OF(1970 - 1);
	if (days < 0) {
		year -= 1;
		days += 365 + LEAP_YEAR(year);
	}
	tm->tm_year = year - 1900;
	tm->tm_yday = days + 1;

	for (month = 0; month < 11; month++) {
		int newdays;

		newdays = days - rtc_month_days(month, year);
		if (newdays < 0)
			break;
		days = newdays;
	}
	tm->tm_mon = month;
	tm->tm_mday = days + 1;

	tm->tm_hour = time / 3600;
	time -= tm->tm_hour * 3600;
	tm->tm_min = time / 60;
	tm->tm_sec = time - tm->tm_min * 60;
}

static int rtc_tm_to_time(struct rtc_time *tm, int *time)
{
	*time = mktime(tm->tm_year + 1900, tm->tm_mon + 1, tm->tm_mday,
			tm->tm_hour, tm->tm_min, tm->tm_sec);
	return 0;
}

static void rtc_time_to_lg_tm(int time, lgoem_dev_rtc_type *lg_tm)
{
    struct rtc_time tm;

    rtc_time_to_tm(time, &tm);

    lg_tm->Year = tm.tm_year + 1900;
    lg_tm->Month = tm.tm_mon + 1;
    lg_tm->Day = tm.tm_mday;
    lg_tm->Hour = tm.tm_hour;
    lg_tm->Minute = tm.tm_min;
    lg_tm->Second = tm.tm_sec;
}

static void rtc_lg_tm_to_time(lgoem_dev_rtc_type *lg_tm, int *time)
{
    struct rtc_time tm;

    tm.tm_year = lg_tm->Year - 1900;
    tm.tm_mon = lg_tm->Month - 1;
    tm.tm_mday = lg_tm->Day;
    tm.tm_hour = lg_tm->Hour;
    tm.tm_min = lg_tm->Minute;
    tm.tm_sec = lg_tm->Second;

    rtc_tm_to_time(&tm, time);
}

static bool getCurrentTime(int *time)
{
    int fd;
    struct rtc_time rtc_tm;

    fd = open("/dev/rtc0", O_RDONLY);

    if (fd < 0)
    {
        LOGE("getCurrentTime(): /dev/rtc0 open error");
        return false;
    }

    if (ioctl(fd, RTC_RD_TIME, &rtc_tm) < 0)
    {
        LOGE("getCurrentTime(): RTC_RD_TIME error");
        close(fd);
        return false;
    }

    rtc_tm_to_time(&rtc_tm, time);
    LOGD("getCurrentTime(): *time = %d", *time);

    close(fd);

    return true;
}

static bool checkPriv()
{
    if (getuid() == AID_SYSTEM && getgid() == AID_SYSTEM) return true; // used by secureclockd


//20100924 bokyung.kim@lge.com PORTING SKT DRM
#ifdef ANDY_LGDRM
    if ( getgid() == AID_LGDRM_ACC   )
            return true;
    else
        return false;
#endif

        return false;
}

/********** GetTime **********/

static int SecureClock_GetTime(int sid, lgoem_dev_rtc_type *time)
{
    int sec;
    int cur_sec;

    if (!checkPriv()) return 0; // no privilege
    if (!android::init()) return 0; // binder error

    if (!getCurrentTime(&cur_sec)) cur_sec = -1; // get current time as sec
    sec = android::gService->GetTime(sid, cur_sec); // get secure time as sec

    if (sec < 0) return 0;

    rtc_time_to_lg_tm(sec, time);

    return 1;
}

extern "C" int OMADRM_SecureClock_GetTime(lgoem_dev_rtc_type *time)
{
    return SecureClock_GetTime(0, time);
}

extern "C" int MSDRM_SecureClock_GetTime(lgoem_dev_rtc_type *time)
{
    return SecureClock_GetTime(1, time);
}

extern "C" int OMADRM_SecureClock_GetNtpTime(lgoem_dev_rtc_type *time)
{
    return SecureClock_GetTime(2, time);
}

/********** SetTime **********/

static int SecureClock_SetTime(int sid, lgoem_dev_rtc_type *time)
{
    int rc;
    int sec;
    int cur_sec;

    if (!checkPriv()) return 0; // no privilege
    if (!android::init()) return 0; // binder error

    if (!getCurrentTime(&cur_sec)) cur_sec = -1; // get current time
    rtc_lg_tm_to_time(time, &sec); // get secure clock time as sec

    rc = android::gService->SetTime(sid, cur_sec, sec);

    return rc < 0 ? 0 : 1;
}

extern "C" int OMADRM_SecureClock_SetTime(lgoem_dev_rtc_type *time)
{
    return SecureClock_SetTime(0, time);
}

extern "C" int MSDRM_SecureClock_SetTime(lgoem_dev_rtc_type *time)
{
    return SecureClock_SetTime(1, time);
}

extern "C" int OMADRM_SecureClock_SetNtpTime(lgoem_dev_rtc_type *time)
{
    return SecureClock_SetTime(2, time);
}

/********** GetStatus **********/

static int SecureClock_GetStatus(int sid)
{
    int rc;

    if (!checkPriv()) return 0; // no privilege
    if (!android::init()) return 0; // binder error

    rc = android::gService->GetStatus(sid);

    return rc == 1 ? 1 : 0;
}

extern "C" int OMADRM_SecureClock_GetStatus(void)
{
    return SecureClock_GetStatus(0);
}

extern "C" int MSDRM_SecureClock_GetStatus(void)
{
    return SecureClock_GetStatus(1);
}

extern "C" int OMADRM_SecureClock_GetNtpStatus(void)
{
    return SecureClock_GetStatus(2);
}

/********** SetStatus **********/

static int SecureClock_SetStatus(int sid, int status)
{
    int rc;

    if (!checkPriv()) return 0; // no privilege
    if (!android::init()) return 0; // binder error

    rc = android::gService->SetStatus(sid, status);

    return rc < 0 ? 0 : 1;
}

extern "C" int OMADRM_SecureClock_SetStatus(int status)
{
    return SecureClock_SetStatus(0, status);
}

extern "C" int MSDRM_SecureClock_SetStatus(int status)
{
    return SecureClock_SetStatus(1, status);
}

extern "C" int OMADRM_SecureClock_SetNtpStatus(int status)
{
    return SecureClock_SetStatus(2, status);
}

/********** RtcChanged **********/

extern "C" int SecureClock_RtcChanged(int offset)
{
    if (!checkPriv()) return 0;
    if (!android::init()) return 0;

    android::gService->RtcChanged(offset);

    return 1;
}

/********** DIVX DRM Memory, Hardware Secret Key **********/

extern "C" int DRM_ReadMemory(char *memory)
{
    int i;
    char *hex;
    char *p;
//20101118, dh.choi@lge.com, use CP Secure Memory instead of MISC temporally [START]
//    if (!checkPriv()) return 0;

//#ifndef MISC_IO
    if (!android::init()) return 0;

    hex = (char *)android::gService->ReadDrmMemory();
    if (hex == NULL) return 0;
//#else
//    hex = (char *)sendCommand("readDrmMemory");
//    if (hex == NULL || strcmp(hex, "error") == 0) return 0;
//#endif
//20101118, dh.choi@lge.com, use CP Secure Memory instead of MISC temporally [END]

    LOGD("DRM_ReadMemory(): memory converted [%d][%s]", strlen(hex), hex);

    for (i = 0, p = hex; i < 80; i++, p = p + 2)
    {
        char buf[3] = "";
        unsigned int value;

        strncpy(buf, p, 2);
        sscanf(buf, "%02x", &value);

        *(memory + i) = value;
    }

    return 1;
}

extern "C" int DRM_ReadHwKey(char *key)
{
    int i;
    char *hex;
    char *p;

    if (!checkPriv()) return 0;
    if (!android::init()) return 0;

    hex = (char *)android::gService->ReadHwKey(); // 160 bytes
    if (hex == NULL) return 0;

    LOGD("DRM_ReadHwKey(): key converted [%d][%s]", strlen(hex), hex);

    for (i = 0, p = hex; i < 32; i++, p = p + 2)
    {
        char buf[3] = "";
        unsigned int value;

        strncpy(buf, p, 2);
        sscanf(buf, "%02x", &value);

        *(key + i) = value;
    }

    return 1;
}

extern "C" int DRM_WriteMemory(const char* memory)
{
    int i;
    char *p = (char *)memory;
    char hex[161] = "";

//20101118, dh.choi@lge.com, use CP Secure Memory instead of MISC temporally [START]
//    if (!checkPriv()) return 0;

//#ifndef MISC_IO
    if (!android::init()) return 0;
//#endif

    for (i = 0; i < 80; i++, p++)
    {
        char buf[3] = "";
        sprintf(buf, "%02x", *p);
        strcat(hex, buf);
    }

    LOGD("DRM_WriteMemory(): memory converted [%d][%s]", strlen(hex), hex);

//#ifndef MISC_IO
    return android::gService->WriteDrmMemory(hex);
//#else
//    char command[256];
//    char *resp;

//    sprintf(command, "writeDrmMemory:%s", hex);
//    resp = (char *)sendCommand(command);
//    if (resp == NULL || strcmp(resp, "error") == 0) return 0;
//    else return 1;
//#endif
//20101118, dh.choi@lge.com, use CP Secure Memory instead of MISC temporally [END]

}
