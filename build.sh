#!/bin/zsh

aarSrcPath="./edu/build/outputs/aar/"
aarSrcName="edu-release.aar"
aarDesPath="./app/libs/"
aarDesName="aPaaS-release.aar"
apkDesPath="./output/"
apkDesName="aPaaS.apk"
apkAlignedName="aPaaS-aligned.apk"
apkSignedName="aPaaS-signed.apk"
zipalignToolName="/zipalign"
apksignerToolName="/apksigner"
echo "Start to Clean Project..."
./gradlew clean
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  echo "Clean Project Success!"
else
  echo "!!!Clean Project Failed!!!"
  exit 1
fi
echo "Start to build apk..."
      ./gradlew app:assembleNormal
      # shellcheck disable=SC2181
      if [ $? -eq 0 ]; then
        apkFilePath="./app/build/outputs/apk/normal/debug/app-normal-debug.apk"
        apkFilePath_Debug="./app/build/outputs/apk/normal/debug/app-normal-debug.apk"
        apkFilePath_Release="./app/build/outputs/apk/normal/release/app-normal-release-unsigned.apk"
        if [ -f $apkFilePath_Debug -a -f $apkFilePath_Release ]; then
          echo "Apk build success!"
        else
          echo "!!!Apk build failed!!!"
          echo 9
        fi
        # shellcheck disable=SC2086
        if [ $1 = "debug" ]; then
          apkFilePath=$apkFilePath_Debug
          echo "Current0 apkFilePath is $apkFilePath"
        else
          apkFilePath=$apkFilePath_Release
          echo "Current1 apkFilePath is $apkFilePath"
        fi
        echo "Current apkFilePath is $apkFilePath"
        if [ ! -d $apkDesPath ]; then
          mkdir $apkDesPath
        fi
        apkFileDesPath=$apkDesPath$apkDesName
        mv $apkFilePath $apkFileDesPath
        if [ $? -eq 0 ]; then
          echo "Move apk to output success!"
        else
          echo "Move apk to output failed!"
          echo 10
        fi
        # shellcheck disable=SC2086
        if [ $1 = "debug" ]; then
          apkFilePath=$apkFilePath_Debug
          echo "Current0 apkFilePath is $apkFilePath"
          mv $apkFileDesPath $apkDesPath$apkSignedName
          echo "Build success!"
        else
          $6$apksignerToolName sign --ks $2 --ks-pass pass:$3 --ks-key-alias $4 --key-pass pass:$5 --out $apkDesPath$apkSignedName $apkFileDesPath
          if [ $? -eq 0 ]; then
            echo "Sign success!"
            rm $apkDesPath$apkDesName
          else
            echo "Sign failed!"
            exit 11
          fi
        fi
      else
        echo "!!!Apk build command execute failed!!!"
        exit 8
      fi
