#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {
    
    Org org = parameters.org
    
    // Separate tests by build number and org name
    def testResultsDir = "${env.WORKSPACE}/tests/${env.BUILD_NUMBER}/${org.name}"
    
    def timeoutMinutes = parameters.timeoutMinutes ?: 300 // give up after 5h by default
    
    sh "mkdir -p ${testResultsDir}"
    echo "Created test result dir ${testResultsDir}"

    echo "Running Apex tests for ${org.name} outputting to ${testResultsDir}"
    
    // default to true. Only use the parameters if is present and not null. This avoids falsy comparison (null == false).
    def usePolling = parameters.usePolling == null ? true : parameters.usePolling
    
    if (usePolling) {
        
        // Use polling to workaround EAI_AGAIN errors
        
        def r1 = shWithResult "sfdx force:apex:test:run --testlevel RunLocalTests --targetusername ${org.username} --json"
        def testRunId = r1.testRunId

        def sleepMinutes = 1        // Adds 30 secondss to the build time on average
        def maxSleeps = timeoutMinutes
        def totalSleeps = 0
        
        def status = ''
        while (status != 'Completed' && totalSleeps < maxSleeps) {
        
            sleep time: sleepMinutes, unit: "MINUTES"
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
        
        // Get more info: trying to understand variable number of test results
        def query = "select ApexClass.Name, Status, ExtendedStatus from ApexTestQueueItem where ParentJobId = '${testRunId}'"
        sh returnStatus: true, script: "sfdx force:data:soql:query --resultformat human --usetoolingapi --query \"${query}\" --targetusername ${org.username}"

    } else {
        
        // Desired, simple approach
        
        // Deliberately no status check so build doesn't fail immediately
        sh returnStatus: true, script: "sfdx force:apex:test:run --synchronous --testlevel RunLocalTests --outputdir ${testResultsDir} --resultformat tap --targetusername ${org.username} --wait ${timeoutMinutes}"
    }
        
    // Prefix class name with target org to separate the test results
    sh "sed -i -- 's/classname=\"/classname=\"${org.name}./g' ${testResultsDir}/*-junit.xml"
}
