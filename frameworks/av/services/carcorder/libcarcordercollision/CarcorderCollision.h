
/*
* Copyright (C) 2016 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

#ifndef ANDROID_CARCORDER_COLLISION_H
#define ANDROID_CARCORDER_COLLISION_H

#include <math.h>
#include <stdlib.h>
#include <stdint.h>
#include <sys/types.h>
#include <gui/Sensor.h>
#include <android/sensor.h>
#include <utils/Log.h>


/**
int64_t define in stdint.h
fabsf() define in math.h
need extern "C" for symbol
*/

int64_t last_notify_timestamp;
int64_t notify_interval;
float last_lateral_x;
float last_lateral_y;
float last_lateral_z;
bool is_lateral_init;


extern "C" void collision_init();

extern "C" bool collision_detect(ASensorEvent value,const float* threshold,int size,int level,int* result);

void set_lateral(float lateral_x,float lateral_y,float lateral_z);

#endif
