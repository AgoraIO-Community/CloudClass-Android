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
echo "-----ready to push code to `git branch --show-current` branch----"
git commit -a -m "$commitMsg"
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
    echo "-----git commit success!----"
    echo "Are you sure to push to the remote end?(Y/N)"
    read -r proceed
    while [ "${proceed}" != "Y" ] && [ "${proceed}" != "N" ]; do
      echo "Error Input!"
      read -r proceed
    done
    if [ "${proceed}" = "Y" ]; then
      git push origin aPaaS:aPaaS
      # shellcheck disable=SC2181
      if [ $? -eq 0 ]; then
          echo "-----push code success!---"
          echo "Are you sure to create tag?(Y/N)"
          read -r proceed
          while [ "${proceed}" != "Y" ] && [ "${proceed}" != "N" ]; do
            echo "Error Input!"
            read -r proceed
          done
          if [ "${proceed}" = "Y" ]; then
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
          elif [ "${proceed}" = "N" ]; then
            echo "Do not create tag, Exit!"
            exit 0
          fi
      else
          echo "-----push code FAILED!!!----"
          exit 0
      fi
    elif [ "${proceed}" = "N" ]; then
      echo "Do not push, Exit!"
      exit 0
    fi
else
    echo "-----git commit FAILED!!!----"
    exit 0
fi


