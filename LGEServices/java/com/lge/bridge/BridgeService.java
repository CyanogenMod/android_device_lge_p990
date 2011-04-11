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

package com.lge.bridge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import java.io.*;
import java.nio.channels.FileChannel;

import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.HashMap;
import java.util.zip.CRC32;
import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;

import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.Build;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.os.SystemClock;
import android.os.Environment;
import android.os.PowerManager;

import android.provider.Settings;
/*import com.lge.provider.Andy_Flex;
import com.lge.config.StarConfig;*/

import com.android.internal.telephony.ITelephony;
/*import com.android.internal.telephony.ATService;
import com.android.internal.telephony.ATResponse;
import com.android.internal.telephony.IATService;
import com.android.internal.telephony.ICallback;*/

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.net.Uri;

import android.content.ContentResolver;
import android.database.Cursor;

public class BridgeService extends IBridgeService.Stub
{
    private static final String LOG_TAG = "BridgeService";

    private String mStx;
    private String mEtx;

    private String mResponse;
    private boolean mResp;
    private String[] mData;
    private Object mLock = new Object();
    private static final int SEND_COMMAND_RESPONSE = 333;

    private String mPhoneNumber = null;
    private String mHardwareVersion = null;
    private String mSoftwareVersion = null;
    private String mCodeCrc = null;

    private boolean mIsBootComplete = false;

    private Context mContext;
    private Vibrator mVibrator;
    private int mHdmiStatus = -1;

    public static final String HDMI_STATUS = "com.lge.bridge.HDMI_STATUS";
    public static final String HDMI_STATUS_REQUEST = "com.lge.bridge.HDMI_STATUS_REQUEST";

    public BridgeService(Context context)
    {
        this.mContext = context;
        System.loadLibrary("bridge_jni"); // libbridge_jni
        mVibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);

        char [] stx = { (char)0x02 };
        char [] etx = { (char)0x03 };
        mStx = new String(stx);
        mEtx = new String(etx);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mContext.registerReceiver(mReceiver, filter, null, null);

    }

    BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
            {
                mIsBootComplete = true;
                Log.d(LOG_TAG, "ACTION_BOOT_COMPLETED: set mIsBootComplete true");

                new Thread()
                {
                    public void run()
                    {
                        // get phone number in advance
                        /*getPhoneNumber();
                        Log.d(LOG_TAG, "ACTION_BOOT_COMPLETED: phone number is " + (mPhoneNumber != null ? mPhoneNumber : "unknown"));*/

                        // broadcast hdmi status
                        if (mHdmiStatus == 0 || mHdmiStatus == 1)
                        {
                            mHandler.post(mHdmiStatusBroadcastTask);
                            Log.d(LOG_TAG, "ACTION_BOOT_COMPLETED: broadcast hdmi stataus");
                        }
                    }
                }.start();
            }
            else if (intent.getAction().equals(HDMI_STATUS_REQUEST))
            {
                Log.d(LOG_TAG, "onReceive(HDMI_STATUS_REQUEST): call setHdmiStatus(" + mHdmiStatus + ")");
                setHdmiStatus("setHdmiStatus:" + mHdmiStatus); // bypass current status
            }
        }
    };

    private native String sendNativeCommandInternal(String command);

    public String sendNativeCommand(String command)
    {
        Log.d(LOG_TAG, "sendNativeCommand(): command[" + command + "]");

        try
        {
            String resp = sendNativeCommandInternal(command);
            Log.d(LOG_TAG, "sendNativeCommand(): returns[" + resp + "]");
            return resp;
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "sendNativeCommand(): exception[" + e + "]");
        }

        return "error";
    }

    public String sendCommand(String command)
    {
        Log.d(LOG_TAG, "sendCommand(): command[" + command + "]");

	/*
        if (command.startsWith("sendATCommand"))
        {
            try
            {
                if (sendATCommand(command.substring(command.indexOf(":") + 1)))
                {
                    return mData[0];
                }
                else
                {
                    return "error";
                }
            }
            catch (Exception e)
            {
                return "error";
            }
        }*/

        if (command.equals("isBootComplete")) return (mIsBootComplete ? "true" : "false");
        /*if (command.equals("getSoftwareVersion")) return getSoftwareVersion();
        if (command.equals("getHardwareVersion")) return getHardwareVersion();
        if (command.equals("getPhoneNumber")) return getPhoneNumber();
        if (command.equals("getMccCode")) return getMccCode();
        if (command.equals("getMncCode")) return getMncCode();
        if (command.equals("getFileCrc")) return getFileCrc();
        if (command.equals("getFpriCrc")) return getFpriCrc();
        if (command.equals("getCodeCrc")) return getCodeCrc();
        if (command.equals("getTotalCrc")) return getTotalCrc();
        if (command.equals("checkDatabase")) return checkDatabase();
        if (command.equals("generateFileCrc")) return generateFileCrc();
        if (command.equals("shutdownModem")) return shutdownModem();
        if (command.equals("getCalCrc")) return getCalCrc();
        if (command.equals("getSLCT")) return getSLCT();*/
        if (command.equals("readDrmMemory")) return sendNativeCommand(command);
        if (command.startsWith("writeDrmMemory")) return sendNativeCommand(command);
        if (command.equals("getFactoryMode")) return sendNativeCommand(command);
        if (command.startsWith("setFactoryMode")) return sendNativeCommand(command);
        if (command.equals("getDeviceTestResultDetail")) return sendNativeCommand(command);
        if (command.startsWith("setDeviceTestResultDetail")) return sendNativeCommand(command);
        if (command.equals("getDeviceTestResult")) return sendNativeCommand(command);
        if (command.startsWith("setDeviceTestResult")) return sendNativeCommand(command);
        /*if (command.equals("vibrateStop")) return vibrateStop(command);
        if (command.startsWith("vibrateFor")) return vibrateFor(command);
        if (command.startsWith("vibrate")) return vibrate(command);*/
        if (command.equals("webDownloadChange")) return webDownloadChange(); // 101110, chihyun.kim@lge.com WEBDN
        /*if (command.equals("sendReadyToModem")) return sendCommandToModem(0); // AT%READY=1
        if (command.equals("initNvItems")) return sendCommandToModem(1); // AT%NVINIT*/
        if (command.equals("getCurrentTime")) return sendNativeCommand(command);
        if (command.startsWith("readDir")) return sendNativeCommand(command);
        if (command.equals("getChipId")) return getChipId();
        if (command.equals("getFrstStatus")) return sendNativeCommand(command);
        if (command.startsWith("setFrstStatus")) return setFrstStatus(command);
        if (command.startsWith("getSecureClockValue")) return sendNativeCommand(command);
        if (command.startsWith("setSecureClockValue")) return sendNativeCommand(command);
        if (command.equals("getSmplCount")) return sendNativeCommand(command);
        if (command.startsWith("setSmplCount")) return sendNativeCommand(command);
        if (command.equals("reboot")) return reboot();
        if (command.startsWith("setHdmiStatus")) return setHdmiStatus(command);
        if (command.equals("getHdmiStatus")) return getHdmiStatus();
        if (command.equals("initInternalSD")) return initInternalSD();
        if (command.equals("checkInternalSD")) return checkInternalSD();
        if (command.startsWith("backupLog")) return backupLog();
        /*if (command.equals("getDatabaseCrc")) return getDatabaseCrc();
        if (command.equals("dumpDatabase")) return dumpDatabase();
        if (command.equals("isExternalMemoryMounted")) return isExternalMemoryMounted();*/

        return "error";
    }

    /*
    private String isExternalMemoryMounted()
    {
        final String status = Environment.getExternalAddStorageState();

        Log.d(LOG_TAG, "isExternalMemoryMounted(): status = " + status);

        if (status.equals(Environment.MEDIA_MOUNTED)) { Log.d(LOG_TAG, "isExternalMemoryMounted() returns true"); return "true"; }
        else { Log.d(LOG_TAG, "isExternalMemoryMounted() returns false"); return "false"; }
    }*/

    private ArrayList<String> mDatabaseFiles = null;

    private ArrayList<String> loadDatabaseFileList()
    {
        if (mDatabaseFiles != null) return mDatabaseFiles;

        mDatabaseFiles = new ArrayList<String>(8);

        mDatabaseFiles.add("/data/data/com.android.providers.telephony/databases/mmssms.db");
        mDatabaseFiles.add("/data/data/com.lge.providers.flex/databases/flex.db");
        /*if (StarConfig.OPERATOR.equals("SKT"))
        {
            mDatabaseFiles.add("/data/data/com.lge.ims.providers.settings/databases/imssettings.db");
        }*/

        return mDatabaseFiles;
    }

    private Long getCrcByFile(String path)
    {
        String logStr = "getCrcByFile(): path[" + path + "]";

        try
        {
            CRC32 crc = new CRC32();

            int read;
            byte[] buffer = new byte[4096];

            FileInputStream fis = new FileInputStream(path);
            while ((read = fis.read(buffer, 0, 4096)) > 0)
            {
                crc.update(buffer, 0, read);
            }
            fis.close();

            Log.d(LOG_TAG, logStr + " crc[" + Long.toHexString(crc.getValue()).toUpperCase() + "]");

            return crc.getValue();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            return -1L;
        }
    }

    private void updateCrcByFile(CRC32 crc, String path)
    {
        String logStr = "updateCrcByFile(): path[" + path + "]";

        try
        {
            int read;
            byte[] buffer = new byte[4096];

            FileInputStream fis = new FileInputStream(path);
            while ((read = fis.read(buffer, 0, 4096)) > 0)
            {
                crc.update(buffer, 0, read);
            }
            fis.close();

            Log.d(LOG_TAG, logStr + " crc updated!");
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
        }
    }

    private String getDatabaseCrc()
    {
        if (loadDatabaseFileList() == null) return "error";

        Long result = 0L;

        try
        {
            for (int i = 0; i < mDatabaseFiles.size(); i++)
            {
                String dbPath = mDatabaseFiles.get(i);
                Long crc = getCrcByFile(dbPath);
                if (crc > 0) result = result + crc;
            }

            //if (checkDatabase().equals("ok")) result = result + 1;

            return Long.toHexString(result).toUpperCase();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "getDatabaseCrc(): exception[" + e + "]");
            return "error";
        }
    }

    private boolean mkdir(String dirPath)
    {
        String logStr = "mkdir(): dirPath[" + dirPath + "]";

        try
        {
            File dir = new File(dirPath);

            if (dir.exists())
            {
                Log.d(LOG_TAG, logStr + " already exists");
                return true;
            }

            boolean resp = dir.mkdir();
            Log.d(LOG_TAG, logStr + " " + resp);

            return resp;

        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            return false;
        }
    }

    private boolean copyFile(String destPath, String srcPath)
    {
        String logStr = "copyFile(): destPath[" + destPath + "], srcPatah[" + srcPath + "]";

        try
        {
            FileInputStream inputStream = new FileInputStream(new File(srcPath));
            FileOutputStream outputStream = new FileOutputStream(new File(destPath));

            FileChannel fcin = inputStream.getChannel();
            FileChannel fcout = outputStream.getChannel();

            long size = fcin.size();

            fcin.transferTo(0, size, fcout);

            fcout.close();
            fcin.close();
            outputStream.close();
            inputStream.close();

            Log.e(LOG_TAG, logStr + " success!");

            return true;
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            return false;
        }
    }

    /*
    private String dumpDatabase()
    {
        if (loadDatabaseFileList() == null) return "error";
        if (isExternalMemoryMounted().equals("false")) return "error";

        try
        {
            String sdPath = Environment.getExternalAddStorageDirectory().getAbsolutePath();

            for (int i = 0; i < mDatabaseFiles.size(); i++)
            {
                String srcPath = mDatabaseFiles.get(i);
                String destPath = sdPath + "/" + srcPath.substring(srcPath.lastIndexOf("/") + 1);

                if (!copyFile(destPath, srcPath)) return "error";
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "dumpDatabase(): exception[" + e + "]");
            return "error";
        }

        return "ok";
    }*/

    private HashMap<String, Long> mContents = null;
    private HashSet<String> mIgnoreList = null;

    private HashSet<String> loadIgnoreList()
    {
        if (mIgnoreList != null) return mIgnoreList;

        mIgnoreList = new HashSet(16);

        mIgnoreList.add("/sdcard/_ExternalSD");
        mIgnoreList.add("/sdcard/melon.info");
        mIgnoreList.add("/sdcard/Android/data/com.cooliris.media");
        mIgnoreList.add("/sdcard/Tstore/Temp/Device.info");
        mIgnoreList.add("/sdcard/Android/data/com.android.providers.media/albumthumbs");

        return mIgnoreList;
    }

    private HashMap<String, Long> loadContentInfo()
    {
        if (mContents != null) return mContents;

        mContents = new HashMap(64);

        try
        {
            String path;
            String ptr;

            path = "/etc/contents.txt";
            LineNumberReader reader = new LineNumberReader(new FileReader(path));

            while ((ptr = reader.readLine()) != null)
            {
                if (ptr.indexOf(",") == -1) continue;
                String[] tokens = ptr.split(",");
                String contentPath = tokens[0];
                long contentLength = Long.parseLong(tokens[1]);

                Log.d(LOG_TAG, "loadContentInfo(): path[" + contentPath + "], length[" + contentLength  +"]");
                mContents.put(contentPath, new Long(contentLength));
            }

            reader.close();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "loadContentInfo(): exception[" + e + "]");
            mContents = null;
        }

        return mContents;
    }

    private int checkInternalSDInternal(File file)
    {
        if (file == null) return -1;

        String path = file.getAbsolutePath();

        if (!file.exists())
        {
            Log.e(LOG_TAG, "checkInternalSDInternal(): path[" + path + "] not exist");
            return -1;
        }

        if (mIgnoreList.contains(path))
        {
            Log.d(LOG_TAG, "checkInternalSDInternal(): path[" + path + "] ignored");
            return 0;
        }

        try
        {
            if (path.substring(path.lastIndexOf("/") + 1).startsWith("."))
            {
                Log.d(LOG_TAG, "checkInternalSDInternal(): path[" + path + "] ignored");
                return 0;
            }
        }
        catch (Exception e)
        {
            ;
        }

        try
        {
            if (file.isDirectory())
            {
                File[] files = file.listFiles();

                int match = 0;

                for (int i = 0; files != null && i < files.length; i++)
                {
                    int retval = checkInternalSDInternal(files[i]);
                    if (retval < 0) return -1;
                    else match = match + retval;
                }

                Log.d(LOG_TAG, "checkInternalSDInternal(): dir[" + path + "], match[" + match +"]");

                return match;
            }
            else if (file.isFile())
            {
                if (mContents.containsKey(path))
                {
                    Long length = mContents.get(path);
                    if (file.length() == length.longValue())
                    {
                        Log.d(LOG_TAG, "checkInternalSDInternal(): file[" + path + "] match");
                        return 1;
                    }
                    else
                    {
                        Log.d(LOG_TAG, "checkInternalSDInternal(): file[" + path + "] size mismatch");
                        return -1;
                    }
                }
                else
                {
                    Log.d(LOG_TAG, "checkInternalSDInternal(): file[" + path + "] mismatch");
                    return -1;
                }
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "checkInternalSDInternal(): exception[" + e + "]");
        }

        return -1;
    }

    private String checkInternalSD()
    {
        if (loadContentInfo() == null || loadIgnoreList() == null) return "error";

        int match = checkInternalSDInternal(new File("/sdcard"));
        int count = mContents.size();

        Log.d(LOG_TAG, "checkInternalSD(): match[" + match + "], count[" + count + "]");

        if (match == count)
            return "ok";
        else
            return "error";
    }

    private String backupLog()
    {
        new Thread()
        {
            public void run()
            {
                fork("/system/bin/logcat -f /sdcard/backup_log.txt");
            }
        }.start();

        return "ok";
    }

    private boolean fork(String cmd)
    {
        Log.i(LOG_TAG, "fork(): cmd[" + cmd + "]");

        Runtime rt = Runtime.getRuntime();
        Process p = null;
        try
        {
            p = rt.exec(cmd);
            p.waitFor();
            p.destroy();
            Log.d(LOG_TAG, "fork(): exitValue[" + p.exitValue() + "]");

            return true;
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "fork(): exception[" + e + "]");
        }
        finally
        {
            if (p != null) p.destroy();
        }

        return false;
    }

    private void initInternalSDInternal(File file)
    {
        if (file == null) return;

        String path = file.getAbsolutePath();

        if (!file.exists())
        {
            Log.e(LOG_TAG, "initInternalSDInternal(): path[" + path + "] not exist");
            return;
        }

        if (mIgnoreList.contains(path) || mContents.containsKey(path))
        {
            Log.d(LOG_TAG, "initInternalSDInternal(): path[" + path + "] ignored");
            return;
        }

        try
        {
            if (file.isDirectory())
            {
                File[] files = file.listFiles();

                for (int i = 0; files != null && i < files.length; i++)
                    initInternalSDInternal(files[i]);

                if (file.delete()) Log.d(LOG_TAG, "initInternalSDInternal(): dir[" + path + "] deleted");
            }
            else if (file.isFile())
            {
                if (file.delete()) Log.d(LOG_TAG, "initInternalSDInternal(): file[" + path + "] deleted");
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "initInternalSDInternal(): exception[" + e + "]");
        }
    }

    private String initInternalSD()
    {
        if (loadContentInfo() == null || loadIgnoreList() == null) return "error";

        initInternalSDInternal(new File("/sdcard"));

        return "ok";
    }

    private String getHdmiStatus()
    {
        if (mHdmiStatus == 0 || mHdmiStatus == 1)
            return Integer.toString(mHdmiStatus);
        else
            return "error";
    }

    private Runnable mHdmiStatusBroadcastTask = new Runnable()
    {
        public void run()
        {
            if (!mIsBootComplete) return;
            if (mHdmiStatus != 0 && mHdmiStatus != 1) return;

            try
            {
                Intent intent = new Intent(HDMI_STATUS);
                intent.putExtra("status", mHdmiStatus);
                mContext.sendBroadcast(intent);

                Log.d(LOG_TAG, "mHdmiStatusBroadcastTask: mHdmiStatus[" + mHdmiStatus + "] broadcast success!");
            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "mHdmiStatusBroadcastTask: exception[" + e + "]");
            }
        }
    };

    private String setHdmiStatus(String command)
    {
        try
        {
            String arg = command.substring(command.indexOf(":") + 1);
            int status = Integer.parseInt(arg);

            Log.d(LOG_TAG, "setHdmiStatus(): status[" + status + "]");

            if (status == 0 || status == 1)
            {
                mHdmiStatus = status;

                if (mIsBootComplete) mHandler.post(mHdmiStatusBroadcastTask);

                return "ok";
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "setHdmiStatus(): exception[" + e + "]");
        }

        return "error";
    }

    private boolean writeSysfs(String path, String value)
    {
        FileWriter writer = null;
        boolean rc = true;

        try
        {
            writer = new FileWriter(new File(path));
            writer.write(value, 0, value.length());
            writer.flush();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "writeSysfs(" + path + "): exception occurs: " + e);
            rc = false;
        }
        finally
        {
            try { writer.close(); } catch (Exception e) { ; }
        }

        return rc;
    }

    private String setFrstStatus(String command)
    {
        try
        {
            String arg = command.substring(command.indexOf(":") + 1);
            int status = Integer.parseInt(arg);

            Log.d(LOG_TAG, "setFrstStatus(): status[" + status + "]");

            if (status == 5) writeSysfs("/sys/power/wake_lock", "qem_wakelock");
            else if (status == 6) writeSysfs("/sys/power/wake_unlock", "qem_wakelock");
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "setFrstStatus(): exception[" + e + "]");
            return "error";
        }

        return sendNativeCommand(command);
    }

    private Runnable mRebootTask = new Runnable()
    {
        public void run()
        {
            try
            {
                PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
                pm.reboot(null); // never returns
            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "pm.reboot() failed: " + e);
            }
        }
    };

    private String reboot()
    {
        mHandler.post(mRebootTask);

        return "ok";
    }

    private String getChipId()
    {
        File cmdline = new File("/proc/cmdline");
        LineNumberReader reader;
        String line;

        try
        {
            reader = new LineNumberReader(new FileReader(cmdline));
            line = reader.readLine();

            Log.d(LOG_TAG, "getChipId(): cmdline: " + line);

            int idx = line.indexOf("uniqueid=");
            idx = idx + 9;

            int spaceIdx = line.indexOf(" ", idx);

            return line.substring(idx, spaceIdx);
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "getChipId(): exception occurs: " + e);
            return "error";
        }
    }

/*    private String setFactoryMode(String command)
    {
        try
        {
            String arg = command.substring(command.indexOf(":") + 1);
            int mode = Integer.parseInt(arg);

            if (mode == 0)
            {
                sendATCommand("at+xlgeset=5,\"6\"");
            }
            else if (mode == 1)
            {
                sendATCommand("at+xlgeset=5,\"7\"");
            }
        }
        catch (Exception e)
        {
            ;
        }

        return sendNativeCommand(command);
    }

    private String vibrate(String command)
    {
        try
        {
            String arg = command.substring(command.indexOf(":") + 1);
            int ms = Integer.parseInt(arg);

            mVibrator.vibrate(ms);
        }
        catch (Exception e)
        {
            return "error";
        }

        return "ok";
    }

    private String vibrateStop(String command)
    {
        try
        {
            mVibrator.cancel();
        }
        catch (Exception e)
        {
            return "error";
        }

        return "ok";
    }

    private String vibrateFor(String command)
    {
        if (command.equals("vibrateForIncomingCall"))
            mVibrator.vibrateForIncomingCall();
        else if (command.equals("vibrateForMessageAlert"))
            mVibrator.vibrateForMessageAlert();
        else if (command.equals("vibrateForAlarm"))
            mVibrator.vibrateForAlarm();
        else if (command.equals("vibrateForCommandButton"))
            mVibrator.vibrateForCommandButton();
        else
            return "error";

        return "ok";
    }

    private String sendCommandToModem(int number)
    {
        Log.d(LOG_TAG, "sendCommandToModem(): number[" + number + "]");

        try
        {
            getPhoneService().sendCommand(number);
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "sendCommandToModem(): exception[" + e + "]");
            return "error";
        }

        return "ok";
    }

    private String getCalCrc()
    {
        try
        {
            if (sendATCommand("AT%CALCRC"))
            {
                return mData[0].substring(1, mData[0].length() - 1);
            }
            else
            {
                return "error";
            }
        }
        catch (Exception e)
        {
            return "error";
        }
    }

    private String getSLCT()
    {
        try
        {
            if (sendATCommand("AT%SLCT"))
            {
                return mData[0].substring(1, mData[0].length() - 1);
            }
            else
            {
                return "error";
            }
        }
        catch (Exception e)
        {
            return "error";
        }
    }

    private String shutdownModem()
    {
        if (sendATCommand("AT%PWROFF"))
            return "true";
        else
            return "false";
    }

    private String generateFileCrc()
    {
        if (loadEfsFiles() == null) return "error";

        String resp = "true";
        File efsCrcFile = null;
        PrintWriter writer = null;

        try
        {
            efsCrcFile = new File("/sdcard/EFS_CRC.txt");
            writer = new PrintWriter(efsCrcFile);

            if (generateCrcByFileSize(writer)) resp = "true";
            else resp = "false";
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "generateFileCrc(): exception: " + e);
            resp = "false";
        }
        finally
        {
            try { writer.close(); } catch (Exception e) { ; }
        }

        return resp;
    }

    private boolean generateCrcByFileSize(PrintWriter writer)
    {
        if (mEfsFiles == null) return false;

        try
        {
            for (int i = 0; i < mEfsFiles.size(); i++)
            {
                File file = new File(mEfsFiles.get(i));
                String path = file.getAbsolutePath();

                if (!file.exists())
                {
                    Log.e(LOG_TAG, "generateCrcByFileSize(): path[" + path + "] not exist");
                    return false;
                }

                Log.d(LOG_TAG, "updateCrcByFileSize(): path[" + path + "], length[" + file.length() + "]");
                CRC32 crc = new CRC32();
                crc.update((int)file.length());
                writer.println(path + " : " + Long.toHexString(crc.getValue()).toUpperCase());
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "generateCrcByFileSize(): exception[" + e + "]");
            return false;
        }

        return true;
    }

    private String checkDatabase()
    {
        Cursor cursor = null;

        try
        {
            cursor = mContext.getContentResolver().query(Settings.System.CONTENT_URI, null, "integrity check", null, null);

            if (cursor != null && cursor.getCount() > 0)
            {
                cursor.moveToFirst();
                String result = cursor.getString(cursor.getColumnIndex("integrity_check"));
                cursor.close(); cursor = null;

                if (null != result && result.equalsIgnoreCase("OK")) return "true";
            }
        }
        finally
        {
            if (cursor != null) try { cursor.close(); } catch (Exception e) { ; }
        }

        return "false";
    }

    private String getTotalCrc()
    {
        long fpriCrc;
        long fileCrc;
        long codeCrc;
        long totalCrc;

        try
        {
            fpriCrc = Long.parseLong(getFpriCrc(), 16);
            fileCrc = Long.parseLong(getFileCrc(), 16);
            codeCrc = Long.parseLong(getCodeCrc(), 16);

            Log.d(LOG_TAG, "getTotalCrc(): fpriCrc[" + fpriCrc + "], fileCrc[" + fileCrc + "], codeCrc[" + codeCrc +"]");
            totalCrc = fpriCrc + fileCrc + codeCrc;

            if (checkDatabase().equals("true")) totalCrc = totalCrc + 1;

            Log.d(LOG_TAG, "getTotalCrc(): totalCrc[" + totalCrc + "]");

            return Long.toString(totalCrc, 16).toUpperCase();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "getTotalCrc(): exception[" + e + "]");
            return "error";
        }
    }

    private Long getApCodeCrc()
    {
        File cmdline = new File("/proc/cmdline");
        LineNumberReader reader;
        String line;

        if (!cmdline.exists() || !cmdline.canRead()) return -1L;

        try
        {
            reader = new LineNumberReader(new FileReader(cmdline));
            line = reader.readLine();

            Log.d(LOG_TAG, "getApCodeCrc(): cmdline: " + line);

            int idx = line.indexOf("CRC=");
            idx = idx + 4;

            int spaceIdx = line.indexOf(" ", idx);

            return Long.parseLong(line.substring(idx, spaceIdx).toUpperCase(), 16);
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "getApCodeCrc(): exception[" + e + "]");
            return -1L;
        }
    }

    private Long getCpCodeCrc()
    {
        try
        {
            if (sendATCommand("AT%CPCODECRC"))
                return Long.parseLong(mData[0].substring(1, mData[0].length() - 1), 16);
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "getCpCodeCrc(): exception[" + e + "]");
        }

        return -1L;
    }

    private String getCodeCrc()
    {
        Long apCodeCrc;
        Long cpCodeCrc;

        if (mCodeCrc != null) return mCodeCrc;

        apCodeCrc = getApCodeCrc();
        cpCodeCrc = getCpCodeCrc();

        Log.d(LOG_TAG, "getCodeCrc(): apCodeCrc[" + apCodeCrc + "], cpCodeCrc[" + cpCodeCrc + "]");

        if (apCodeCrc > 0 && cpCodeCrc > 0)
        {
            mCodeCrc = Long.toHexString(apCodeCrc + cpCodeCrc).toUpperCase();
            Log.d(LOG_TAG, "getCodeCrc(): mCodeCrc[" + mCodeCrc + "]");

            return mCodeCrc;
        }

        return "error";
    }*/

    private HashMap<String, String> mSettingMap = null;

    private boolean isFileExist(String path)
    {
        if (path == null) return false;
        else return new File(path).exists();
    }

    private HashMap<String, String> loadSettingMap(String path)
    {
        if (mSettingMap != null) return mSettingMap;

        String logStr = "loadSettingMap(): path[" + path + "]";

        if (!isFileExist(path))
        {
            Log.e(LOG_TAG, logStr + " not exist");
            mSettingMap = null; return mSettingMap;
        }

        mSettingMap = new HashMap<String, String>();

        DocumentBuilderFactory Dbf;
        DocumentBuilder DocBld;
        Document document;
        Dbf = DocumentBuilderFactory.newInstance();

        try
        {
            DocBld = Dbf.newDocumentBuilder();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            mSettingMap = null; return mSettingMap;
        }

        try
        {
            document = DocBld.parse(new File(path));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            mSettingMap = null; return mSettingMap;
        }
        catch (SAXException e)
        {
            e.printStackTrace();
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            mSettingMap = null; return mSettingMap;
        }

        NodeList paramList = document.getElementsByTagName("parameter");
        String itemVal = null;

        for (int i = 0; i < paramList.getLength(); i++)
        {
            Node paramNode = paramList.item(i);
            NamedNodeMap attrs = paramNode.getAttributes();
            StringBuffer sumString = new StringBuffer();

            for (int j = 0; j < attrs.getLength(); j++)
            {
                String itemName = attrs.item(j).getNodeName();
                itemVal = attrs.item(j).getNodeValue();
                if (j == 0)
                    sumString.append(itemVal);
                else
                    sumString.append(":"+itemVal);
            }

            if (paramNode.getNodeType() == Node.ELEMENT_NODE)
            {
                String paramVal = null;
                try
                {
                    paramVal = paramNode.getFirstChild().getNodeValue();
                }
                catch (NullPointerException e)
                {
                    paramVal = "";
                }

                mSettingMap.put(sumString.toString(), paramVal);
            }
        }

        return mSettingMap;
    }

    private String getFpriCrc()
    {
        if (loadSettingMap("/etc/settings.xml") == null)
        {
            Log.e(LOG_TAG, "getFpriCrc(): loadSettingMap() fail!");
            return "error";
        }

        CRC32 crc = new CRC32();

        updateCrcBySettings(crc);

        String crcHex = Long.toHexString(crc.getValue()).toUpperCase();

        return crcHex;
    }

    private void updateCrcBySettings(CRC32 crc)
    {
        final ContentResolver resolver = mContext.getContentResolver();
        Iterator iter = mSettingMap.keySet().iterator();

        for (int i = 0; i < mSettingMap.size(); i++)
        {
            String key = iter.next().toString();
            String keys[] = key.split(":");
            String name = null;
            String value = null;
            Uri uri = null;

            if (keys[0].equals("system"))
            {
                uri = Settings.System.CONTENT_URI;
                name = keys[1];
                value = getContentValue(resolver, uri, name);
            }
            else if (keys[0].equals("secure"))
            {
                uri = Settings.Secure.CONTENT_URI;
                name = keys[1];
                value = getContentValue(resolver, uri, name);
            }

            if (value != null && value.length() > 0)
            {
                Log.d(LOG_TAG, "updateCrcBySettings(): name[" + name + "], value[" + value + "]");
                crc.update(value.getBytes());
            }
        }
    }

    private String getContentValue(ContentResolver resolver, Uri uri, String name)
    {
        Cursor cursor = null;
        String result = null;

        try
        {
            cursor = resolver.query(
                Settings.NameValueTable.getUriFor(uri, name),
                new String[] { "value" },
                null, null, null);

            if (cursor == null || cursor.getCount() <= 0) return "";

            cursor.moveToFirst();
            result = cursor.getString(0);
        }
        finally
        {
            if (cursor != null) try { cursor.close(); } catch (Exception e) { ; }
        }

        return result;
    }

    private ArrayList<String> mEfsFiles = null;

    private ArrayList<String> loadEfsFiles()
    {
        if (mEfsFiles != null) return mEfsFiles;

        mEfsFiles = new ArrayList<String>(128);

        try
        {
            String path;
            String ptr;

            path = "/etc/efs.txt";
            LineNumberReader reader = new LineNumberReader(new FileReader(path));

            while ((ptr = reader.readLine()) != null)
            {
                if (ptr.length() > 0)
                {
                    String logStr = "loadEfsFiles: path[" + ptr + "]";

                    if (!isFileExist(ptr))
                    {
                        Log.e(LOG_TAG, logStr + " not exist");
                        mEfsFiles = null;
                        break;
                    }

                    Log.d(LOG_TAG, logStr + " added");
                    mEfsFiles.add(ptr);
                }
            }

            reader.close();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "loadEfsFiles(): exception[" + e + "]");
            mEfsFiles = null;
        }

        return mEfsFiles;
    }

    private String getFileCrc()
    {
        if (loadEfsFiles() == null) return "error";

        CRC32 crc = new CRC32();

        if (!updateCrcByFileSize(crc)) return "error";

        String crcHex = Long.toHexString(crc.getValue()).toUpperCase();

        return crcHex;
    }

    private boolean updateCrcByFileSize(CRC32 crc)
    {
        if (mEfsFiles == null) return false;

        try
        {
            for (int i = 0; i < mEfsFiles.size(); i++)
            {
                File file = new File(mEfsFiles.get(i));
                String path = file.getAbsolutePath();

                if (!file.exists())
                {
                    Log.e(LOG_TAG, "updateCrcByFileSize(): path[" + path + "] not exist");
                    return false;
                }

                Log.d(LOG_TAG, "updateCrcByFileSize(): path[" + path + "], length[" + file.length() + "]");
                crc.update((int)file.length());
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "updateCrcByFileSize(): exception[" + e + "]");
            return false;
        }

        return true;
    }

/*
    private String getHardwareVersion()
    {
        if (mHardwareVersion != null) return mHardwareVersion;

        if (sendATCommand("AT%HWVER"))
        {
            Log.d(LOG_TAG, "sendATCommand success: mData["+mData[0]+"]");

            mHardwareVersion = mData[0];
        }
        else
        {
            Log.d(LOG_TAG, "sendATCommand fail");
            mHardwareVersion = null;
            return "error";
        }

        return mHardwareVersion;
    }

    private String getSoftwareVersion()
    {
        if (mSoftwareVersion != null) return mSoftwareVersion;

        try
        {
            if (sendATCommand("AT%SWV"))
            {
                String swv = mData[0].substring(1, mData[0].length() - 1);

                StringTokenizer st = new StringTokenizer(swv, "+");
                mSoftwareVersion = st.nextToken();

                return mSoftwareVersion;
            }
            else
            {
                return "error";
            }
        }
        catch (Exception e)
        {
            return "error";
        }
    }

    private String checkSoftwareVersion()
    {
        String apSoftwareVersion;
        String cpSoftwareVersion;
        String logStr = "checkSoftwareVersion():";

        String swv = getSoftwareVersion(); // software version string returned by AT%SWV from CP
        if (swv == null || swv.equals("error")) return "error";
        logStr = logStr + " swv[" + swv + "]";

        // get cpSoftwareVersion
        try
        {
            String[] tokens = swv.split("-");
            if (tokens.length < 8) return "error";
            cpSoftwareVersion = tokens[2].toUpperCase();
            logStr = logStr + " cpSoftwareVersion[" + cpSoftwareVersion + "]";
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            return "error";
        }

        // get apSoftwareVersion
        try
        {
            String lgeSoftwareVersion = Build.VERSION.LGE;
            apSoftwareVersion = lgeSoftwareVersion.substring(lgeSoftwareVersion.lastIndexOf("-") + 1).toUpperCase();
            logStr = logStr + " apSoftwareVersion[" + apSoftwareVersion + "]";
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, logStr + " exception[" + e + "]");
            return "error";
        }

        Log.d(LOG_TAG, logStr);

        if (apSoftwareVersion.equals(cpSoftwareVersion)) return "ok";
        else return "error";
    }

    private String getPhoneNumber()
    {
        if (mPhoneNumber != null) return mPhoneNumber;

        if (sendATCommand("AT+CNUM"))
        {
            Log.d(LOG_TAG, "sendATCommand success: mData["+mData[0]+"]");

            try
            {
                // +CNUM: "","+821022114981",145
                mPhoneNumber = mData[0].substring(mData[0].indexOf(",") + 2);
                // +821022114981",145
                mPhoneNumber = mPhoneNumber.substring(0, mPhoneNumber.indexOf("\""));
                // +821022114981

                Log.d(LOG_TAG, "extracted phone number is [" + mPhoneNumber + "]");

                mPhoneNumber = mStx + mPhoneNumber + mEtx;
            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "getPhoneNumber(): exception[" + e + "]");
                mPhoneNumber = null;
                return "error";
            }
        }
        else
        {
            Log.d(LOG_TAG, "sendATCommand fail");
            mPhoneNumber = null;
            return "error";
        }

        return mPhoneNumber;
    }

    private String getMncCode()
    {
        return Andy_Flex.getMncCode();
    }

    private String getMccCode()
    {
        return Andy_Flex.getMccCode();
    }*/

// 101110, chihyun.kim@lge.com WEBDN [START]
    private String webDownloadChange()
    {
        try
        {
           PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
           pm.reboot("webDownloadChange");
        }
        catch (Exception e)
        {
           Log.e(LOG_TAG, "pm.reboot() failed: " + e);
           return "error";
        }
        return "true";
    }
// 101110, chihyun.kim@lge.com WEBDN [END]

    private static ITelephony mPhoneService = null;

    private static ITelephony getPhoneService()
    {
        if (mPhoneService != null) return mPhoneService;

        int tryCount = 0;
        while (true)
        {
            tryCount = tryCount + 1;
            Log.d(LOG_TAG, "getPhoneService(): tryCount[" + tryCount + "]");
            mPhoneService = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (mPhoneService != null) break;
            else SystemClock.sleep(3000);
        }

        return mPhoneService;
    }

    /*private static ATService mATService = null;

    private static ATService getATService()
    {
        if (mATService != null) return mATService;

        int tryCount = 0;
        while (true)
        {
            tryCount = tryCount + 1;
            Log.d(LOG_TAG, "getATService(): tryCount[" + tryCount + "]");
            mATService = ATService.getDefault();
            if (mATService != null && mATService.checkATService()) break;
            else SystemClock.sleep(3000);

            if (tryCount > 5) return null;
        }

        return mATService;
    }

    private boolean sendATCommand(String command) // fix WBT#170918, do not apply syncronized to this method
    {
        Log.d(LOG_TAG, "sendATCommand(): command[" + command + "]");

        synchronized (mLock)
        {
            try
            {
                Message m = mHandler.obtainMessage(SEND_COMMAND_RESPONSE);

                mResp = false;
                getATService().sendCommand(command, m);
                mLock.wait(1000);

                return mResp;
            }
            catch (Exception e)
            {
                Log.d(LOG_TAG, "sendATCommand(): exception[" + e + "]");
                return false;
            }
        }
    }*/

    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            synchronized (mLock)
            {
                try
                {
                    switch (msg.what)
                    {
                        case SEND_COMMAND_RESPONSE:/*
                            ATResponse response = new ATResponse((String)msg.obj);
                            Log.d(LOG_TAG, "handleMessage: response[" + response + "]");
                            mData = response.getData();
                            if (mData != null && mData.length > 0) mResp = true;
                            else mResp = false;
                            mLock.notifyAll();*/
                            break;
                        default:
                            super.handleMessage(msg);
                    }
                }
                catch (Exception e)
                {
                    Log.e(LOG_TAG, "handleMessage(): exception[" + e + "]");
                }
            }
        }
    };

    private void createContextFile(String name)
    {
        if (mContext.getFileStreamPath(name).exists())
        {
            Log.d(LOG_TAG, "createContextFile(): name[" + name + "] already exists");
            return;
        }

        FileOutputStream stream = null;
        try
        {
            stream = mContext.openFileOutput(name, Context.MODE_PRIVATE);
            if (stream != null)
            {
                stream.write(1); // content is meaningless
                stream.flush();
                stream.close();
            }

            Log.d(LOG_TAG, "createContextFile(): name[" + name + "] created");
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "createContextFile(): exception[ " + e + "]");
            if (stream != null) try { stream.close(); } catch (Exception ce) { ; }
        }
    }

    private void deleteContextFile(String name)
    {
        if (!mContext.getFileStreamPath(name).exists())
        {
            Log.d(LOG_TAG, "deleteContextFile(): name[" + name + "] is not exists");
            return;
        }

        try
        {
            mContext.getFileStreamPath(name).delete();
            Log.d(LOG_TAG, "deleteContextFile(): name[" + name + "] deleted");
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "deleteContextFile(): exception[ " + e + "]");
        }
    }
}
