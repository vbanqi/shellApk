package com.android.dexunshell;
import java.io.ByteArrayOutputStream;  
import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileOutputStream;
import java.io.IOException;  
import java.io.RandomAccessFile;
import java.security.MessageDigest;  
import java.security.NoSuchAlgorithmException;  
import java.util.zip.Adler32;  

public class DexShellTool {  
    /** 
     * @param args 
     */  
    public static void main(String[] args) {  
        try {
        	
        	String str = "./classes.dex";
            File f = new File(str);  
            if (!f.exists()) {  
                f.createNewFile();  
            } 
            
            File payloadSrcFile = new File(args[0]/*"g:/payload.apk"*/);
            
            File unShellDexFile = new File(args[1]/*"g:/unshell.dex"*/);
            byte[] unShellDexArray = readFileBytes(unShellDexFile); 
            int unShellDexLen = unShellDexArray.length;  
            
            
            
            
            FileInputStream datafilein = new FileInputStream(payloadSrcFile);
            RandomAccessFile file = new RandomAccessFile(f,"rw");
            
            int payloadLen = 0;

            file.write(unShellDexArray);
            System.out.println("unshell 长度"+unShellDexLen);
            System.out.println(file.getFilePointer());
            byte[] buffer = new byte[1024*1024];
            int prelen=0;
            while((prelen=datafilein.read(buffer))!=-1){
            	byte[] enbuf = encrpt(buffer,0,prelen);
            	int blocklen = enbuf.length;
//            	System.out.println("数据长度"+prelen);
//            	int blocklen = prelen;
            	System.out.println(blocklen);
            	file.write(intToByte(blocklen));
            	file.write(enbuf, 0, blocklen);
            	payloadLen+=blocklen+4;
            	System.out.println("数据长度"+prelen);
            }
            int totalLen = payloadLen + unShellDexLen +4;  
            file.write(intToByte(payloadLen));
            
            System.out.println("合并后"+totalLen);
            fixFileSizeHeader(file,totalLen);
            System.out.println("总长度"+file.getFilePointer());
            System.out.println(file.getFilePointer());
            fixSHA1Header(file);
            System.out.println("sha1"+file.getFilePointer());
            System.out.println(file.getFilePointer());
            fixCheckSumHeader(file);
            file.close();
            datafilein.close();
            
//            FileOutputStream localFileOutputStream = new FileOutputStream(str);  
//            byte[] payloadArray = encrpt(readFileBytes(payloadSrcFile));
//            int payloadLen = payloadArray.length;
//          int totalLen = payloadLen + unShellDexLen +4;
//          byte[] newdex = new byte[totalLen];  
//          //添加解壳代码  
//            System.arraycopy(unShellDexArray, 0, newdex, 0, unShellDexLen);  
//            //添加加密后的解壳数据  
//            System.arraycopy(payloadArray, 0, newdex, unShellDexLen, payloadLen);  
//            //添加解壳数据长度  
//            System.arraycopy(intToByte(payloadLen), 0, newdex, totalLen-4, 4);  
//                        //修改DEX file size文件头  
//            fixFileSizeHeader(newdex);  
//            //修改DEX SHA1 文件头  
//            fixSHA1Header(newdex);  
//            //修改DEX CheckSum文件头  
//            fixCheckSumHeader(newdex);  
//            
//            
//            localFileOutputStream.write(newdex);
//            localFileOutputStream.flush();  
//            localFileOutputStream.close();  
  
  
        } catch (Exception e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
    }  
    private static void fixFileSizeHeader(RandomAccessFile _file,int totallen) throws IOException {  
    	  
    	_file.seek(32);
        byte[] newfs = intToByte(totallen);  
//        System.out.println(Integer.toHexString(dexBytes.length));  
        byte[] refs = new byte[4];  
        for (int i = 0; i < 4; i++) {  
            refs[i] = newfs[newfs.length - 1 - i];
            System.out.println(Integer.toHexString(newfs[i]));  
        }  
        _file.write(refs);
//        System.arraycopy(refs, 0, dexBytes, 32, 4);  
//        return refs;
    } 
    
    private static void fixSHA1Header( RandomAccessFile _file)  
            throws Exception {
    	MessageDigest md = MessageDigest.getInstance("SHA-1");
    	byte[] buffer = new byte[1024*1024];
        int len=0;
        _file.seek(32);
        while((len=_file.read(buffer))!=-1){
        	md.update(buffer, 0, len);
        }
        
        byte[] newdt = md.digest();  
        _file.seek(12);
        _file.write(newdt,0,20);
//        _file.write(buffer,0,20);
        
        String hexstr = "";
        for (int i = 0; i < newdt.length; i++) {  
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)  
                    .substring(1);  
        }  
        System.out.println(hexstr);  
    }
      
    private static void fixCheckSumHeader(RandomAccessFile _file) throws Exception {  
        Adler32 adler = new Adler32();  
        byte[] buffer = new byte[1024*1024];
        int len=0;
        _file.seek(12);
        while((len=_file.read(buffer))!=-1){
        	adler.update(buffer, 0, len);
        }
        
//        adler.update(dexBytes, 12, dexBytes.length - 12);
        long value = adler.getValue();  
        int va = (int) value;  
        _file.seek(8);
        byte[] newcs = intToByte(va);  
        byte[] recs = new byte[4];  
        for (int i = 0; i < 4; i++) {  
            recs[i] = newcs[newcs.length - 1 - i];
            System.out.println(Integer.toHexString(newcs[i]));  
        }  
        _file.write(recs);
        
//        System.arraycopy(recs, 0, dexBytes, 8, 4);  
        System.out.println(Long.toHexString(value));  
        System.out.println();  
    }
    
    //直接返回数据，读者可以添加自己加密方法  
    private static byte[] encrpt(byte[] srcdata,int offset,int len){ 
//        System.out.printf("java.library.path:%s",System.getProperty("java.library.path"));
        return CZsEncrypt.e(srcdata,offset,len); 
//    	return srcdata;
    }  
  
  
    private static void fixCheckSumHeader(byte[] dexBytes) {  
        Adler32 adler = new Adler32();  
        adler.update(dexBytes, 12, dexBytes.length - 12);
        long value = adler.getValue();  
        int va = (int) value;  
        byte[] newcs = intToByte(va);  
        byte[] recs = new byte[4];  
        for (int i = 0; i < 4; i++) {  
            recs[i] = newcs[newcs.length - 1 - i];
            System.out.println(Integer.toHexString(newcs[i]));  
        }  
        System.arraycopy(recs, 0, dexBytes, 8, 4);  
        System.out.println(Long.toHexString(value));  
        System.out.println();  
    }   
  
  
    public static byte[] intToByte(int number) {  
        byte[] b = new byte[4];  
        for (int i = 3; i >= 0; i--) {  
            b[i] = (byte) (number % 256);  
            number >>= 8;  
        }  
        return b;  
    }  
  
  
    private static void fixSHA1Header(byte[] dexBytes)  
            throws NoSuchAlgorithmException {  
        MessageDigest md = MessageDigest.getInstance("SHA-1");  
        md.update(dexBytes, 32, dexBytes.length - 32);  
        byte[] newdt = md.digest();  
        System.arraycopy(newdt, 0, dexBytes, 12, 20);  
        String hexstr = "";  
        for (int i = 0; i < newdt.length; i++) {  
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)  
                    .substring(1);  
        }  
        System.out.println(hexstr);  
    }  
  
  
    private static void fixFileSizeHeader(byte[] dexBytes) {  
  
  
        byte[] newfs = intToByte(dexBytes.length);  
        System.out.println(Integer.toHexString(dexBytes.length));  
        byte[] refs = new byte[4];  
        for (int i = 0; i < 4; i++) {  
            refs[i] = newfs[newfs.length - 1 - i];  
            System.out.println(Integer.toHexString(newfs[i]));  
        }  
        System.arraycopy(refs, 0, dexBytes, 32, 4);  
    }  
  
  
    private static byte[] readFileBytes(File file) throws IOException {  
        byte[] arrayOfByte = new byte[1024];  
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();  
        FileInputStream fis = new FileInputStream(file);  
        while (true) {  
            int i = fis.read(arrayOfByte);  
            if (i != -1) {  
                localByteArrayOutputStream.write(arrayOfByte, 0, i);  
            } else {  
                return localByteArrayOutputStream.toByteArray();  
            }  
        }  
    }
  
}  
