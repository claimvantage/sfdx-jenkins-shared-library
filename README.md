# claimvantage/sfdx-jenkins-shared-library

## Contents
* [Why?](#why)
* [Prerequisites](#prerequisites)
* [Pipelines](#pipelines)
* [Steps](#steps)

<a name="why"></a>
## Why?

The two aims of this library are:

* To avoid the duplication of 150+ lines of `Jenkinsfile` logic across dozens of projects so a reliable pattern can be applied and maintained.

  This is accomplished by providing custom pipeline steps that hide some of the detail.
  A default pipeline that sits on top of these steps is also provided
  and is **recommended** for projects that fit its default pattern.
  
* To make the process of testing against various org configurations - e.g. person Accounts turned on or Platform Encryption turned on - simple, and not require additional builds to be setup.

  When multiple `project-scratch-def.json` files are provided that match a regular expression, parallel builds are done
  using scratch orgs created from the files.
  
For some background information including how to hook up this library, see e.g.
[Extending your Pipeline with Shared Libraries, Global Functions and External Code](https://jenkins.io/blog/2017/06/27/speaker-blog-SAS-jenkins-world/). The library is pulled directly from Git for
each new build.

<a name="prerequsities"></a>
## Prerequisites

### Jenkins

Use a recent "Long Term Support" (LTS) version of [Jenkins](https://jenkins.io/). Add these plugins:

* https://jenkins.io/doc/pipeline/steps/credentials-binding
* https://jenkins.io/doc/pipeline/steps/ws-cleanup/

### Tools

Requires [Salesforce SFDX CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_install_cli.htm) and [develersrl/git-externals](https://github.com/develersrl/git-externals) to be installed where Jenkins is running for all the stages to work.

### Jenkins Environment Variables

These must be set up for all the stages to work.

| Name | Description | Example |
|------|-------------|---------|
| CONNECTED_APP_CONSUMER_KEY_DH | ??? | 3MV...KBVI |
| HUB_ORG_DH | ??? | janedoe@claimvantage.claimvantagecrm.com |
| JENKINS_CONFLUENCE_CREDENTIALS_ID | ??? | jenkins-export-confluence |
| JENKINS_PRIVATE_KEY_ID| ??? | jenkins-to-github |
| JWT_CRED_ID_DH | ??? | devhub-key |
| SFDC_HOST_DH | ??? | https://claimvantage.my.salesforce.com |

<a name="pipelines"></a>
## Pipelines

### buildPackagePipeline

This is a ready-made pipeline - **recommended** that you start with this - that runs these [stages](vars/createScratchOrg.groovy):

```
stage("help")
stage("checkout")
perOrgInParallel() {
    stage("org create")
    stage("org install")
    stage("org push")
    stage("org test")
    stage("org delete")
}
stage("publish")
stage("clean")
```
To use it, your `Jenkinsfile` should look like this (and you will need `project-scratch-def.json` files that match the reqular expression shown below):
```
#!groovy
@Library('sfdx-jenkins-shared-library')
import com.claimvantage.sjsl.Help
import com.claimvantage.sjsl.Package

buildPackagePipeline(
    help: new Help('cx', '33226968', 'extras-help'),
    packages: [
        new Package('Claims v14.4', '04t2J000000AksW', env.'cve.package.password.v12'),
        new Package('Absence v14.1', '04t0V000000xDzW', env.'cvab.package.password.v12')
    ]
)
```
Edit the Help and Package details to reflect the specific project.

To build a package that has no help and does not depend on other packages the `Jenkinsfile` simplifies to this:
```
#!groovy
@Library('sfdx-jenkins-shared-library')_

buildPackagePipeline()
```
Note the added, required underscore.

* _glob_

  The matching pattern used to find the `project-scratch-def.json` files. Each matched file results in a separate parallel build.
  The default value is "config/project-scratch-def.*.json"; this assumes that an extra part will be insered into the file names and that part is used as a name for the parallel work.

* _help_

  A simple bean object that holds the values needed to extract, process, and commit the help.
  When left out, no help processing is done.

* _packages_

  And array of simple bean objects holding the values needed to install existing managed package versions.
  When left out, no packages installations are done.

<a name="steps"></a>
## Steps

### createScratchOrg

[Creates a scratch org](vars/createScratchOrg.groovy)
and adds values relating to that to the supplied [Org](src/com/claimvantage/jsl/Org.groovy) object for use by later steps. This step has to come before most other steps.

* _org_

  Required. An instance of Org that has it's `projectScratchDefPath` property set.
  
### deleteScratchOrg

[Deletes a scratch org](vars/deleteScratchOrg.groovy) 
identified by values added to the [Org](src/com/claimvantage/jsl/Org.groovy) object by **createScratchOrg**. This step has to come after most other steps.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.
  
### installPackage

[Installs a package](vars/installPackage.groovy)
into a scratch org. Package installs typically take 2 to 20 minutes depending on the package size.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.
  
* _package_

  Required. An instance of the [Package](src/com/claimvantage/jsl/Package.groovy) bean object
  whose properties identify the package version to install.
  
### processHelp

This is a ClaimVantage [proprietary stage](vars/processHelp.groovy) that extracts
help content from Confluence, processes that content and then adds the content to Git so that
it can be pulled into a package via Git externals.

* _help_

  Required. An instance of the [Help](src/com/claimvantage/jsl/Help.groovy) bean object
  whose properties identify the help information.
  
### pushToOrg

[Pushes](vars/pushToOrg.groovy) the components into a scratch org.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.

### retrieveExternals

Based on a `git_externals.json` file in the rewpository root, pulls in external content.

### runApexTests

[Runs Apex tests](vars/runApexTests.groovy) for an org and puts the test results in a unique folder
based on the name of the [Org](src/com/claimvantage/jsl/Org.groovy) object.
The test class names are also prefixed by that name so that when multiple orgs are tested,
the test results are presented separated by the name.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.
  
### withOrgsInParallel

Finds matching `project-scratch-def.json` files, and for each one uses the Jenkins Pipeline **parallel** step to [execute
the nested steps](vars/withOrgsInParallel.groovy). This allows multiple org configurations to be handled at the same time.

* _glob_

  The matching pattern used to find the `project-scratch-def.json` files. Each matched file results in a separate parallel build.
  The default value is "config/project-scratch-def.*.json"; this assumes that an extra part will be insered into the file names and that part is used as a name for the parallel work.
