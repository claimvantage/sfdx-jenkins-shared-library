# sfdx-jenkins-shared-library

## Why?

The two aims of this library are:

* To avoid the duplication of 150+ lines of `Jenkinsfile` logic so a reliable pattern can be applied and maintained.

  This is accomplished by providing custom pipeline steps that hide some of the detail.
  A default pipeline that sits on top of these steps is also provided.
  
* To make the process of testing against various org configurations - e.g. person Accounts turned on or Platform Encryption turned on - simple.

  When multiple `project-scratch-def.json` files are provided that match a regular expression, parallel builds are done
  using scratch orgs created from the files.
  
For some background information including how to hook up this library, see e.g. [Share a standard Pipeline across multiple projects with Shared Libraries](https://jenkins.io/blog/2017/10/02/pipeline-templates-with-shared-libraries/).

## Prerequsities

### Jenkins Plugins

Requires https://wiki.jenkins.io/display/JENKINS/Credentials+Binding+Plugin.

Requires cleanup plugin too.

### Tools

Requires the [Salesforce SFDX CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_install_cli.htm) to be installed where Jenkins is running.

Git externals.

### Jenkins Environment Variables

| Name | Description | Example |
|------|-------------|---------|
| CONNECTED_APP_CONSUMER_KEY_DH | ??? | 3MV...KBVI |
| HUB_ORG_DH | ??? | janedoe@claimvantage.claimvantagecrm.com |
| SFDC_HOST_DH | ??? | https://claimvantage.my.salesforce.com |

## Pipelines

### buildPackagePipeline

A ready-made pipeline is available; see the stages it uses by looking at [buildPackagePipeline](vars/buildPackagePipeline.groovy). To use it, your `Jenkinsfile` should look like this:
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
  The default value is "config/project-scratch-def.*.json"; this assumes that an extra part will be insered into the file names.

* _help_

  A simple bean object that holds the values needed to extract, process, and commit the help.
  When left out, no help processing is done.

* _packages_

  And array of simple bean objects holding the values needed to install existing managed package versions.
  When left out, no packages installations are done.

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

This is a ClaimVantage proprietary stage that extracts help content from Confluence, processes that contengt
and then adds the content to Git so that it can be pulled into a package via Git externals.

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
  The default value is "config/project-scratch-def.*.json"; this assumes that an extra part will be insered into the file names.
