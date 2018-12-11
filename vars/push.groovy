#!/usr/bin/env groovy

def call() {

    echo "Push for ${SFDC_USERNAME}"

    def rc = sh returnStatus: true, script: "sfdx force:source:push --targetusername ${SFDC_USERNAME}"
    if (rc != 0) { error 'Push failed' }
    
    return this
}
