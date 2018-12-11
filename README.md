# pipeline-shared-library

Work in progress.

Provides [building blocks](/vars) for Jenkins pipeline builds to avoid duplication of bolerplate including both code and data references.
For some background information, see e.g.:
* [Share a standard Pipeline across multiple projects with Shared Libraries](https://jenkins.io/blog/2017/10/02/pipeline-templates-with-shared-libraries/)
* [Extending your Pipeline with Shared Libraries, Global Functions and External Code](https://jenkins.io/blog/2017/06/27/speaker-blog-SAS-jenkins-world/)

Here is an example `Jenkinsfile` using these building blocks (may have to be on separate lines):

```
pipeline {
    stages {
        stage('Checkout') { checkout }
        stage('Externals') { retrieveExternals } 
        stage('Create org') { createScratchOrg }
        stage('Install Claims') { installPackage "Claims v14.4" "04t2J000000AksW" env."cve.package.password.v12" }
        stage('Install Absence') { installPackage "Absence v14.1" "04t0V000000xDzW" env."cvab.package.password.v12" }
        stage('Push') { push }
        stage('Test') { runApexTests }
        stage('Help') { processHelp "extras-help" "33226968" "cx" }
    }
    post {
        cleanUp
    }
}
```
and for multiple org configurations (something like this maybe):
```
node {
        stage('Checkout') { checkout }
        stage('Externals') { retrieveExternals }
        stage('Configurations') {
            def stagesPerOrg = [:]
            for (def org : ...) {
                def stages = {
                    try {
                        stage('Create org') { createScratchOrg }
                        stage('Install Claims') { installPackage "Claims v14.4" "04t2J000000AksW" env."cve.package.password.v12" }
                        stage('Install Absence') { installPackage "Absence v14.1" "04t0V000000xDzW" env."cvab.package.password.v12" }
                        stage('Push') { push }
                        stage('Test') { runApexTests }
                    } finally {
                        stage('Clean up') { cleanUp }
                    }
                }
                stagesPerOrg[org] = stages;
            }
            parallel stagesPerOrg
        }
        stage('Help') { processHelp "extras-help" "33226968" "cx" }
    }
}
```
