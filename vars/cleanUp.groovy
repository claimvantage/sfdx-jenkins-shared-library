#!/usr/bin/env groovy

def call() {

    echo "Clean up"

    echo "... delete scratch org for ${SFDC_USERNAME}"

    if (SFDC_USERNAME) {
        def rc = sh returnStatus: true, script: "sfdx force:org:delete --targetusername ${SFDC_USERNAME} --noprompt"
        if (rc != 0) { error 'Scratch org deletion request failed' }
    }
    
    echo "... remove workspace"
    
    // Always remove workspace and don't fail the build for any errors
    cleanWs notFailBuild: true
}
