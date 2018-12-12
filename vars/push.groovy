#!/usr/bin/env groovy

def call(Org org) {

    echo "Push for ${org}"

    shWithStatus "sfdx force:source:push --targetusername ${org.username}"
    
    return this
}
