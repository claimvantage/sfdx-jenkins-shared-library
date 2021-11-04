#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {

    Org org = (Org) parameters.org
    
    // Install LWC Test Runner
    sh script: "sfdx force:lightning:lwc:test:setup"

    // Using colons as separators instead of forward slashes. Branches may have slashes.
    // E.g: release/v1 would come as release%20Fv1 and it would not find the proper directory
    def orgNameFolder = org.name.replace("%20F", ":")
    // Leaving it on a separate folder for now
    def testResultsFolder = "jest-tests/${env.BUILD_NUMBER}/${orgNameFolder}"
    def testResultsFile = "test-result-lwc-junit.xml"
    def testResultsPath = "${testResultsFolder}/${testResultsFile}"

    // Run LWC tests - deliberately no status check so build doesn't fail immediately; klunky reporter option passing via environment variables
    // Using -- -- because the jest command is two levels below the npm
    sh returnStatus: true, script: "JEST_JUNIT_OUTPUT_DIR='${testResultsFolder}' JEST_JUNIT_OUTPUT_NAME='${testResultsFile}' npm run test:unit -- -- --ci --reporters=default --reporters=jest-junit"

    // Prefix class name with target org to separate the test results
    sh returnStatus: true, script: "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${testResultsPath}"

    // Collect results
    junit keepLongStdio: true, testResults: "${testResultsPath}"
}