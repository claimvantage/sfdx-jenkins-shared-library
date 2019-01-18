#!/usr/bin/env groovy

def call() {
    
    def exists = fileExists 'git_externals.json'

    echo "Retrieve externals ${exists}"
    
    // Make Git externals optional
    if (exists) {
        withCredentials([sshUserPrivateKey(credentialsId: env.GITHUB_CREDENTIAL_ID, keyFileVariable: 'jenkins_private_key')]) {
            sshagent (credentials: "${jenkins_private_key}"]) {
                // Want to throw away the noisy output; TODO ${jenkins_private_key} here?
                sh returnStdout:true, script: '''
                git externals update
                git externals foreach git pull
                git externals update
                '''
            }
        }
    }
}
