#!/bin/zsh

commitMsg=$1
releaseTag=$2
if [ -z "$commitMsg" ]
  then
    echo "！！！！！commitMsg is empty！！！！"
    exit 0
fi
if [ -z "$releaseTag" ]
  then
    echo "！！！！！releaseTag is empty！！！！"
    exit 0
fi
# shellcheck disable=SC2006
echo "-----push code to `git branch --show-current` branch----"
git commit -a -m "$commitMsg"
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
    echo "-----git commit success!----"
else
    echo "-----git commit FAILED!!!----"
    exit 0
fi
git push origin android-sdk:android-sdk
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
    echo "-----push code success!---"
else
    echo "-----push code FAILED!!!----"
    exit 0
fi
echo "-----ready to create new release->$releaseTag----"
# shellcheck disable=SC2086
git tag -a $releaseTag -m "$commitMsg"
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
    echo "-----create tag success!----"
else
    echo "-----create tag FAILED!!!----"
    exit 0
fi
# shellcheck disable=SC2086
git push origin $releaseTag
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
    echo "-----create release->$releaseTag success!----"
else
    echo "-----push tag FAILED!!!----"
    exit 0
fi
