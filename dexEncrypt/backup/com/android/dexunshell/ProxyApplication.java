package com.android.dexunshell;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

public class ProxyApplication extends Application {


//    private static final String appkey = "APPLICATION_CLASS_NAME";
    private String apkFileName;
    private String odexPath;
    private String libPath;
    private MyClassLoader dLoader;

//
//    protected void attachBaseContext(Context base) {
//        super.attachBaseContext(base);
//        try {
//
//
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }


    public void onCreate() {
        Log.v(this.getClass().getName(),"先有aplication");
        {
            try {
                File odex = this.getDir("payload_odex", MODE_PRIVATE);
                File dex = this.getDir("payload_dex", MODE_PRIVATE);
                File buf = this.getDir("payload_buf", MODE_PRIVATE);
                File libs = this.getDir("lib", MODE_PRIVATE);
                
                odexPath = odex.getAbsolutePath();
                Log.e(this.getClass().getName(),"壳工程odexPath----"+odexPath);
                libPath = libs.getAbsolutePath();
                Log.e(this.getClass().getName(),"壳libPath----"+libPath);
                apkFileName = dex.getAbsolutePath() + "/classes.dex";
                Log.e(this.getClass().getName(),"壳工程apkFileName----"+apkFileName);
                String bufName = buf.getAbsolutePath() + "/buffer.dex";
                File dexFile = new File(apkFileName);
                if (!dexFile.exists())
                    dexFile.createNewFile();
                File bufFile = new File(bufName);
                if (!bufFile.exists())
                	bufFile.createNewFile();
                RandomAccessFile bufile = new RandomAccessFile(bufName, "rw");
                // 读取程序classes.dex文件
                readDexFileFromApk(bufile);
                // 分离出解壳后的apk文件已用于动态加载
                this.splitPayLoadFromDex(bufile);
                bufile.close();
                // 配置动态加载环境
                Object currentActivityThread = RefInvoke.invokeStaticMethod(
                        "android.app.ActivityThread", "currentActivityThread",
                        new Class[]{}, new Object[]{});
                String packageName = this.getPackageName();
                Log.e(this.getClass().getName(),"壳创建activitycontext----"+packageName);
                Map mPackages = (Map) RefInvoke.getFieldOjbect(
                        "android.app.ActivityThread", currentActivityThread,
                        "mPackages");
                WeakReference wr = (WeakReference) mPackages.get(packageName);
                dLoader = new MyClassLoader(apkFileName, odexPath,
                        libPath, (ClassLoader) RefInvoke.getFieldOjbect(
                        "android.app.LoadedApk", wr.get(), "mClassLoader"));
                Log.e(this.getClass().getName(),"壳创建DexClassLoader----"+packageName);
                RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader",
                        wr.get(), dLoader);



                    Log.e(this.getClass().getName(),"壳设置DexClassLoader----"+packageName);

                // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
                String appClassName = null;

                ApplicationInfo ai = this.getPackageManager()
                        .getApplicationInfo(this.getPackageName(),
                                PackageManager.GET_META_DATA);
                Log.e(this.getClass().getName(),"壳获取applicationinfo----"+packageName);
                Bundle bundle = ai.metaData;
                Log.e(this.getClass().getName(),"壳获取bundle----"+packageName);
                if (bundle != null
                        && bundle.containsKey("APPLICATION_CLASS_NAME")) {
                    appClassName = bundle.getString("APPLICATION_CLASS_NAME");
                } else {
                    return;
                }

                Log.e(this.getClass().getName(),"替换context----"+appClassName);
                Application app = (Application)dLoader.loadClass(appClassName).newInstance();
                Log.e(this.getClass().getName(),"壳替换加载application----"+appClassName);
                RefInvoke.setFieldOjbect("android.app.ContextImpl", "mOuterContext", this.getBaseContext(), app);
                RefInvoke.setFieldOjbect("android.content.ContextWrapper", "mBase", app, this.getBaseContext());

                Log.e(this.getClass().getName(),"将app设定到binddata中----android.app.ActivityThread->mInitialApplication");
                Object mBoundApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mBoundApplication");
                Object info = RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData", mBoundApplication, "info");
                RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication", info, app);

                Log.e(this.getClass().getName(),"获得旧的app对象----android.app.ActivityThread->mInitialApplication");
                Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mInitialApplication");

                Log.e(this.getClass().getName(),"将新的app放到app顶部中----android.app.ActivityThread->mInitialApplication");
                RefInvoke.setFieldOjbect("android.app.ActivityThread", "mInitialApplication", currentActivityThread, app);

                Log.e(this.getClass().getName(),"获得app列表,并将新的app加入到app列表中----android.app.ActivityThread->mAllApplications");
                ArrayList<Application> mAllApplications = (ArrayList<Application>)RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mAllApplications");
                mAllApplications.remove(oldApplication);
                mAllApplications.add(app);

                Log.e(this.getClass().getName(),"获得资源列表----android.app.ActivityThread->mProviderMap");
                Map mProviderMap = (Map) RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mProviderMap");

//            Object mBoundApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,"mBoundApplication");
//            Object loadedApkInfo = RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData",mBoundApplication, "info");
//            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication",loadedApkInfo, null);
//            Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,"mInitialApplication");
//            ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke.getFieldOjbect("android.app.ActivityThread",
//                            currentActivityThread, "mAllApplications");
//            mAllApplications.remove(oldApplication);
//            ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke
//                    .getFieldOjbect("android.app.LoadedApk", loadedApkInfo,
//                            "mApplicationInfo");
//            ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke
//                    .getFieldOjbect("android.app.ActivityThread$AppBindData",
//                            mBoundApplication, "appInfo");
//
//            appinfo_In_LoadedApk.className = appClassName;
//            appinfo_In_AppBindData.className = appClassName;
////            Application app = (Application) RefInvoke.invokeMethod(
////                    "android.app.LoadedApk", "makeApplication", loadedApkInfo,
////                    new Class[]{boolean.class, Instrumentation.class},
////                    new Object[]{false, null});
//            RefInvoke.setFieldOjbect("android.app.ActivityThread",
//                    "mInitialApplication", currentActivityThread, app);


//            Map mProviderMap =  (Map)RefInvoke.getFieldOjbect(
//                    "android.app.ActivityThread", currentActivityThread,
//                    "mProviderMap");
                int count=0;



                Iterator it = mProviderMap.values().iterator();

                while (it.hasNext()) {
                    Object providerClientRecord = it.next();
                    Object localProvider = RefInvoke.getFieldOjbect(
                            "android.app.ActivityThread$ProviderClientRecord",
                            providerClientRecord, "mLocalProvider");
                    RefInvoke.setFieldOjbect("android.content.ContentProvider",
                            "mContext", localProvider, app);
                    count++;
                }
                Log.e(this.getClass().getName(),"添加资源到资源到----provider"+mProviderMap.size());
                app.onCreate();
                Log.v(this.getClass().getName(),app.getClass().getName());
                Log.v(this.getClass().getName(),"lv.keexample.CApplication");
                Log.v(this.getClass().getName(),appClassName);
//                RefInvoke.invokeMethod(appClassName, "onCreate", app, new Class[]{}, new Object[]{});
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void splitPayLoadFromDex(RandomAccessFile bufile) throws IOException {
//        
        long ablen = bufile.length();
        bufile.seek(ablen-4);
        
//        byte[] dexlen = bufile.re;
//        System.arraycopy(bufile, ablen - 4, dexlen, 0, 4);
//        ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
//        DataInputStream in = new DataInputStream(bais);
        byte[] intbuf = new byte[4];
        bufile.read(intbuf);
        int readInt = byteToInt(intbuf);
        Log.e(this.getClass().getName(),"壳解出前classes得长度"+readInt);
//        System.out.println(Integer.toHexString(readInt));
//        byte[] newdex = new byte[readInt];
//        System.arraycopy(bufile, ablen - 4 - readInt, newdex, 0, readInt);
//        byte[] apkdata = decrypt(newdex);
//        Log.e(this.getClass().getName(),"壳解出解密后classes得长度"+apkdata.length);
        RandomAccessFile file = new RandomAccessFile(apkFileName,"rw");
        bufile.seek(ablen-4-readInt);
        int totallen=0;
        int blocklen=0;
        byte[] buf_e,buf_d;
        while(totallen<readInt){
        	bufile.read(intbuf);
        	blocklen = byteToInt(intbuf);
        	Log.e(this.getClass().getName(),"壳解出前block得长度"+blocklen);
        	buf_e=new byte[blocklen];
        	bufile.read(buf_e);
        	buf_d=decrypt(buf_e,0,blocklen);
        	Log.e(this.getClass().getName(),"壳解出后block得长度"+buf_d.length);
        	file.write(buf_d);
        	totallen+=blocklen+4;
        	Log.e(this.getClass().getName(),"壳解出后总得长度"+totallen+","+readInt);
        }
        Log.e(this.getClass().getName(),"壳解出后文件得长度"+file.length());
        file.close();
//        try {
//        	Log.e(this.getClass().getName(),"壳解出解密后file得长度"+file.length());
//            FileOutputStream localFileOutputStream = new FileOutputStream(file);
//            localFileOutputStream.write(apkdata);
//            localFileOutputStream.close();
//            Log.e(this.getClass().getName(),"壳写文件后file得长度"+file.length());
            
//        byte[] buffer = new byte[1024];
//
//        File file = new File(apkFileName);
//        try {
//            int len=0;
//            int bl=0;
//            int redus=0;
//            FileOutputStream localFileOutputStream = new FileOutputStream(file);
//            Log.e(this.getClass().getName(), "壳classes写入文件" + apkFileName);
//            while (len<readInt){
//                redus=readInt-len;
//                bl=redus>1024?1024:redus;
//                System.arraycopy(apkdata, ablen - 4 - readInt-len, buffer, 0, bl);
//                localFileOutputStream.write(buffer);
//                len+=bl;
//            }
//            Log.e(this.getClass().getName(), "壳classes写入文件结束"+len);
//            localFileOutputStream.flush();
//            localFileOutputStream.close();


//        } catch (IOException localIOException) {
//            localIOException.printStackTrace();
//            throw new RuntimeException(localIOException);
//        }
//        ZipInputStream localZipInputStream = new ZipInputStream(
//                new BufferedInputStream(new FileInputStream(file)));
//        Log.e(this.getClass().getName(),"壳获取壳名称----"+file.getName());
//        while (true) {
//            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
//            if (localZipEntry == null) {
//                localZipInputStream.close();
//                break;
//            }
//            String name = localZipEntry.getName();
//            Log.e(this.getClass().getName(),"壳获取壳名称----provider"+name);
//            if (name.startsWith("lib/") && name.endsWith(".so")) {
//                File storeFile = new File(libPath + "/"
//                        + name.substring(name.lastIndexOf('/')));
//                storeFile.createNewFile();
//                FileOutputStream fos = new FileOutputStream(storeFile);
//                byte[] arrayOfByte = new byte[1024];
//                while (true) {
//                    int i = localZipInputStream.read(arrayOfByte);
//                    if (i == -1)
//                        break;
//                    fos.write(arrayOfByte, 0, i);
//                }
//                fos.flush();
//                fos.close();
//            }
//            localZipInputStream.closeEntry();
//        }
//        localZipInputStream.close();


    }

    /**
     * 读取包中classes
     * @param bufile 
     * @return
     * @throws IOException
     */
    private void readDexFileFromApk(RandomAccessFile bufile) throws IOException {
    	
//        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(
                        this.getApplicationInfo().sourceDir)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
//            Log.e(this.getClass().getName(),"壳读取classes----readDexFileFromApk"+localZipEntry.getName());
            if (localZipEntry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    bufile.write(arrayOfByte, 0, i);
                }
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
//        return dexByteArrayOutputStream.toByteArray();
    }
    
    public static int byteToInt(byte[] _data){
        int l1 = (((int)_data[0])<<24)&0xff000000;
        int l2 = (((int)_data[1])<<16)&0x00ff0000;
        int l3 = (((int)_data[2])<<8)&0x0000ff00;
        int l4 = (_data[3])&0x000000ff;
        int l=l1|l2|l3|l4;
        return l;
    }
    
    public static byte[] intToByte(int number) {  
        byte[] b = new byte[4];  
        for (int i = 3; i >= 0; i--) {  
            b[i] = (byte) (number % 256);  
            number >>= 8;  
        }  
        return b;  
    }

    // //直接返回数据，读者可以添加自己解密方法
    private byte[] decrypt(byte[] data,int offset,int len) {
        return CZsEncrypt.d(data,offset,len);
    }
}