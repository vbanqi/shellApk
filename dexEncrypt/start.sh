#!bin/bash

# sh preprocess.sh
adb uninstall lv.keexample
adb install app-sign.apk

#test
# prodir=preload
# applicationName=lv.keexample.CApplication
# sed -i -e "s/$applicationName/com.android.dexunshell.ProxyApplication/g" ./$prodir/AndroidManifest.xml
# sed -i -e "s/\<\/application\>/\<meta-data android:name=\"APPLICATION_CLASS_NAME\" android:value=\"lv.keexample.CApplication\"\/\>\<\/application\>/g" ./$prodir/AndroidManifest.xml

