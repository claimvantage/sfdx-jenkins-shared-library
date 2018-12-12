def call() {

    echo "Retrieve externals"
    
    sh '''
    which ssh-agent || ( apt-get update -y && apt-get install openssh-client -y )
    eval $(ssh-agent -s)
    ssh-add ${jenkins_private_key}
    git externals update
    '''
    
    return this
}
