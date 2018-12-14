#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Org org) {

    echo "Push for ${org}"

    shWithStatus "sfdx force:source:push --targetusername ${org.username}"
    
    return this
}
