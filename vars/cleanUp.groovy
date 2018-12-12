#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Org org) {

    echo "Clean up ${org}"

    echo "... delete scratch org for ${org.username}"

    if (org.username) {
        shWithStatus "sfdx force:org:delete --targetusername ${org.username} --noprompt"
    }
    
    echo "... remove workspace"
    
    // Always remove workspace and don't fail the build for any errors
    cleanWs notFailBuild: true
}
