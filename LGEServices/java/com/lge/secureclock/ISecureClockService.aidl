package com.lge.secureclock;

interface ISecureClockService
{
    // sid means SecureClock ID (OMA = 0, MS = 1, NTP = 2)
    int GetTime(in int sid, in int CurTime); // return < 0 means error
    int SetTime(in int sid, in int CurTime, in int SecureTime); // return < 0 means error
    int GetStatus(in int sid); // return < 0 means error
    int SetStatus(in int sid, in int Status); // return < 0 means error
    void RtcChanged(in int Offset);

    String ReadDrmMemory(); // 80 chars
    String ReadHardwareKey(); // 32 chars
    int WriteDrmMemory(String memory); // 80 chars
}

