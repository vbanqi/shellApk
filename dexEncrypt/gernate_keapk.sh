#!/bin/bash
echo "解压解壳程序app-release!"
unzip app-release.apk -d app-release

echo "解压加密数据"

echo "生成unshell.dex"
mv app-release/classes.dex unshell.dex

echo "给程序套壳！"
java DexShellTool preload.apk unshell.dex
mv classes.dex app-release/
cd app-release

echo "打包程序"
rm -r META-INF
zip -r ../app-unsign.apk ./*
cd ..

echo "程序签名"
jarsigner -verbose -keystore ~/Documents/keystore/mmykeystore -signedjar app-sign.apk app-unsign.apk mmykeystore -storepass maotouying

echo "善后"
rm -r app-release
rm app-unsign.apk
rm unshell.dex