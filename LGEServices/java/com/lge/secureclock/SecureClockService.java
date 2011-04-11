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

package com.lge.secureclock;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.ServiceManager;
import android.util.Log;
import java.util.Calendar;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.provider.Settings;

/*
import com.android.internal.telephony.ATService;
import com.android.internal.telephony.ATResponse;
import com.android.internal.telephony.IATService;
import com.android.internal.telephony.ICallback;*/

import com.lge.bridge.IBridgeService;

public class SecureClockService extends ISecureClockService.Stub
{
    private static final String LOG_TAG = "SecureClockService";

    private static final int SET_OMA_OFFSET_EVENT = 1001;
    private static final int SET_MS_OFFSET_EVENT = 1002;
    private static final int SET_NTP_OFFSET_EVENT = 1003;

    private static final int GET_OMA_OFFSET_EVENT = 2001;
    private static final int GET_MS_OFFSET_EVENT = 2002;
    private static final int GET_NTP_OFFSET_EVENT = 2003;

    private static final int SET_OMA_STATUS_EVENT = 3001;
    private static final int SET_MS_STATUS_EVENT = 3002;
    private static final int SET_NTP_STATUS_EVENT = 3003;

    private static final int GET_OMA_STATUS_EVENT = 4001;
    private static final int GET_MS_STATUS_EVENT = 4002;
    private static final int GET_NTP_STATUS_EVENT = 4003;

    private static final int SET_OMA_TIME_EVENT = 5001;
    private static final int SET_MS_TIME_EVENT = 5002;
    private static final int SET_NTP_TIME_EVENT = 5003;

    private static final int GET_OMA_TIME_EVENT = 6001;
    private static final int GET_MS_TIME_EVENT = 6002;
    private static final int GET_NTP_TIME_EVENT = 6003;

    private static final int READ_DRM_MEMORY = 7001;
    private static final int READ_HARDWARE_KEY = 7002;

    private static final int WRITE_DRM_MEMORY = 8001;

    private static final int WAIT_FOR_RESPONSE = 5 * 1000; // 5 seconds

    private Context mContext;
    private Object mLock = new Object();
    private int mValue;
    private String mStringValue;
    private boolean mValid;
    private boolean mResponse;
    //private ATService mATService;

    private int cOmaOffset; private boolean cOmaOffsetValid;
    private int cMsOffset; private boolean cMsOffsetValid;
    private int cNtpOffset; private boolean cNtpOffsetValid;

    private int cOmaStatus; private boolean cOmaStatusValid;
    private int cMsStatus; private boolean cMsStatusValid;
    private int cNtpStatus; private boolean cNtpStatusValid;

    private int cOmaTime; private boolean cOmaTimeValid;
    private int cMsTime; private boolean cMsTimeValid;
    private int cNtpTime; private boolean cNtpTimeValid;

    private String cHardwareKey; private boolean cHardwareKeyValid;
    private boolean mIsRtcResetChecked = false;

    private void checkRtcReset()
    {
        Log.d(LOG_TAG, "checking rtc reset: start!");
        IBridgeService bridge = IBridgeService.Stub.asInterface(ServiceManager.getService("bridge"));

        try
        {
            int event;

            // 1. get rtc
            String resp = bridge.sendCommand("getCurrentTime");
            int rtc = Integer.parseInt(resp.substring(1, resp.length() - 1));
            Log.d(LOG_TAG, "checking rtc reset: current time[" + rtc + "]");

            // 2. find synchronized secureclock
            int sid;
            for (sid = 0; sid < 3; sid++)
            {
                // Log.d(LOG_TAG, "checking rtc reset: wait 3s for echo secureclock");
                // SystemClock.sleep(3000);

                if (sid == 0) event = GET_OMA_STATUS_EVENT;
                else if (sid == 1) event = GET_MS_STATUS_EVENT;
                else event = GET_NTP_STATUS_EVENT; // sid == 2

                if (!getValue(event)) continue;
                if (mValue != 1) continue; // skip not synchronized secureclock

                Log.d(LOG_TAG, "checking rtc reset: secureclock sid[" + sid + "] synchronized");

                // 3. get offset for synchronized secureclock
                if (sid == 0) event = GET_OMA_OFFSET_EVENT;
                else if (sid == 1) event = GET_MS_OFFSET_EVENT;
                else event = GET_NTP_OFFSET_EVENT; // sid == 2

                if (!getValue(event)) continue;
                int offset = mValue;

                Log.d(LOG_TAG, "checking rtc reset: secureclock sid[" + sid + "] offset[" + offset + "]");

                // 4. get stored secure time for synchronized secureclock
                if (sid == 0) event = GET_OMA_TIME_EVENT;
                else if (sid == 1) event = GET_MS_TIME_EVENT;
                else event = GET_NTP_TIME_EVENT; // sid == 2

                if (!getValue(event)) continue;
                int time = mValue;

                Log.d(LOG_TAG, "checking rtc reset: secureclock sid[" + sid + "] stored time[" + time + "]");

                // 5. invalidate all secureclock if rtc reset
                if (rtc + offset < time)
                {
                    Log.d(LOG_TAG, "checking rtc reset: rtc + offset < stored time, invalidate all secureclock!");

                    for (int _sid = 0; _sid < 3; _sid++) // invalidate all secure clock
                    {
                        int _event;

                        if (_sid == 0) _event = SET_OMA_STATUS_EVENT;
                        else if (_sid == 1) _event = SET_MS_STATUS_EVENT;
                        else _event = SET_NTP_STATUS_EVENT; // _sid == 2

                        setValue(_event, 0);
                    }
                }

                break; // checking one secureclock is enough!
            }
        }
        catch (Exception e)
        {
            Log.d(LOG_TAG, "checking rtc reset: exception[" + e + "]");
        }

        Log.d(LOG_TAG, "checking rtc reset: end!");
    }

    public SecureClockService(Context _context)
    {
        mContext = _context;
       // mATService = ATService.getDefault();
    }

    // sid means SecureClock ID (OMA = 0, MS = 1, NTP = 2)

    public int GetTime(int sid, int CurTime)
    {
        Log.d(LOG_TAG, "GetTime(): sid[" + sid + "], CurTime[" + CurTime + "]");

        synchronized (this)
        {
            int event;

            if (CurTime < 0) return -1;

            // 1. check sync flag

            if (sid == 0) event = GET_OMA_STATUS_EVENT;
            else if (sid == 1) event = GET_MS_STATUS_EVENT;
            else event = GET_NTP_STATUS_EVENT; // sid == 2

            if (!getValue(event)) return -1;
            if (mValue != 1) return -1;

            // 2. get offset (secure time - rtc)

            if (sid == 0) event = GET_OMA_OFFSET_EVENT;
            else if (sid == 1) event = GET_MS_OFFSET_EVENT;
            else event = GET_NTP_OFFSET_EVENT; // sid == 2

            if (!getValue(event)) return -1;

            // 3. calculate secure time (offset + rtc)

            int SecureTime =  mValue + CurTime;

            return SecureTime;
        } // synchronized (this)
    }

    public int SetTime(int sid, int CurTime, int SecureTime)
    {
        Log.d(LOG_TAG, "SetTime(): sid[" + sid + "], CurTime[" + CurTime + "], SecureTime[" + SecureTime + "]");

        synchronized (this)
        {
            int event;
            boolean result;

            if (CurTime < 0) return -1;

            // 1. set secure time (for handling rtc reset event)

            if (sid == 0) event = SET_OMA_TIME_EVENT;
            else if (sid == 1) event = SET_MS_TIME_EVENT;
            else event = SET_NTP_TIME_EVENT; // sid == 2

            result = setValue(event, SecureTime);

            // 2. set offset (secure time - rtc)

            if (result)
            {
                if (sid == 0) event = SET_OMA_OFFSET_EVENT;
                else if (sid == 1) event = SET_MS_OFFSET_EVENT;
                else event = SET_NTP_OFFSET_EVENT; // sid == 2

                int offset = SecureTime - CurTime;
                result = setValue(event, offset); // set offset
            }

            // 3. set status (sync flag)

            if (sid == 0) event = SET_OMA_STATUS_EVENT;
            else if (sid == 1) event = SET_MS_STATUS_EVENT;
            else event = SET_NTP_STATUS_EVENT; // sid == 2

            if (!setValue(event, result ? 1 : 0)) return -1; // set status
            else return result ? 1 : -1;
        } // synchronized (this)
    }

    public int GetStatus(int sid)
    {
        Log.d(LOG_TAG, "OMADRM_GetStatus(): sid[" + sid + "]");

        synchronized (this)
        {
            if (!mIsRtcResetChecked) { checkRtcReset(); mIsRtcResetChecked = true; }

            int event;

            if (sid == 0) event = GET_OMA_STATUS_EVENT;
            else if (sid == 1) event = GET_MS_STATUS_EVENT;
            else event = GET_NTP_STATUS_EVENT; // sid == 2

            if (getValue(event)) return mValue;
            else return -1;
        } // synchronized (this)
    }

    public int SetStatus(int sid, int Status)
    {
        Log.d(LOG_TAG, "OMADRM_SetStatus(): sid[" + sid + "], Status[" + Status + "]");

        synchronized (this)
        {
            int event;

            if (sid == 0) event = SET_OMA_STATUS_EVENT;
            else if (sid == 1) event = SET_MS_STATUS_EVENT;
            else event = SET_NTP_STATUS_EVENT; // sid == 2

            if (setValue(event, Status)) return 1;
            else return -1;
        } // synchronized (this)
    }

    private boolean setValue(int event, int value)
    {
        Log.d(LOG_TAG, "setValue(): event[" + event + "], value[" + value + "]");

        try
        {
            IBridgeService bridge = IBridgeService.Stub.asInterface(ServiceManager.getService("bridge"));
            String resp = bridge.sendCommand("setSecureClockValue:" + event + "," + value);
            if (resp.equals("ok"))
            {
                setCachedValue(event, value);
                return true;
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "setValue(): exception[" + e + "]");
        }

        return false;
    }

    private boolean getValue(int event) // value will be stored into mValue
    {
        Log.d(LOG_TAG, "getValue(): event[" + event + "]");

        if (isCachedValueValid(event))
        {
            mValue = getCachedValue(event);
            Log.d(LOG_TAG, "getValue(): cached mValue[" + mValue + "]");

            return true;
        }

        try
        {
            IBridgeService bridge = IBridgeService.Stub.asInterface(ServiceManager.getService("bridge"));
            String resp = bridge.sendCommand("getSecureClockValue:" + event);

            if (!resp.equals("error"))
            {
                mValue = Integer.parseInt(resp);
                setCachedValue(event, mValue);

                Log.d(LOG_TAG, "getValue(): mValue[" + mValue + "]");
                return true;
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "getValue(): exception[" + e + "]");
        }

        return false;
    }

    private void initCachedValue()
    {
        cOmaOffsetValid = false;
        cMsOffsetValid = false;
        cNtpOffsetValid = false;
        cOmaStatusValid = false;
        cMsStatusValid = false;
        cNtpStatusValid = false;
        cOmaTimeValid = false;
        cMsTimeValid = false;
        cNtpTimeValid = false;
        cHardwareKeyValid = false;
    }

    private int getCachedValue(int event)
    {
        switch (event)
        {
            case GET_OMA_OFFSET_EVENT:
                return cOmaOffset;
            case GET_MS_OFFSET_EVENT:
                return cMsOffset;
            case GET_NTP_OFFSET_EVENT:
                return cNtpOffset;
            case GET_OMA_STATUS_EVENT:
                return cOmaStatus;
            case GET_MS_STATUS_EVENT:
                return cMsStatus;
            case GET_NTP_STATUS_EVENT:
                return cNtpStatus;
            case GET_OMA_TIME_EVENT:
                return cOmaTime;
            case GET_MS_TIME_EVENT:
                return cMsTime;
            case GET_NTP_TIME_EVENT:
                return cNtpTime;
        }

        return 0;
    }

    private boolean isCachedValueValid(int event)
    {
        switch (event)
        {
            case GET_OMA_OFFSET_EVENT:
                return cOmaOffsetValid;
            case GET_MS_OFFSET_EVENT:
                return cMsOffsetValid;
            case GET_NTP_OFFSET_EVENT:
                return cNtpOffsetValid;
            case GET_OMA_STATUS_EVENT:
                return cOmaStatusValid;
            case GET_MS_STATUS_EVENT:
                return cMsStatusValid;
            case GET_NTP_STATUS_EVENT:
                return cNtpStatusValid;
            case GET_OMA_TIME_EVENT:
                return cOmaTimeValid;
            case GET_MS_TIME_EVENT:
                return cMsTimeValid;
            case GET_NTP_TIME_EVENT:
                return cNtpTimeValid;
        }

        return false;
    }

    private void setCachedValue(int event, int value)
    {
        switch (event)
        {
            case GET_OMA_OFFSET_EVENT:
            case SET_OMA_OFFSET_EVENT:
                cOmaOffset = value; cOmaOffsetValid = true; break;
            case GET_MS_OFFSET_EVENT:
            case SET_MS_OFFSET_EVENT:
                cMsOffset = value; cMsOffsetValid = true; break;
            case GET_NTP_OFFSET_EVENT:
            case SET_NTP_OFFSET_EVENT:
                cNtpOffset = value; cNtpOffsetValid = true; break;
            case GET_OMA_STATUS_EVENT:
            case SET_OMA_STATUS_EVENT:
                cOmaStatus = value; cOmaStatusValid = true; break;
            case GET_MS_STATUS_EVENT:
            case SET_MS_STATUS_EVENT:
                cMsStatus = value; cMsStatusValid = true; break;
            case GET_NTP_STATUS_EVENT:
            case SET_NTP_STATUS_EVENT:
                cNtpStatus = value; cNtpStatusValid = true; break;
            case GET_OMA_TIME_EVENT:
            case SET_OMA_TIME_EVENT:
                cOmaTime = value; cOmaTimeValid = true; break;
            case GET_MS_TIME_EVENT:
            case SET_MS_TIME_EVENT:
                cMsTime = value; cMsTimeValid = true; break;
            case GET_NTP_TIME_EVENT:
            case SET_NTP_TIME_EVENT:
                cNtpTime = value; cNtpTimeValid = true; break;
        }
    }

    public Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case WRITE_DRM_MEMORY:
                    synchronized (mLock)
                    {
                        Log.d(LOG_TAG, "handleMessage(): event = " + msg.what);

                        mResponse = true;
                        mLock.notifyAll();
                    }
                    break;
                case READ_DRM_MEMORY:
                case READ_HARDWARE_KEY:
                    synchronized (mLock)
                    {
                        Log.d(LOG_TAG, "handleMessage(): event = " + msg.what);

                        /*ATResponse response = new ATResponse((String)msg.obj);
                        String[] data = response.getData();
                        if (data != null && data.length > 0)
                        {
                            Log.d(LOG_TAG, "handleMessage(): response = " + data[0]);

                            String dataOnly = data[0].substring(1, data[0].length() - 1);
                            Log.d(LOG_TAG, "handleMessage(): dataOnly = " + dataOnly);
                            mStringValue = dataOnly;
                            mValid = true;
                        }*/

                        mResponse = true;
                        mLock.notifyAll();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    public void RtcChanged(int Offset)
    {
        synchronized (this)
        {
            Log.d(LOG_TAG, "update secureclock by RTC changed event [START]");
            for (int sid = 0; sid < 3; sid++)
            {
                Log.d(LOG_TAG, "update secureclock by RTC changed event [" + sid + "]");

                int event;

                if (sid == 0) event = GET_OMA_STATUS_EVENT;
                else if (sid == 1) event = GET_MS_STATUS_EVENT;
                else event = GET_NTP_STATUS_EVENT; // sid == 2

                if (!getValue(event)) break; // TODO: exception?
                if (mValue != 1) continue;

                if (sid == 0) event = GET_OMA_OFFSET_EVENT;
                else if (sid == 1) event = GET_MS_OFFSET_EVENT;
                else event = GET_NTP_OFFSET_EVENT; // sid == 2

                if (!getValue(event)) break; // TODO: exception?

                if (sid == 0) event = SET_OMA_OFFSET_EVENT;
                else if (sid == 1) event = SET_MS_OFFSET_EVENT;
                else event = SET_NTP_OFFSET_EVENT; // sid == 2

                if (!setValue(event, mValue - Offset)) break; // TODO: exception?
            }
            Log.d(LOG_TAG, "update secureclock by RTC changed event [END]");
        }
    }

    public String ReadDrmMemory()
    {
// gunwoo1.kim@lge.com, fix WBT#170370 [START]
//      synchronized (this)
//      {
// gunwoo1.kim@lge.com, fix WBT#170370 [END]
            synchronized (mLock)
            {
                /*mValid = false;

                Message m = mHandler.obtainMessage(READ_DRM_MEMORY);
                mATService.sendCommand("AT%DRMMEM?", m);
                try { mLock.wait(WAIT_FOR_RESPONSE); }
                catch (Exception e) { Log.e(LOG_TAG, "Interrupted"); }

                Log.d(LOG_TAG, "ReadDrmMemory(): mValid[" + mValid + "], mStringValue[" + mStringValue + "]");

                if (mValid) return mStringValue;
                else*/ return null;
            }
// gunwoo1.kim@lge.com, fix WBT#170370 [START]
//      }
// gunwoo1.kim@lge.com, fix WBT#170370 [END]
    }

    public String ReadHardwareKey()
    {
// gunwoo1.kim@lge.com, fix WBT#170371 [START]
//      synchronized (this)
//      {
// gunwoo1.kim@lge.com, fix WBT#170371 [END]
            if (cHardwareKeyValid) return cHardwareKey;

            synchronized (mLock)
            {
                /*mValid = false;

                Message m = mHandler.obtainMessage(READ_HARDWARE_KEY);
                mATService.sendCommand("AT%DRMHWKEY?", m);
                try { mLock.wait(WAIT_FOR_RESPONSE); }
                catch (Exception e) { Log.e(LOG_TAG, "Interrupted"); }

                if (mValid) { cHardwareKey = mStringValue; cHardwareKeyValid = true; return cHardwareKey; }
                else*/ return null;
            }
// gunwoo1.kim@lge.com, fix WBT#170371 [START]
//      }
// gunwoo1.kim@lge.com, fix WBT#170371 [END]
    }

    public int WriteDrmMemory(String memory)
    {
// gunwoo1.kim@lge.com, fix WBT#170372 [START]
//      synchronized (this)
//      {
// gunwoo1.kim@lge.com, fix WBT#170372 [END]
            synchronized (mLock)
            {
                mResponse = false;
/*
                Message m = mHandler.obtainMessage(WRITE_DRM_MEMORY);
                mATService.sendCommand("AT%DRMMEM=" + memory, m);
                try { mLock.wait(WAIT_FOR_RESPONSE); }
                catch (Exception e) { Log.e(LOG_TAG, "Interrupted"); }*/

                return mResponse ? 1 : 0;
            }
// gunwoo1.kim@lge.com, fix WBT#170373 [START]
//      }
// gunwoo1.kim@lge.com, fix WBT#170373 [END]
    }
}
