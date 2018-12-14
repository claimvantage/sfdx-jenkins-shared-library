#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Org org) {

    echo "Run Apex tests for ${org}"

    def BUILD_NUMBER = env.BUILD_NUMBER
    
    // Separate tests by org name
    def testResultsDir = "tests/${BUILD_NUMBER}/${org.name}"
    
    sh "mkdir -p ${testResultsDir}"
    shWithStatus "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${testResultsDir} --resultformat tap --targetusername ${org.username} --wait 180"
    
    // Prefix class name with target org to separate the test results
    shWithStatus "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${testResultsDir}/*-junit.xml"
}
