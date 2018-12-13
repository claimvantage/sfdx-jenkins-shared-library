# sfdx-jenkins-shared-library

Work in progress.

Provides [building blocks](/vars) for Jenkins pipeline builds to avoid duplication of boilerplate including both code and data references.
For some background information, see e.g.:
* [Share a standard Pipeline across multiple projects with Shared Libraries](https://jenkins.io/blog/2017/10/02/pipeline-templates-with-shared-libraries/)
* [Extending your Pipeline with Shared Libraries, Global Functions and External Code](https://jenkins.io/blog/2017/06/27/speaker-blog-SAS-jenkins-world/)

Requires the [Salesforce SFDX CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_install_cli.htm) to be installed where jenkins is running.

Requires https://wiki.jenkins.io/display/JENKINS/Credentials+Binding+Plugin.

Requires cleanup plugin too.

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
        // Make this glob the default
        withProjectScratchDef(glob: 'config/project-scratch-def.*.json', variable: 'org') {
            def stages = {
                try {
                    stage("${org} create") {
                        createScratchOrg org
                    }
                    stage("${org} install Claims") {
                        installPackage org "Claims v14.4" "04t2J000000AksW" env."cve.package.password.v12"
                    }
                    stage("${org} install Absence") {
                        installPackage org "Absence v14.1" "04t0V000000xDzW" env."cvab.package.password.v12"
                    }
                    stage("${org} install Accommodations") {
                        if (org == "accommodations") {
                            installPackage org "Accomodation v14.2" "04t1v0000025QyB" env."cvawa.package.password.v12"
                        }
                    }
                    stage("${org} push") {
                        push org
                    }
                    stage("${org} encryption") {
                        if (org == "encryption") {
                            setupEncryption org
                        }
                    }
                    stage("${org} test") {
                        runApexTests org
                    }
                } finally {
                    stage("${org} clean up') {
                        cleanUp org
                    }
                }
            }
            stagesPerOrg[org] = stages;
        }
        parallel stagesPerOrg
    }
    stage('Test results') {
        junitAll
    }
}
```
