#!/usr/bin/env groovy

def call() {

    echo "Clean up"

    echo "... delete scratch org for ${SFDC_USERNAME}"

    if (SFDC_USERNAME) {
        shWithStatus "sfdx force:org:delete --targetusername ${SFDC_USERNAME} --noprompt"
    }
    
    echo "... remove workspace"
    
    // Always remove workspace and don't fail the build for any errors
    cleanWs notFailBuild: true
}
