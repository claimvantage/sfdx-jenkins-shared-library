#!/usr/bin/env groovy

def call(scriptText) {
    
    def json = sh returnStdout: true, script: scriptText
    def object = new groovy.json.JsonSlurperClassic().parseText(json);
    if (object.status != 0) {
        error "Script ${scriptText} failed: status ${object.status} message: ${object.message} json: ${json}"
    }
    
    return object.result
}
