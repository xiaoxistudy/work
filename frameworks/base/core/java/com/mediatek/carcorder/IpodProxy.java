package com.mediatek.carcorder;

import android.util.Log;



public class IpodProxy{
	
    private static final String TAG = "IpodProxy";
	private static CarcorderManager mCarcorderManager;
	private static IpodProxy mIpodProxy;
	 
	private IpodProxy(){   }

	public synchronized static IpodProxy getInstance(CarcorderManager carcorderManager){
       if(carcorderManager==null){  //we do not allow carcorderManager is null
            Log.e(TAG,"param carcorderManager is null.");
			return null;
       }
	   
	   if(mIpodProxy==null){
           mIpodProxy=new IpodProxy();
       	}
	   mCarcorderManager=carcorderManager;
	   return mIpodProxy;
	}
	
	/**
	*  a Parameter
	*/
    private class ProxyParameter{
       private static final String PARAM_TYPE_END =    "0";
	   private static final String PARAM_TYPE_INT =    "1";
	   private static final String PARAM_TYPE_LONG =   "2";
	   private static final String PARAM_TYPE_FLOAT =  "3";
	   private static final String PARAM_TYPE_DOUBLE = "4";
	   private static final String PARAM_TYPE_STRING = "5";

	   private StringBuilder params = new StringBuilder(128);
	   
       public void writeBoolean(boolean param){
           if(param){
              writeInt(1);
           }else{
              writeInt(0);
           }
       }
	   public void writeInt(int param){
	   	   write(PARAM_TYPE_INT,Integer.toString(param));
	   }

	   public void writeLong(long param){
           write(PARAM_TYPE_LONG,Long.toString(param));
	   }
	   public void writeFolat(float param){
           write(PARAM_TYPE_FLOAT,Float.toString(param));
	   }
	   public void writeDouble(double param){
           write(PARAM_TYPE_DOUBLE,Double.toString(param));
	   }
	   public void writeString(String param){
           write(PARAM_TYPE_STRING,param);
	   }

	   public void write(String type,String param){
           params.append(type);
		   params.append("=");
		   params.append(param);
		   params.append(";");
	   }

	  public String toString(){
		 return params.toString()+PARAM_TYPE_END;
	  }
		
    }
	/**
	*call ipod function result
	*/
	public static final int RESULT_SUCCESS = 0;               //had exec right now
	public static final int RESULT_PARAMS_SAVED = 1;          //params had save in service
	public static final int RESULT_PARAMS_REPLACED = 2;       //params had replace and save in service
	public static final int RESULT_FAIL = -1;                 //unknow fail
	public static final int RESULT_EXCEPTION = -2;            //happend exception,maybe binder had die
	
    /**
	 *  define ipod function ,we can call
	 */
    private static final String DO_SHUTDOWN = "do_shutdown";  /*control real shutdown or not*/
	private static final String SET_REBOOT_TIME_SLOT = "set_reboot_time_slot";  /*set a time for a fixer time make the system reboot*/
    private static final String SET_REBOOT_CONTROL = "set_reboot_control";
    private static final String EXIT_IPOD = "exit_ipod";

	
	/*control real shutdown or not*/
	public int doShutdown(String reason, boolean isShutdown){
        
		try{
			ProxyParameter param = new ProxyParameter();
            param.writeString(reason);
			param.writeBoolean(isShutdown);			
			return mCarcorderManager.postIpodCommand(DO_SHUTDOWN,param.toString());	
        }catch(Exception e){
           e.printStackTrace();
        }
		return RESULT_EXCEPTION;
	}

  /*set a time for a fixer time make the system reboot*/
	public int setRebootTimeSlot(int slotTime){
      try{
			ProxyParameter param = new ProxyParameter();
            param.writeInt(slotTime);
			return mCarcorderManager.postIpodCommand(SET_REBOOT_TIME_SLOT,param.toString());	
        }catch(Exception e){
           e.printStackTrace();
        }
		return RESULT_EXCEPTION;
	}
  
   /*control the feature of reboot is on or off*/
   public int setRebootControl(int flag){
       try{
			ProxyParameter param = new ProxyParameter();
            param.writeInt(flag);
			return mCarcorderManager.postIpodCommand(SET_REBOOT_CONTROL,param.toString());	
        }catch(Exception e){
           e.printStackTrace();
        }
		return RESULT_EXCEPTION;
    }
   
   public int exitIpod(int reason){
	    try{
		   ProxyParameter param = new ProxyParameter();
		   param.writeInt(reason);
		   return mCarcorderManager.postIpodCommand(EXIT_IPOD,param.toString());   
	    }catch(Exception e){
			  e.printStackTrace();
	    }
	    return RESULT_EXCEPTION;
	}
   
}

