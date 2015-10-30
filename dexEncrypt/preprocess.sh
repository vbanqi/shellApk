#!bin/bash
#1.解析变量
shellApk=null
shellDir=null
process_apk=null
apktool_path=null
applicationName=null
keystorepath=null
# set -x
echo "读取配置文件"
for i in `cat config.cof|grep -v ^$|grep -v '#'`;
do
	a=`echo $i|cut -d = -f1`
	echo $a
	echo $i
	echo "------------"
	if [ "$a" = "processApk" ];then
		process_apk=`echo $i|cut -d = -f2`
		echo $process_apk
	elif [ "$a" = "apktoolPath" ];then
		apktool_path=`echo $i|cut -d = -f2`
		echo $apktool_path
	elif [ "$a" = "applicationName" ];then
		applicationName=`echo $i|cut -d = -f2`
		echo $applicationName
	elif [ "$a" = "shellApk" ]; then
		shellApk=`echo $i|cut -d = -f2`
	elif [ "$a" = "keystorepath"]; then
		keystorepath=`echo $i|cut -d = -f2`
	fi
done

prodir=`echo $process_apk|cut -d . -f1`
shellDir=`echo $shellApk|cut -d . -f1`

#2、修改nanifest文件中得application
echo $process_apk
echo $apktool_path
echo "修改系统配置文件Manifest！"
java -jar $apktool_path -f -s d $process_apk||exit "apktool 执行错误！"


echo $prodir

sed -i -e "s/$applicationName/com.android.dexunshell.ProxyApplication/g" ./$prodir/AndroidManifest.xml||exit "run ant clean && ant first."
sed -i -e "s/\<\/application\>/\<meta-data android:name=\"APPLICATION_CLASS_NAME\" android:value=\"$applicationName\"\/\>\<\/application\>/g" ./$prodir/AndroidManifest.xml

echo "打包程序"
# java -jar ../apktool_2.0.0rc4.jar b preload
java -jar $apktool_path b $prodir||exit "apktool 执行错误"

#3、将修改后的manifest加入到包中
cp $prodir/build/apk/AndroidManifest.xml .
rm -r $prodir
unzip -q $process_apk -d $prodir
cp -f AndroidManifest.xml $prodir

# #4、判断unshell.dex是否存在
# if [ -e "unshell.dex"];then
# 	echo "unshell.dex 存在"
# else
	echo "解压解壳程序$shellApk!"
	unzip $shellApk -d $shellDir
	echo "生成unshell.dex"
	if [ ! -e "$prodir/lib/" ];then
		mkdir $prodir/lib/
	fi
	cp -r $shellDir/lib/* $prodir/lib/
	mv $shellDir/classes.dex unshell.dex
# fi

#5、打包程序并签名
cd ./encrypt
java DexShellTool ../$prodir/classes.dex ../unshell.dex||exit "处理classes.dex 出错~"
mv classes.dex ../$prodir
cd ../$prodir
echo "打包程序"
rm -r META-INF
zip -q -r ../app-unsign.apk ./*
cd ..

echo "程序签名"
jarsigner -keystore $keystorepath -signedjar app-sign.apk app-unsign.apk mmykeystore -storepass maotouying

# echo "善后"
# rm -r app-release
# rm app-unsign.apk
# rm unshell.dex
# rm -r $prodir
# rm -r $shellDir

