#!/usr/bin/env groovy

def call(script) {
    
    echo "Script ${script}"

    def status = sh returnStatus: true, script: script
    if (status != 0) {
        error "Script ${script} failed: status ${status}"
    }
}
