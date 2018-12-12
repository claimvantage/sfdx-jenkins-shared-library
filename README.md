# jenkins-shared-library

Work in progress.

Provides [building blocks](/vars) for Jenkins pipeline builds to avoid duplication of boilerplate including both code and data references.
For some background information, see e.g.:
* [Share a standard Pipeline across multiple projects with Shared Libraries](https://jenkins.io/blog/2017/10/02/pipeline-templates-with-shared-libraries/)
* [Extending your Pipeline with Shared Libraries, Global Functions and External Code](https://jenkins.io/blog/2017/06/27/speaker-blog-SAS-jenkins-world/)

Here is an example (declarative) `Jenkinsfile` using these building blocks (may have to be on separate lines):

```
pipeline {
    stages {
        stage('Help') { processHelp "extras-help" "33226968" "cx" }
        stage('Checkout') { checkout }
        stage('Externals') { retrieveExternals } 
        stage('Create org') { createScratchOrg }
        stage('Install Claims') { installPackage "Claims v14.4" "04t2J000000AksW" env."cve.package.password.v12" }
        stage('Install Absence') { installPackage "Absence v14.1" "04t0V000000xDzW" env."cvab.package.password.v12" }
        stage('Push') { push }
        stage('Test') { runApexTests }
    }
    post {
        cleanUp
    }
}
```
and for multiple org configurations e.g. with Platform Encryption or with Person Accounts (scripted):
```
node {
    stage('Help') {
        processHelp "extras-help" "33226968" "cx"
    }
    stage('Checkout') {
        checkout
    }
    stage('Externals') {
        retrieveExternals
    }
    stage('Orgs') {
        def stagesPerOrg = [:]
        forEachProjectScratchDef {
            def stages = {
                // Probably needed for closure to work
                def org = ${ORG}
                try {
                    stage("${org} create") {
                        createScratchOrg
                    }
                    stage("${org} install Claims") {
                        installPackage "Claims v14.4" "04t2J000000AksW" env."cve.package.password.v12"
                    }
                    stage("${org} install Absence") {
                        installPackage "Absence v14.1" "04t0V000000xDzW" env."cvab.package.password.v12"
                    }
                    stage("${org} install Accommodations") {
                        if (org == "accommodations") {
                            installPackage "Accomodation v14.2" "04t1v0000025QyB" env."cvawa.package.password.v12"
                        }
                    }
                    stage("${org} push") {
                        push
                    }
                    stage("${org} encryption") {
                        if (org == "encryption") {
                            setupEncryption
                        }
                    }
                    stage("${org} test") {
                        runApexTests
                    }
                } finally {
                    stage("${org} clean up') {
                        cleanUp
                    }
                }
            }
            stagesPerOrg[org] = stages;
        }
        parallel stagesPerOrg
    }
}
```
