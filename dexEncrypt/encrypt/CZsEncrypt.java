/**
 * Created by lvqiang on 15/3/16.
 */
public class CZsEncrypt {
    static {
    	// System.out.printf("java.library.path:%s\n",System.getProperty("java.library.path"));
        System.load("/Users/lvqiang/tools/dexEncrypt/encrypt/libendex.so");
    }

    public static native byte[] d(byte[] _byte,int offset,int len);

    public static native byte[] e(byte[] _byte,int offset,int len);

    public static void main(String[] args) {
    	String hello="大家好啊……";
    	byte[] b=hello.getBytes();
    	// System.out.println("加密前长度："+b.length);
		byte[] c=e(b,0,b.length);
		System.out.println("加密后长度："+c.length);
		byte[] d=d(c,0,c.length);
		System.out.println("解密后长度："+d.length);
		String out=new String(d);
		System.out.println(out);
    }
}
