#!/usr/bin/env groovy
import com.claimvantage.sjsl.Help
import groovy.json.JsonSlurper

import java.nio.file.Files

def call(Map parameters = [:]) {
    
    Help h
    if (parameters.help) {
        h = (Help) parameters.help;
    } else {
        h = new Help(parameters.spaceKey, parameters.rootPageId, parameters.repository)
    }
    
    String branch = parameters.branch ?: 'master'
    
    echo "Process help ${h.spaceKey}/${h.rootPageId} into ${h.repository} only for branch ${branch}"
    
    if (env.BRANCH_NAME == branch) {

        withCredentials([usernameColonPassword(credentialsId: env.CONFLUENCE_CREDENTIAL_ID, variable: 'USERPASS')]) {

            echo "... extract from Confluence"

            exportConfuenceSpace(USERPASS, h.rootPageId, "exportedHelp.zip")
        }

        sshagent (credentials: [env.GITHUB_CREDENTIAL_ID]) {
            
            def helpFixer = "hf.jar"

            if (h.forceDownloadHelpFixer || !fileExists(helpFixer)) {
                // Using Jenkins GitHub Personal Access Token to access private repo asset through API
                withCredentials([string(credentialsId: 'jenkins-github-api-token', variable: 'githubToken')]) {

                    def assetUrl = getLatestVersion("${githubToken}", "claimvantage", "ant-help-fixer-2").assets[0].url

                    downloadGithubAsset("${githubToken}", assetUrl, helpFixer)
                }
            }

            echo "... run fixer"
            
            def helpFixerParams = h.helpFixerParams ?: [];

            // Backslashes needed for $ that are not tokens inside all of this script
            // To update correctly the help is necessary to do
            // 1) rm -rf ${h.repository}/*
            // 2) unzip -o optimizedHelp.zip -d ${h.repository}
            // in order to update the repository with deletions as well
            sh """
            java -jar ${helpFixer} -s exportedHelp.zip -t optimizedHelp.zip -k ${h.spaceKey} ${helpFixerParams.join(' ')}
            if [ -d ${h.repository} ]; then rm -rf ${h.repository}; fi
            git clone git@github.com:claimvantage/${h.repository}.git
            rm -rf ${h.repository}/*
            which unzip || ( apt-get update -y && apt-get install unzip -y )
            unzip -o optimizedHelp.zip -d ${h.repository}
            """

            echo "... commit if necessary"

            // Avoid build breaking when nothing has changed so nothing to commit
            sh """
            cd ${h.repository}
            git add --all
            git config user.name "Jenkins"
            git config user.email "jenkins@claimvantage.com"
            if [ -z "\$(git status --porcelain)" ]; then
                echo "No help changes to commit"
            else 
                echo "Help changes to commit"
                git commit -m "Committed by Jenkins >> ${env.BRANCH_NAME} b#${env.BUILD_NUMBER}"
                git push
            fi
            cd ..
            rm -rf ${h.repository}
            """
        }
    } else {
        echo "... not processed because branch name was ${env.BRANCH_NAME}"
    }
    
    return this
}

static def getLatestVersion(token, owner, repo) {
    def url = "https://api.github.com/repos/${owner}/${repo}/releases/latest"

    def connection = new URL(url).openConnection() as HttpURLConnection
    connection.setRequestProperty("Authorization", "token ${token}")
    connection.setRequestProperty("Accept", "application/vnd.github.v3.raw")

    return new JsonSlurper().parse(connection.inputStream)
}

static def downloadGithubAsset(token, url, fileName) {
    def connection = new URL(url).openConnection() as HttpURLConnection
    connection.setRequestProperty("Authorization", "token ${token}")
    connection.setRequestProperty("Accept", "application/octet-stream")

    Files.copy(connection.inputStream, new FileOutputStream(fileName))
}

static def exportConfuenceSpace(userpass, rootPageId, zipFileName) {
    def url = "https://wiki.claimvantage.com/rest/scroll-html/1.0/sync-export?exportSchemeId=-7F00010101621A20869A6BA52BC63995&rootPageId=${rootPageId}"

    def connection = new URL(url).openConnection() as HttpURLConnection
    connection.setRequestProperty("Authorization", "Basic ${userpass}")

    Files.copy(connection.inputStream, new FileOutputStream(zipFileName))
}
