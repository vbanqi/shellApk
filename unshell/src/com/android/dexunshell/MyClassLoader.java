package com.android.dexunshell;

import dalvik.system.DexClassLoader;

public class MyClassLoader extends DexClassLoader {

	public MyClassLoader(String l, String I,
			String ll, ClassLoader lI) {
		super(l, I, ll, lI);
	}
	
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return super.loadClass(className, false);
	}
}

