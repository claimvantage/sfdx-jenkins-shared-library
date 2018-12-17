#!/usr/bin/env groovy

def call() {

    echo "Retrieve externals"
    
    // TODO no-op if no get_externals.json
    
    withCredentials([sshUserPrivateKey(credentialsId: env.GITHUB_CREDENTIAL_ID, keyFileVariable: 'jenkins_private_key')]) {
        // Want to throw away the noisy output; TODO ${jenkins_private_key} here?
        sh returnStdout:true, script: '''
        which ssh-agent || ( apt-get update -y && apt-get install openssh-client -y )
        eval $(ssh-agent -s)
        ssh-add ${jenkins_private_key}
        git externals update
        '''
    }
}
