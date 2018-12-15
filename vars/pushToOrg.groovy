#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Org org) {

    echo "Push for ${org}"

    // Trying to cut log noise
    // shWithStatus "sfdx force:source:push --targetusername ${org.username}"
    sh returnStdout: true, script: "sfdx force:source:push --targetusername ${org.username}"
    
    return this
}
