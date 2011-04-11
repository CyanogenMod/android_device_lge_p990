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

#include <bridge/IBridgeService.h>
#include <bridge/Bridge.h>

#include <utils/Log.h>

#undef LOG_TAG
#define LOG_TAG "libbridge"

namespace android
{

static sp<IBridgeService> gBridgeService;

static bool init()
{
    if (gBridgeService != NULL) return true;

    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> binder = sm->getService(String16("bridge"));
    gBridgeService = interface_cast<IBridgeService>(binder);

    if (gBridgeService != NULL) return true;
    else return false;
}

}; // namespace android

extern "C" const char* sendCommand(const char *command)
{
    int i;
    char *hex;
    char *p;

    if (!android::init()) return NULL;

    return (char *)android::gBridgeService->sendCommand(command);
}
