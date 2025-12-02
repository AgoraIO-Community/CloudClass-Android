// -*- mode: groovy -*-
// vim: set filetype=groovy :
@Library('agora-build-pipeline-library') _
import groovy.transform.Field

buildUtils = new agora.build.BuildUtils()

compileConfig = [
        "sourceDir"  : "cloudclass-android",
        "docker"     : "hub.agoralab.co/server/arsenal_build_android:0.2.4-jdk11",
        "non-publish": [
                "command"  : "./ci/build/build_android.sh",
                "extraArgs": "",
        ],
        "publish"    : [
                "command"  : "./ci/build/build_android.sh",
                "extraArgs": "",
        ]
]

def doBuild(buildVariables) {
    timestamps {
        stage('doBuild time') {
            preCommand = '''
            export ANDROID_NDK_ROOT="${HOME}/Library/Android/android-ndk-r21e"
        '''
            type = params.Package_Publish ? "publish" : "non-publish"
            command = compileConfig.get(type).command
            preCommand = compileConfig.get(type).get("preCommand", "")
            postCommand = compileConfig.get(type).get("postCommand", "")
            docker = compileConfig.docker
            extraArgs = compileConfig.get(type).extraArgs
            extraArgs += " " + params.getOrDefault("extra_args", "")
            commandConfig = [
                    "command"   : command,
                    "sourceRoot": "${compileConfig.sourceDir}",
                    "extraArgs" : extraArgs,
                    "docker"    : docker,
            ]
            loadResources(["config.json", "artifactory_utils.py"])
            buildUtils.customBuild(commandConfig, preCommand, postCommand)
        }
    }
}

def doPublish(buildVariables) {
    timestamps {
        stage('doPublish time') {
            if (!params.Package_Publish) {
                return
            }
            (shortVersion, releaseVersion) = buildUtils.getBranchVersion()
            def archiveInfos = [
                    [
                            "type"          : "ARTIFACTORY",
                            "archivePattern": "*.zip",
                            "serverPath"    : "FlexibleDemoAndroid/${shortVersion}/${buildVariables.buildDate}/${env.platform}",
                            "serverRepo"    : "AD_repo"
                    ]
            ]
            archiveUrls = archive.archiveFiles(archiveInfos) ?: []
            archiveUrls = archiveUrls as Set
            if (archiveUrls) {
                def content = archiveUrls.join("\n")
                writeFile(file: 'package_urls', text: content, encoding: "utf-8")
            }
            archiveArtifacts(artifacts: "package_urls", allowEmptyArchive: true)
            sh "rm -rf *.zip || true"
        }
    }
}

pipelineLoad(this, "FlexibleDemoAndroid", "build", "android", "apiexample_linux")