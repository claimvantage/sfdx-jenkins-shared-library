#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Org org) {

    def BUILD_NUMBER = env.BUILD_NUMBER
    
    def workspace = env.WORKSPACE_ROOT
    if (!workspace) = env.WORKSPACE
    
    // Separate tests by org name
    def testResultsDir = "${workspace}/tests/${BUILD_NUMBER}/${org.name}"
    
    sh "mkdir -p ${testResultsDir}"

    echo "Running Apex tests for ${org.name} outputting to ${testResultsDir}"
    
    // Deliberately no status check
    sh  returnStatus: true, script: "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${testResultsDir} --resultformat tap --targetusername ${org.username} --wait 180"
    
    // Prefix class name with target org to separate the test results
    sh "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${testResultsDir}/*-junit.xml"
}
