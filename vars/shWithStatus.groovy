#!/usr/bin/env groovy

def call(scriptText) {

    def status = sh returnStatus: true, script: scriptText
    if (status != 0) {
        error "Script ${scriptText} failed: status ${status}"
    }
    
    return this
}
