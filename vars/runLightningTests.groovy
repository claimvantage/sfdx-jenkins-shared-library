#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {

    Org org = (Org) parameters.org
    String configFile = parameters.configFile
    String appName = parameters.appName

    // Separate tests by build number and org name
    def testResultsDir = "${env.WORKSPACE}/tests/lightning/${env.BUILD_NUMBER}/${org.name}"

    sh "mkdir -p ${testResultsDir}"
    echo "Created lightning test result dir ${testResultsDir}"

    echo "Running Apex tests for ${org.name} outputting to ${testResultsDir}"

    sh returnStatus: true, script: "sfdx force:lightning:test:run --configfile ${configFile} --targetusername ${org.username} --resultformat tap --appname ${appName} --outputdir ${testResultsDir}"
    
    // Prefix class name with target org to separate the test results
    sh "sed -i -- 's/classname=\"/classname=\"${org.name}.lightningTest/g' ${testResultsDir}/*-junit.xml"
}