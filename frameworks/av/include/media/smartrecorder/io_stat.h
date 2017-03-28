/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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
#include <unistd.h>
#include <string.h>
#include <utils/Log.h>
#include <cutils/xlog.h>

#define MAX_PF_NAME            1024

namespace android{
struct io_stats {
    /* # of sectors read */
    unsigned long rd_sectors    __attribute__ ((aligned (8)));
    /* # of sectors written */
    unsigned long wr_sectors    __attribute__ ((packed));
    /* # of read operations issued to the device */
    unsigned long rd_ios        __attribute__ ((packed));
    /* # of read requests merged */
    unsigned long rd_merges     __attribute__ ((packed));
    /* # of write operations issued to the device */
    unsigned long wr_ios        __attribute__ ((packed));
    /* # of write requests merged */
    unsigned long wr_merges     __attribute__ ((packed));
    /* Time of read requests in queue */
    unsigned int  rd_ticks      __attribute__ ((packed));
    /* Time of write requests in queue */
    unsigned int  wr_ticks      __attribute__ ((packed));
    /* # of I/Os in progress */
    unsigned int  ios_pgr       __attribute__ ((packed));
    /* # of ticks total (for this device) for I/O */
    unsigned int  tot_ticks     __attribute__ ((packed));
    /* # of ticks requests spent in queue */
    unsigned int  rq_ticks      __attribute__ ((packed));
};

class SdcardIO{
   public:
    SdcardIO();
    virtual ~SdcardIO();
    
    int read_sysfs_file_stat(char *filename, struct io_stats *sdev);
    
    void set_sysfs_file_name(const char *name);
    
    unsigned int calc_disk_speed(struct io_stats sdev1, struct io_stats  sdev2);
        
    char filename[MAX_PF_NAME];
};
}
