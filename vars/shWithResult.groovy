#!/usr/bin/env groovy

def call(scriptText) {
    def result = sh returnStdout: true, script: scriptText
    def object = new groovy.json.JsonSlurperClassic().parseText(result);
    if (object.status != 0) error "Script ${scriptText} failed: status ${object.status} message: ${object.message} json: ${result}"
    return object.result
}
