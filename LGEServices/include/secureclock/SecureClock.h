#ifndef SECURE_CLOCK_H
#define SECURE_CLOCK_H

#ifdef __cplusplus
extern "C" {
#endif

typedef struct
{
    unsigned int Year;
    unsigned int Month;
    unsigned int Day;
    unsigned int Hour;
    unsigned int Minute;
    unsigned int Second;
}
lgoem_dev_rtc_type;

int OMADRM_SecureClock_GetTime(lgoem_dev_rtc_type *time);
int OMADRM_SecureClock_SetTime(lgoem_dev_rtc_type *time);
int OMADRM_SecureClock_GetStatus(void);
int OMADRM_SecureClock_SetStatus(int status);

int OMADRM_SecureClock_GetNtpTime(lgoem_dev_rtc_type *time);
int OMADRM_SecureClock_SetNtpTime(lgoem_dev_rtc_type *time);
int OMADRM_SecureClock_GetNtpStatus(void);
int OMADRM_SecureClock_SetNtpStatus(int status);

int MSDRM_SecureClock_GetTime(lgoem_dev_rtc_type *time);
int MSDRM_SecureClock_SetTime(lgoem_dev_rtc_type *time);
int MSDRM_SecureClock_GetStatus(void);
int MSDRM_SecureClock_SetStatus(int status);

int SecureClock_RtcChanged(int offset);

int DRM_ReadMemory(char *memory);
int DRM_ReadHwKey(char *key);
int DRM_WriteMemory(const char* memory);

#ifdef __cplusplus
}
#endif

#endif
