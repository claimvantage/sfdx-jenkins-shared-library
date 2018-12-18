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
                    deleteScratchOrg org
                }
            }
            stage("publish") {
                junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
            }
            stage("clean") {
                // Always remove workspace and don't fail the build for any errors
                cleanWs notFailBuild: true
            }
        }
    }
}
