#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Org org) {

    // Separate tests by buuld number and org name
    def testResultsDir = "${env.WORKSPACE}/tests/${env.BUILD_NUMBER}/${org.name}"
    
    sh "mkdir -p ${testResultsDir}"
    echo "Created test result dir ${testResultsDir}"

    echo "Running Apex tests for ${org.name} outputting to ${testResultsDir}"
    
    // Deliberately no status check
    sh  returnStatus: true, script: "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${testResultsDir} --resultformat tap --targetusername ${org.username} --wait 180"
    
    // Prefix class name with target org to separate the test results
    sh "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${testResultsDir}/*-junit.xml"
}
