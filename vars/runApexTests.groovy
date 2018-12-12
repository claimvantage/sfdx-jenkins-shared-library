#!/usr/bin/env groovy

def call(Org org) {

    echo "Run Apex tests for ${org}"

    def BUILD_NUMBER = env.BUILD_NUMBER
    def TEST_RESULTS_DIR = "tests/${BUILD_NUMBER}"
    
    sh "mkdir -p ${TEST_RESULTS_DIR}"
    shWithStatus "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${RUN_ARTIFACT_DIR} --resultformat tap --targetusername ${org.username} --wait 180"
    
    // Prefix class name with target org to separate the test results
    // TODO need separate folder perhaps
    shWithStatus "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${TEST_RESULTS_DIR}/*-junit.xml"
    
    junit keepLongStdio: true, testResults: 'tests/**/*-junit.xml'
    
    return this
}
