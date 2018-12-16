# sfdx-jenkins-shared-library

Work in progress.

Provides [building blocks](/vars) for Jenkins pipeline builds to avoid duplication of boilerplate including both code and data references.
For some background information, see e.g. [Share a standard Pipeline across multiple projects with Shared Libraries](https://jenkins.io/blog/2017/10/02/pipeline-templates-with-shared-libraries/)

## Prerequsities

Requires the [Salesforce SFDX CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_install_cli.htm) to be installed where jenkins is running.

Requires https://wiki.jenkins.io/display/JENKINS/Credentials+Binding+Plugin.

Requires cleanup plugin too.

## Pipeline

A ready-made pipleline is available. To use it your `Jenkinsfile` should look like this:
```
#!groovy
@Library('sfdx-jenkins-shared-library')
import com.claimvantage.jsl.Help
import com.claimvantage.jsl.Package

buildPackagePipeline(
    help: new Help('cx', '33226968', 'extras-help'),
    packages: [
        new Package('Claims v14.4', '04t2J000000AksW', env.'cve.package.password.v12'),
        new Package('Absence v14.1', '04t0V000000xDzW', env.'cvab.package.password.v12')
    ]
)
```
Edit the Help and Package details to reflect the soecific project.

To build a package that has no help and does not depend on other packages the `Jenkinsfile` simplifies to this:
```
#!groovy
@Library('sfdx-jenkins-shared-library')_

buildPackagePipeline()
```
Note the added, required underscore

## Steps
