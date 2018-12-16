#!/usr/bin/env groovy
import com.claimvantage.jsl.Help
import com.claimvantage.jsl.Package

def call(Map parameters = [:]) {
    
    String glob = parameters.glob
    Help help = (Help) parameters.help
    Package[] packages = (Package[]) parameters.packages;
    
    pipeline {
        stages {
            if (help) {
                stage("help") {
                    processHelp(help: new Help('cx', '33226968', 'extras-help'))
                }
            }
            stage("checkout") {
                checkout(scm: scm, quiet: true)
                retrieveExternals()
            }
            withOrgsInParallel(glob: glob) { org ->
                stage("${org.name} create") {
                    createScratchOrg org
                }
                if (packages && packages.size() > 0) {
                    stage("${org.name} install") {
                        for (Package p in packages) {
                            installPackage(org: org, p)
                        }
                    }
                }
                stage("${org.name} push") {
                    pushToOrg org
                }
                stage("${org.name} test") {
                    runApexTests org
                }
                stage("${org.name} delete") {
                    deleteScratchOrg org
                }
            }
            stage('publish') {
                publishTestResults()
            }
            stage('clean') {
                // Always remove workspace and don't fail the build for any errors
                cleanWs notFailBuild: true
            }
        }
    }
}
