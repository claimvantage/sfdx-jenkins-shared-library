#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Org org) {

    echo "Run Apex tests for ${org}"

    def BUILD_NUMBER = env.BUILD_NUMBER
    def TEST_RESULTS_DIR = "tests/${BUILD_NUMBER}/${org.name}"
    
    sh "mkdir -p ${TEST_RESULTS_DIR}"
    shWithStatus "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${TEST_RESULTS_DIR} --resultformat tap --targetusername ${org.username} --wait 180"
    
    // Prefix class name with target org to separate the test results
    shWithStatus "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${TEST_RESULTS_DIR}/*-junit.xml"
    
    return this
}
