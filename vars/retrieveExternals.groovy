def call() {

    echo "Retrieve externals"
    
    def JPK = env.JENKINS_PRIVATE_KEY_ID
    echo "Retrieve externals 2"
    if (!JPK) error "env.JENKINS_PRIVATE_KEY_ID must be set"
    
    echo "Retrieve externals 3"
    
    withCredentials([sshUserPrivateKey(credentialsId: JPK, keyFileVariable: 'jenkins_private_key')]) {
        sh '''
        which ssh-agent || ( apt-get update -y && apt-get install openssh-client -y )
        eval $(ssh-agent -s)
        ssh-add ${jenkins_private_key}
        git externals update
        '''
    }
}
