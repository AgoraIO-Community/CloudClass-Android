#!/bin/bash

echo "start build_app"
echo ANDROID_NDK_ROOT: $ANDROID_NDK_ROOT
echo ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT
echo GRADLE_HOME: $GRADLE_HOME

# debug mode
set -x
. ../apaas-cicd-android/utils/v1/build_utils.sh
. ../apaas-cicd-android/utils/v1/file_utils.sh
. ../apaas-cicd-android/pack/v1/build_apk.sh

projectPath=$(pwd)
isRelease=$isRelease

# apkDesPath from build_apk.sh
rm -r $apkDesPath

# branch
branch_name=$cloudclass_android_branch
# release/2.8.80 -> release_2.8.80
branch_name=$(echo "$branch_name" | sed 's/\//_/g')

env

ls -l open-cloudclass-android

#echo "cloudclass-android branch="$(git symbolic-ref --short HEAD)
#
#echo "open-cloudclass-android  branch="
#git -C open-cloudclass-android branch --show-current

# set gradle proxy
handleGradle() {
  buildGradleZIP $projectPath $GRADLE_HOME
  $projectPath/gradlew -Dhttps.proxyHost=10.10.114.51 -Dhttps.proxyPort=1080
}

buildApkName() {
  if [ "$isRelease" = true ]; then
    tag=""
  else
    tag="_debug"
  fi
  #Flexible_Android_release_2.8.80_20231016_1717_debug.apk
  apk_name=flexible_android_${branch_name}_${build_date}_${BUILD_ID}${tag}.apk
}

handleAARBuild() {
  # use aar
  $projectPath/gradlew -b readyAAR.gradle
  cat gradle.properties

  downloadAAR AgoraEduUIKit
  downloadAAR AgoraCloudScene
  downloadAAR AgoraClassSDK

  ls -l

  # mv
  mv AgoraEduUIKit-release.aar app/libs
  mv AgoraCloudScene-release.aar app/libs
  mv AgoraClassSDK-release.aar app/libs

  ls -l app/libs
}

handleBuildSignApk() {
  buildToolsPath=${ANDROID_SDK_ROOT}/build-tools/33.0.1/apksigner
  buildSignApk $isRelease $buildToolsPath $apk_name
}

handleBuild() {
  buildToolsPath=${ANDROID_SDK_ROOT}/build-tools/33.0.1/apksigner
  buildSignApk false $buildToolsPath $apk_name
}

handleUploadApk() {
  # apkPath from build_apk.sh
  apk_des_path=$apkPath
  module_name="App"
  uploadFile $apk_des_path $module_name $branch_name

  #  upload_file_url from build_utils.sh
  notifyWeChat ${upload_file_url}/${apk_name}
}

buildApkName
handleGradle
#handleAARBuild

if [ "${Package_Publish}" = true ] || [[ "${branch_name}" =~ "release" ]]; then
  handleBuildSignApk
  handleUploadApk
else
  handleBuild
fi
