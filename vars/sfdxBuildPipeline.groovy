#!/usr/bin/env groovy
import com.claimvantage.sjsl.Help
import com.claimvantage.sjsl.Org
import com.claimvantage.sjsl.Package

def call(Map parameters = [:]) {
    
    def glob = parameters.glob
    def stagger = parameters.stagger
    
    def helps = parameters.helps ?: []
    if (parameters.help) helps += parameters.help
    
    def packages = parameters.packages ?: []
    if (parameters.package) packages += parameters.package
    
    def packagesAfterPushStage = parameters.packagesAfterPushStage ?: []
    if (parameters.packageAfterPushStage) packagesAfterPushStage += parameters.packageAfterPushStage
    
    Closure afterCheckoutStage = parameters.afterCheckoutStage ?: null
    Closure afterOrgCreateStage = parameters.afterOrgCreateStage ?: null
    Closure beforePushStage = parameters.beforePushStage ?: null
    Closure beforeTestStage = parameters.beforeTestStage ?: null
    Closure afterTestStage = parameters.afterTestStage ?: null
    Closure finalStage = parameters.finalStage ?: null
    
    def keepOrg = parameters.keepOrg
    def keepWs = parameters.keepWs
    def channelNotification = parameters.channelNotification ?: null
    def skipApexTests = parameters.skipApexTests ?: false
    def apexTestsTimeoutMinutes = parameters.apexTestsTimeoutMinutes
    def apexTestsUsePooling = parameters.apexTestsUsePooling
    
    def cronPerBranch = parameters.cron ?: [:]
    def branchCronExpression = cronPerBranch.get(env.BRANCH_NAME)

    // defaults to 7 days
    def daysToKeepPerBranch = parameters.daysToKeepPerBranch ?: [:]
    def daysToKeepBranch = daysToKeepPerBranch.get(env.BRANCH_NAME) ?: 7
    
    pipeline {
        node {

            if (channelNotification?.trim()) {
                stage("slack notification start") {
                    def user;
                    def userId;
                    def userEmail;

                    /**
                    * https://plugins.jenkins.io/build-user-vars-plugin/
                    */
                    wrap([$class: 'BuildUser']) {
                        user = "${env.BUILD_USER}"
                        userId = "${env.BUILD_USER_ID}"
                        userEmail = "${env.BUILD_USER_EMAIL}"
                    }

                    if (userId?.trim()) {
                        try {
                            slackSend(
                                channel: "${channelNotification}",
                                color: 'good',
                                message: "${env.JOB_NAME} - #${env.BUILD_NUMBER} Started by ${user} [<mailto:${userEmail}|${userId}>] (<${env.BUILD_URL}|Open>)"
                            )
                        } catch (error) {
                            echo "Error: ${error.getMessage()}"
                        }
                    }
                }
            }

            def propertiesConfigured = []
            propertiesConfigured.push(
                buildDiscarder(
                    logRotator(
                        artifactDaysToKeepStr: '',
                        artifactNumToKeepStr: '',
                        daysToKeepStr: '${daysToKeepBranch}',
                        numToKeepStr: ''
                    )
                )
            );
            propertiesConfigured.push(
                [$class: 'JobRestrictionProperty']
            );
            if (branchCronExpression != null) {
                // For e.g. once a night runs
                propertiesConfigured.push(
                    pipelineTriggers(
                        [
                            cron(branchCronExpression)
                        ]
                    )
                );
            }
            properties(
                propertiesConfigured
            )
            
            if (helps.size() > 0) {
                stage("help") {
                    for (def h in helps) {
                        processHelp(help: h, branch: parameters.helpBranch)
                    }
                }
            }
            stage("checkout") {
                checkout(scm: scm)
                retrieveExternals()
            }
            if (afterCheckoutStage) {
                stage("after checkout") {
                    afterCheckoutStage.call()
                }
            }
            // Use multiple scratch orgs in parallel
            withOrgsInParallel(glob: glob, stagger: stagger) { org ->
                stage("${org.name} create") {
                    createScratchOrg org
                }

                try {
                    if (afterOrgCreateStage) {
                        stage("after ${org.name} create") {
                            afterOrgCreateStage org
                        }
                    }
                    if (packages.size() > 0) {
                        stage("${org.name} install") {
                            for (def p in packages) {
                                installPackage(org: org, package: p)
                            }
                        }
                    }
                    if (beforePushStage) {
                        stage("${org.name} before push") {
                            beforePushStage org
                        }
                    }
                    stage("${org.name} push") {
                        pushToOrg org
                    }
                    
                    if (packagesAfterPushStage.size() > 0) {
                        stage("${org.name} install after push") {
                            for (def p in packagesAfterPushStage) {
                                installPackage(org: org, package: p)
                            }
                        }
                    }
                    
                    if (beforeTestStage) {
                        stage("${org.name} before test") {
                            beforeTestStage org
                        }
                    }
                    if (!skipApexTests) {
                        stage("${org.name} test") {
                            runApexTests(org: org, timeoutMinutes: apexTestsTimeoutMinutes, usePolling: apexTestsUsePooling)
                        }
                    }
                    if (afterTestStage) {
                        stage("${org.name} after test") {
                            afterTestStage org
                        }
                    }
                } finally {
                    stage("${org.name} delete") {
                        if (keepOrg) {
                            // To allow diagnosis of failures
                            echo "Keeping scratch org name ${org.name} username ${org.username} password ${org.password} url ${org.instanceUrl} orgId ${org.orgId}"
                        } else {
                            echo "Deleting scratch org name ${org.name}"
                            deleteScratchOrg org
                        }
                    }
                }
            }
            stage("publish") {
                junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
            }

            if (channelNotification?.trim()) {
                stage("slack notification end") {
                    /*
                    * Slack integration
                    * https://api.slack.com/docs/message-guidelines
                    */
                    def slackColor;
                    if ("${currentBuild.currentResult}" == "UNSTABLE") {
                        slackColor = 'warning'
                    } else if ("${currentBuild.currentResult}" == "FAILURE") {
                        slackColor = 'danger'
                    } else if ("${currentBuild.currentResult}" == "SUCCESS") {
                        slackColor = 'good'
                    }
                    try {
                        slackSend channel: '#tech-builds', color: slackColor, message: "${URLDecoder.decode(env.JOB_NAME)} - #${env.BUILD_NUMBER} - ${currentBuild.currentResult} after ${currentBuild.durationString.minus(' and counting')} (<${env.BUILD_URL}|Open>)"
                    } catch (error) {
                        echo "Error: ${error.getMessage()}"
                    }
                }
            }

            stage("clean") {
                if (keepWs) {
                    // To allow diagnosis of failures
                    echo "Keeping workspace ${env.WORKSPACE}"
                } else {
                    // Always remove workspace and don't fail the build for any errors
                    echo "Deleting workspace ${env.WORKSPACE}"
                    cleanWs notFailBuild: true
                }
            }

            // To allow notification or any extra final step	
            if (finalStage) {	
                stage("final stage") {	
                    finalStage.call()	
                }	
            }
        }
    }
}
