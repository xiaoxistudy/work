#ifndef MPEG4_FILE_CACHE_WRITER_EX_H_
#define MPEG4_FILE_CACHE_WRITER_EX_H_
#define USE_SECTION_SAVE_FILE

#ifdef USE_SECTION_SAVE_FILE
namespace android {

class MPEG4FileCacheWriterEx{
	public:
		MPEG4FileCacheWriterEx();
		virtual ~MPEG4FileCacheWriterEx();
		int overwriter_close(int next_mfd);
		int init(int next_mfd);
		void setOwner(MPEG4FileCacheWriterSmp *owner);
	private:
		MPEG4FileCacheWriterSmp* mOwner;
		};
}
#endif
#endif // MPEG4_FILE_CACHE_WRITER_H_
