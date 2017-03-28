
#ifndef MPEG2TS_FILE_CACHE_WRITER_SMP_H_
#define MPEG2TS_FILE_CACHE_WRITER_SMP_H_
#define USE_SECTION_SAVE_FILE 

namespace android {

#define PERFORMANCE_PROFILE //for performance  profile
#define DEFAULT_FILE_CACHE_SIZE 128*1024
#define MPEG2TSWRITER_DATA_WRITED_SIZE    30*1024*1024LL //default writed size

class MPEG2TSWriterSmp;

class MPEG2TSFileCacheWriterSmp{
public:

    MPEG2TSFileCacheWriterSmp(MPEG2TSWriterSmp *owner, int fd, size_t cachesize);

	virtual  ~MPEG2TSFileCacheWriterSmp();

	bool     isFileOpen();

	size_t   cacheWrite(const void *data, size_t size);

	int      cacheClose();

    status_t cacheFlush();
    void     cacheReset(int fd);

	bool     getFile();
    void     setSDCardCheckThreadExt();
    void     waitSDCardCheckThreadExt();


    MPEG2TSWriterSmp* mOwner;
	pthread_t	mSDCardMemoryCheckThrd;
	Condition 	mSDCardCheckThreadExitCondition;
    bool        mSDCardCheckThreadExit;
    bool        mSDCardCheckThreadStarted;
    Mutex       mLock;  


private:
	
	void*  mpCache;
	size_t mCacheSize;
	size_t mDirtySize;
	bool   mFileOpen;
    int    mFd;
    //FILE*  mFile;

#ifdef PERFORMANCE_PROFILE
	int64_t mTotalTime;
	int64_t mTotalWrite;
	int64_t mWriteNum;
    int64_t mLeftSize;
#endif

};

}  // namespace android

#endif // MPEG2TS_FILE_CACHE_WRITER_H_


