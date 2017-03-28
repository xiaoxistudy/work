
#define LOG_TAG "ipoboot"

#include <string.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <stdio.h>
#include <utils/Log.h>
#include <sys/resource.h>
#include <cutils/properties.h>
#include <linux/input.h>
#include <sys/ioctl.h>


int main()
{
  
    //void (*libipod_exit)(int reason) = NULL;
    //void *handle;

    ALOGE("ipoboot bootup start");
    
    property_set("smswakeup.ipobootup", "0");
        
    system("sendevent /dev/input/event0 1 116 1");
    system("sendevent /dev/input/event0 0 0 0");
    usleep(700*1000);
    system("sendevent /dev/input/event0 1 116 0");
    system("sendevent /dev/input/event0 0 0 0");
    
    ALOGE("ipoboot bootup end");
 /*   
    handle = dlopen("/system/lib/libipod.so", RTLD_NOW);
    if (handle == NULL) {
        ALOGE("Can't load ipod library: %s", dlerror());
    } else {
        //if ((libipod_setup = (void (*)(struct ipod_param *))dlsym(handle, "libipod_setup")) == NULL) {
        
        //}

        if ((libipod_exit = (void (*)(int))dlsym(handle, "exit_ipod")) == NULL) {
            ALOGE("exit_ipod error: %s", dlerror());
        } else {
        
            libipod_exit(0);
            ALOGE("ipoboot bootup end");
        }
        dlclose(handle);
    }
    */
}
