#!/usr/bin/env groovy

def call() {

    echo "Run Apex tests for ${SFDC_USERNAME}"

    def BUILD_NUMBER = env.BUILD_NUMBER
    def RUN_ARTIFACT_DIR = "tests/${BUILD_NUMBER}"
    
    sh "mkdir -p ${RUN_ARTIFACT_DIR}"
    shWithStatus "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${RUN_ARTIFACT_DIR} --resultformat tap --targetusername ${SFDC_USERNAME} --wait 180"
    junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
    
    return this
}
