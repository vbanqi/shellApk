加壳工程的研发结果：
1、androidtest为壳工程这个里面最主要的时ProxyApplication和RefInvoke两个类
2、加壳过程详见脚本preprocess.sh
3、加壳方法是配置相关的配置文件config.cof,然后执行preprocess.sh脚本
4、preload.apk是待加壳apk，androidketest.apk是壳apk
5、备份文件夹是重要代码备份，app-sign.apk是最终apk
6、encrypt文件夹是加密相关的代码
7、编译jni调用库的命令如下：
gcc -fPIC -shared app_dex_encrypt.c CZsEncrypt.c -o libendex.so -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -I. -lssl -lcrypto  -std=c99

