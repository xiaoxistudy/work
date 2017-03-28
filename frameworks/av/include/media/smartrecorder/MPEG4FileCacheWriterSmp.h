
#ifndef MPEG4_FILE_CACHE_WRITER_SMP_H_
#define MPEG4_FILE_CACHE_WRITER_SMP_H_
#define USE_SECTION_SAVE_FILE 

namespace android {

#define PERFORMANCE_PROFILE //for performance  profile
#define DEFAULT_FILE_CACHE_SIZE 512*1024

#define TEST_BAD_PERFORMANCE //for moniter the sdcard write bitrate

class MPEG4WriterSmp;
#ifdef USE_SECTION_SAVE_FILE
class MPEG4FileCacheWriterEx;
#endif
class MPEG4FileCacheWriterSmp{
public:
	//MPEG4FileCacheWriterSmp();
	MPEG4FileCacheWriterSmp(int fd, size_t cachesize = DEFAULT_FILE_CACHE_SIZE);

	virtual ~MPEG4FileCacheWriterSmp();

	bool isFileOpen();
    bool startFileLimitCheckThread();
	size_t write(const void *data, size_t size, size_t num);

	int seek(off64_t offset, int refpos);

	int close();

	bool getFile();

	void setOwner(MPEG4WriterSmp *owner);
#ifdef PERFORMANCE_PROFILE
	void getPerformanceInfo(int64_t* total_time = NULL, int64_t* max_time = NULL, int64_t* times_of_write = NULL);
#endif
#ifdef USE_SECTION_SAVE_FILE
    friend class MPEG4FileCacheWriterEx;
    MPEG4FileCacheWriterEx *mCacheWriterEx;
#endif
	pthread_t	mFileLimitCheckThrd;
	pthread_t	mSDcardRWspeedCheckThrd;
    bool        mFileLimitThrdExit;
    bool        mSdcardRWspeedcheckThrdExit;
    MPEG4WriterSmp* mOwner;
    int mFd;
	Mutex mSyncLock;
	Mutex       msdcardRWLock;
	Mutex       msdcardRWwaitLock;
    Condition mdelayTimeCondition;
    Condition   mCheckSDRWspeedCondition;
    Condition   mSDRWwaitCondition;
private:

	//inline status_t flush();
	status_t flush();
	void* mpCache;

	size_t mCacheSize;

	size_t mDirtySize;

	bool mFileOpen;

	//bool mWriteDirty;

    
	size_t mClusterAlignLeftSize;
	long mClusterSize;

#ifdef PERFORMANCE_PROFILE
	int64_t mTotaltime;
	int64_t mMaxtime;
	int64_t mTimesofwrite;
	int64_t mTotalWrite;

#ifdef TEST_BAD_PERFORMANCE
	int64_t mTestDelayFreq;
	int64_t mTestDelayTimeUs;
#endif
#endif

};

}  // namespace android

#endif // MPEG4_FILE_CACHE_WRITER_H_


