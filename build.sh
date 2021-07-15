#!/bin/zsh

aarSrcPath="./edu/build/outputs/aar/"
aarSrcName="edu-release.aar"
aarDesPath="./app/libs/"
aarDesName="aPaaS-release.aar"
apkDesPath="./output/"
apkDesName="aPaaS.apk"
echo "Start to Clean Project..."
./gradlew clean
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  echo "Clean Project Success!"
else
  echo "!!!Clean Project Failed!!!"
  exit 1
fi
echo "Start to setup project depends on the AAR file..."
./gradlew -b ./dependAAR.gradle
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  echo "Setup Success!"
else
  echo "!!!Setup Failed!!!"
  exit 2
fi
echo "Start to build aPaas-AAR file"
./gradlew edu:assemble
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  if [ ! -d "$aarSrcPath" ]; then
    echo "!!!aarSrcPath not exists---AAR build failed!!!"
    exit 4
  fi
  aarSrcFilePath=$aarSrcPath$aarSrcName
  if [ ! -f "$aarSrcFilePath" ]; then
    echo "!!!AAR file not exists---AAR build failed!!!"
    exit 5
  fi
  if [ ! -d "$aarDesPath" ]; then
    mkdir $aarDesPath
  fi
  # shellcheck disable=SC2181
  if [ $? -eq 0 ]; then
    if [ -f $aarSrcFilePath ]; then
      echo "Copy aPaas-aar File to libs/ success!"
      aarDesFilePath=$aarDesPath$aarDesName
      mv $aarSrcFilePath $aarDesFilePath
      if [ $? -eq 0 ]; then
        echo "Move aar to libs/ success!"
      else
        echo "Move apk to libs/ failed!"
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
          mv $apkFileDesPath "./output/aPaaS-signed.apk"
          echo "Build success!"
        else
          jarsigner -verbose -keystore $2 -storepass $3 -keypass $5 -signedjar "./output/aPaaS-signed.apk" $apkFileDesPath $4
          if [ $? -eq 0 ]; then
            echo "Sign success!"
          else
            echo "Sign failed!"
            exit 11
          fi
        fi
      else
        echo "!!!Apk build command execute failed!!!"
        exit 8
      fi
    else
      echo "!!!Des aar file not exists!!!"
      exit 7
    fi
  else
    echo "!!!Copy command execute failed!!!"
    exit 6
  fi
else
  echo "!!!Build AAR Failed!!!"
  exit 3
fi
