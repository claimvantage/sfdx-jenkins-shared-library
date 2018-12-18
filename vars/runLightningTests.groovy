#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {

    Org org = (Org) parameters.org
    String configFile = parameters.configFile

    // Separate tests by build number and org name
    def testResultsDir = "${env.WORKSPACE}/lightning-tests/${env.BUILD_NUMBER}/${org.name}"

    shWithStatus "sfdx force:lightning:test:run --configfile ${configFile} --targetusername ${org.username} --appname ${env.BUILD_NUMBER} --outputdir ${testResultsDir}"
    
    echo "Evaluate test results..."
    def json = readFile(file:"${testResultsDir}/lightning-test-result.json")
    def obj = new groovy.json.JsonSlurperClassic().parseText(json)
    if (obj.summary.outcome != "Passed") { error 'Lightning validation failed. Pass Rate: ' + obj.summary.passRate }
}