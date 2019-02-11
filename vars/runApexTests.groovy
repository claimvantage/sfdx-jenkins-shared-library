#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Org org) {

    // Separate tests by build number and org name
    def testResultsDir = "${env.WORKSPACE}/tests/${env.BUILD_NUMBER}/${org.name}"
    
    sh "mkdir -p ${testResultsDir}"
    echo "Created test result dir ${testResultsDir}"

    echo "Running Apex tests for ${org.name} outputting to ${testResultsDir}"
    
    def experiencingEaiAgainErrors = true
    
    if (experiencingEaiAgainErrors) {
        
        // Use polling to workaround EAI_AGAIN errors
        
        def r1 = shWithResult "sfdx force:apex:test:run --testlevel RunLocalTests --targetusername ${org.username} --json"
        def testRunId = r1.testRunId

        def sleepMinutes = 5        // Adds 2.5 minutes to the build time on average
        def maxSleeps = 48          // Give up after about 4 hours
        def totalSleeps = 0
        
        def status = ''
        while (status != 'Completed' && totalSleeps < maxSleeps) {
        
            sleep 60 * sleepMinutes
            totalSleeps++;

            def query = "select Status, MethodsEnqueued, MethodsCompleted, MethodsFailed from ApexTestRunResult where AsyncApexJobId = '${testRunId}'"
            def r2 = shWithResult "sfdx force:data:soql:query --usetoolingapi --query \"${query}\" --targetusername ${org.username} --json"
            def record = r2.records[0]
            
            status = record.Status;
            def enqueued = record.MethodsEnqueued
            def completed = record.MethodsCompleted
            def failed = record.MethodsFailed

            echo "Test run status is \"${status}\" with ${completed} of ${enqueued} methods run (${failed} methods failed) after ${totalSleeps} sleeps of ${sleepMinutes} minutes each"
        }
    
        // Deliberately no status check so build doesn't fail immediately
        sh returnStatus: true, script: "sfdx force:apex:test:report --testrunid ${testRunId} --outputdir ${testResultsDir} --resultformat tap --targetusername ${org.username}"
    } else {
        
        // Desired, simple approach
        
        // Deliberately no status check so build doesn't fail immediately
        sh returnStatus: true, script: "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${testResultsDir} --resultformat tap --targetusername ${org.username} --wait 180"
    }
    
    // Prefix class name with target org to separate the test results
    sh "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${testResultsDir}/*-junit.xml"
}
