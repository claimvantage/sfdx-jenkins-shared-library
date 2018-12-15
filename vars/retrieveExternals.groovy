def call() {

    echo "Retrieve externals"
    
    // The name of the credentials (added via the root "Credentials" link in Jenkins
    def JPK = env.JENKINS_PRIVATE_KEY_ID
    if (!JPK) error "env.JENKINS_PRIVATE_KEY_ID must be set"
    
    // Want to throw away the noisy output
    sh returnStdout:true, script: """
    which ssh-agent || ( apt-get update -y && apt-get install openssh-client -y )
    eval $(ssh-agent -s)
    ssh-add ${jenkins_private_key}
    git externals update
    """
}
