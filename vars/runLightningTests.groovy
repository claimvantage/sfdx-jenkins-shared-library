#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {

    Org org = (Org) parameters.org
    String configFile = parameters.configFile

    // Separate tests by build number and org name
    def testResultsDir = "${env.WORKSPACE}/lightning-tests/${env.BUILD_NUMBER}/${org.name}"
    sh "mkdir -p ${testResultsDir}"
    echo "Created lightning test result dir ${testResultsDir}"

    echo "Running Apex tests for ${org.name} outputting to ${testResultsDir}"

    def script = "sfdx force:lightning:test:run --configfile ${configFile} --targetusername ${org.username} --appname ${org.name}_${env.BUILD_NUMBER} --outputdir ${testResultsDir}"
    def json = sh returnStdout: true, script: script
    def object = new groovy.json.JsonSlurperClassic().parseText(json);
    
    echo "Evaluate test results..."
    if (object.summary.outcome != "Passed") { error 'Lightning validation failed. Pass Rate: ' + object.summary.passRate }
}