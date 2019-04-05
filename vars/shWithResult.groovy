#!/usr/bin/env groovy

def call(script) {
    
    echo "Script ${script}"
    
    def json = sh returnStdout: true, script: script
    def object = readJSON text: json
    if (object.status != 0) {
        error "Script ${script} failed: status ${object.status} message: ${object.message} json: ${json}"
    }
    
    return object.result
}
