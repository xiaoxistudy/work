/*
* Copyright (C) 2016 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/


#include "CarcorderCollision.h"


void collision_init(){
    last_notify_timestamp=0;
    notify_interval=ms2ns(500);
    is_lateral_init=false;
}

bool collision_detect(ASensorEvent value,const float* threshold,int size,int level,int* result)
{
     if(size<3){
	 	ALOGW("size(%d) is less than 3.",size);
        return false;
     }
     float gsensor_threshold_x=threshold[0];
     float gsensor_threshold_y=threshold[1];
     float gsensor_threshold_z=threshold[2];
     if (value.type == ASENSOR_TYPE_ACCELEROMETER) 
	 {
		 if(!is_lateral_init){
             set_lateral(value.data[0],value.data[1],value.data[2]);
			 is_lateral_init=true;
		 }
         float absolute_x=fabsf(value.data[0]-last_lateral_x);
		 float absolute_y=fabsf(value.data[1]-last_lateral_y);
		 float absolute_z=fabsf(value.data[2]-last_lateral_z);
		
		if(absolute_x> gsensor_threshold_x || \
		   absolute_y> gsensor_threshold_y || \
		   absolute_z> gsensor_threshold_z)
        {
           if((value.timestamp - last_notify_timestamp) > notify_interval)
           {
                last_notify_timestamp = value.timestamp;
				*result=1;
				set_lateral(value.data[0],value.data[1],value.data[2]);
				ALOGD("CarcorderCollision:%lld\t%8f\t%8f\t%8f\t\n",last_notify_timestamp,absolute_x, absolute_y, absolute_z);
                return true;
           }
        }
		set_lateral(value.data[0],value.data[1],value.data[2]);
     }
    
     return false;
}

void set_lateral(float lateral_x,float lateral_y,float lateral_z)
{
   last_lateral_x=lateral_x;
   last_lateral_y=lateral_y;
   last_lateral_z=lateral_z;
}



