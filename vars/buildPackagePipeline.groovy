#!/usr/bin/env groovy
import com.claimvantage.sjsl.Help
import com.claimvantage.sjsl.Org
import com.claimvantage.sjsl.Package

def call(Map parameters = [:]) {
    
    String glob = parameters.glob ?: 'config/project-scratch-def.*.json'
    
    def helps = parameters.helps ?: []
    if (parameters.help) helps += parameters.help
    
    def packages = parameters.packages ?: []
    if (parameters.package) packages += parameters.package
    
    Closure beforePushStage = parameters.beforePushStage ?: null
    Closure beforeTestStage = parameters.beforeTestStage ?: null
    
    def keepOrg = parameters.keepOrg
    def keepWs = parameters.keepWs
    
    pipeline {
        node {
            if (helps.size() > 0) {
                stage("help") {
                    for (def h in helps) {
                        processHelp(help: h)
                    }
                }
            }
            stage("checkout") {
                checkout(scm: scm, quiet: true)
                retrieveExternals()
            }
            // Use multiple scratch orgs in parallel
            withOrgsInParallel(glob: glob) { org ->
                stage("${org.name} create") {
                    createScratchOrg org
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
                if (beforeTestStage) {
                    stage("${org.name} before test") {
                        beforeTestStage org
                    }
                }
                stage("${org.name} test") {
                    runApexTests org
                }
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
            stage("publish") {
                junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
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
        }
    }
}
