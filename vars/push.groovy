#!/usr/bin/env groovy

def call() {

    echo "Push for ${SFDC_USERNAME}"

    shWithStatus "sfdx force:source:push --targetusername ${SFDC_USERNAME}"
    
    return this
}
