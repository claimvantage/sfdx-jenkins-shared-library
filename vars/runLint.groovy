#!/usr/bin/env groovy

def call(Map parameters = [:]) {
    def folders = parameters.folders
    def PARALLEL_LINT_STAGES_MAP = folders.collectEntries {
        ["${it}" : generateStage(it)]
    }
    parallel PARALLEL_LINT_STAGES_MAP
}
def generateStage(job) {
    return {
        stage("lint: ${job}") {
            sh "sfdx force:lightning:lint ${job} --exit"
        }
    }
}