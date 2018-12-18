# sfdx-jenkins-shared-library

## Contents
* [Why?](#why)
* [Prerequisites](#prerequisites)
* [Pipelines](#pipelines)
* [Steps](#steps)
* [Multiple Orgs](#multiple)
* [Org Bean](#org)

<a name="why"></a>
## Why?

The two aims of this library are:

* To avoid the duplication of 150+ lines of `Jenkinsfile` logic across dozens of projects so a reliable pattern can be applied and maintained.

  This is accomplished by providing custom pipeline steps that hide some of the detail.
  A default pipeline that sits on top of these steps is also provided
  and is **recommended** for projects that fit its default pattern.
  
* To make the process of testing against various org configurations - e.g. person Accounts turned on or Platform Encryption turned on - simple, and not require additional builds to be setup.

  When multiple
  [Scratch Org Definition File](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_scratch_orgs_def_file.htm)
  files are provided that match a regular expression, parallel builds are done using scratch orgs created from the files.
  
The resulting Jenkinsfile can be as simple as:
```
#!groovy
@Library('sfdx-jenkins-shared-library')_

buildPackagePipeline()
```
  
For some background information including how to hook up this library, see e.g.
[Extending your Pipeline with Shared Libraries, Global Functions and External Code](https://jenkins.io/blog/2017/06/27/speaker-blog-SAS-jenkins-world/). The library is pulled directly from Git for
each new build, so setup is simple.

<a name="prerequsities"></a>
## Prerequisites

### Unix Only

Makes use of shell scripting so only Unix platforms are supported.

### Jenkins

Use a recent "Long Term Support" (LTS) version of [Jenkins](https://jenkins.io/). Add these:

* [Credentials Binding Plugin](https://jenkins.io/doc/pipeline/steps/credentials-binding/)
* [Workspace Cleanup plugin](https://jenkins.io/doc/pipeline/steps/ws-cleanup/)

### Tools

Requires [Salesforce SFDX CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_install_cli.htm) to be installed where Jenkins is running (and also Git). If Git externals are being used, also requires [develersrl/git-externals](https://github.com/develersrl/git-externals).

### Jenkins Environment Variables

These must be set up for all the stages to work.

| Name | Description | Example |
|:-----|:------------|:--------|
| CONFLUENCE_CREDENTIAL_ID<sup>[2]</sup> | Confluence username/password credentials stored in Jenkins in "Credentials" under this name. | to-confluence |
| DEVHUB_CONSUMER_KEY<sup>[1]</sup> | Consumer key for the Connected App setup in the Dev Hub. | 3MV...KBVI |
| DEVHUB_CREDENTIAL_ID<sup>[1]</sup> | A Dev Hub generated private key stored in Jenkins in "Credentials" under this name. | to-devhub |
| DEVHUB_USERNAME<sup>[1]</sup> | A Dev Hub username to work under when connecting to the Dev Hub. | janedoedev@claimvantage.com |
| GITHUB_CREDENTIAL_ID<sup>[3]</sup> | A GitHub generated private key **and** username stored in Jenkins in "Credentials" under this name.| to-github |


[1] These values are inter-related and so need updating together. They are used to connect to the Dev Hub for the initial authentication before a scratch org is created and used.

[2] Used to extract help pages from Confluence.

[3] Used to retrieve Git externals.

<a name="pipelines"></a>
## Pipelines

### buildPackagePipeline

This is a [ready-made pipeline](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/buildPackagePipeline.groovy) - **recommended** that you start with this - that runs these stages using both the steps listed in [Steps](#steps) section below and standard steps:

```
stage("help") {...}
stage("checkout") {...}
withOrgsInParallel() {
    stage("org create") {...}
    stage("org install") {...}
    stage("org before push") {...}
    stage("org push") {...}
    stage("org before test") {...}
    stage("org test") {...}
    stage("org delete") {...}
}
stage("publish")  {...}
stage("clean") {...}
```
To use it, your `Jenkinsfile` should look like this (and you will need
[Scratch Org Definition File](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_scratch_orgs_def_file.htm)
files that match the reqular expression):
```
#!groovy
@Library('sfdx-jenkins-shared-library')
import com.claimvantage.sjsl.Help
import com.claimvantage.sjsl.Package

buildPackagePipeline(
    glob: 'config/project-scratch-def.*.json',
    help: new Help('cx', '33226968', 'extras-help'),
    packages: [
        new Package('Claims v14.4', '04t2J000000AksW', env.'cve.package.password.v12'),
        new Package('Absence v14.1', '04t0V000000xDzW', env.'cvab.package.password.v12')
    ]
)
```
Edit the Help and Package details to reflect the specific project.

To build a package that has no help and does not depend on other packages and has a single default named `project-scratch-def.json`, the `Jenkinsfile` simplifies to this:
```
#!groovy
@Library('sfdx-jenkins-shared-library')_

buildPackagePipeline(
    glob: 'config/project-scratch-def.json'
)
```
Note the added, required underscore at the end of the `@Library` line.

The named values available are:

* _beforePushStage_

  An (optional) closure that is executed immediately before the push stage. The `org` is passed in to this.
  See the [Org Bean](#org) section below.
  
  This is good point to insert extra content: the **createTrigger** step is one way to do that.

* _beforeTestStage_

  An (optional) closure that is executed immediately before the test stage. The `org` is passed in to this.
  See the [Org Bean](#org) section below.
  
  This example executes some Apex code included in the pushed code (in this case setting up a role):
  ```
  beforeTestStage: { org ->
      sh "echo 'cveep.UserBuilder.ensureRole();' | sfdx force:apex:execute --targetusername ${org.username}"
  }
  ```

* _glob_

  The matching pattern used to find the
  [Scratch Org Definition File](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_scratch_orgs_def_file.htm)
  files. Each matched file results in a separate parallel build.
  The default value is "config/project-scratch-def.*.json"; this assumes that an extra part will be insered into the file names and that part is used as a name for the parallel work.

* _help_ (or _helps_)

  Reference a simple bean object (or an array of those objects) that holds the values needed to extract, process, and commit the help.
  When left out, no help processing is done.

* _package_ (or _packages_)

  Reference a simple bean object (or an array of those objects) that holds the values needed to install existing managed package versions.
  When left out, no package installation is done.

<a name="steps"></a>
## Steps

The general pattern to use these steps is this, where the `withOrgsInParallel` step passes the `org` value into the closure:
```
#!/usr/bin/env groovy
@Library('sfdx-jenkins-shared-library')
import com.claimvantage.sjsl.Help
import com.claimvantage.sjsl.Org
import com.claimvantage.sjsl.Package

node {
    stage("checkout") {
        ...
    }
    withOrgsInParallel() { org ->
        stage("${org.name} create") {
            createScratchOrg org
        }
        ...
    }
    stage("publish") {
        ...
    }
}
```

### createScratchOrg

[Creates a scratch org](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/createScratchOrg.groovy)
and adds values relating to that to the supplied `org` object for use by later steps. This step has to come before most other steps. The org is created with the minimum duration value of `--durationdays 1` as the number of active scatch orgs
is limited, and failing builds might not get to their **deleteScratchOrg** step. Details of the created org are output into the log via an **echo** step.

* _org_

  Required. An instance of Org that has it's `projectScratchDefPath` property set.
  
### createTrigger

[Creates a trigger](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/createTrigger.groovy)
and its corresponding meta data file in the org.

* _org_

  Required. An instance of Org that has it's `projectScratchDefPath` property set.

* _name_

  Required. The name of the trigger.

* _text_

  Required. The complete text of the trigger 
  
### deleteScratchOrg

[Deletes a scratch org](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/deleteScratchOrg.groovy) 
identified by values added to the [Org](src/com/claimvantage/jsl/Org.groovy) object by **createScratchOrg**. This step has to come after most other steps.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.
  
### installPackage

[Installs a package](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/installPackage.groovy)
into a scratch org. Package installs typically take 2 to 20 minutes depending on the package size.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.
  
* _package_

  Required. An instance of the [Package](src/com/claimvantage/jsl/Package.groovy) bean object
  whose properties identify the package version to install.
  
### processHelp

This is a [ClaimVantage proprietary stage](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/processHelp.groovy) that extracts
help content from Confluence, processes that content and then adds the content to Git so that
it can be pulled into a package via Git externals.

* _branch_

  The branch name for which this step runs.
  The default value is "master".

* _help_

  Required. An instance of the [Help](src/com/claimvantage/jsl/Help.groovy) bean object
  whose properties identify the help information.
  
### pushToOrg

[Pushes](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/pushToOrg.groovy) the components into a scratch org.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.

### retrieveExternals

If a `git_externals.json` file is in the repository root,
[uses git-externals](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/reterieveExternals.groovy) to pull in that content.
If no file is present, the step does nothing (and git-externals does not have to be installed).

### runApexTests

[Runs Apex tests](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/runApexTests.groovy) for an org and puts the test results in a unique folder
based on the name of the `org` object.
The test class names are also prefixed by that name so that when multiple orgs are tested,
the test results are presented separated by the name.

* _org_

  Required. An instance of Org that has been populated by **createScratchOrg**.
  
### withOrgsInParallel

Finds matching
[Scratch Org Definition File](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_scratch_orgs_def_file.htm)
files, and for each one uses the Jenkins Pipeline **parallel** step to [execute
the nested steps](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/vars/withOrgsInParallel.groovy). This allows multiple org configurations to be handled at the same time.

* _glob_

  The matching pattern used to find the
  [Scratch Org Definition File](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_scratch_orgs_def_file.htm)
  files. Each matched file results in a separate parallel build.
  The default value is "config/project-scratch-def.*.json"; this assumes that an extra part will be insered into the file names and that part is used as a name for the parallel work.

<a name="multiple"></a>
## Multiple Orgs

Each scratch org that is created and used (in parallel) is defined by a [Scratch Org Definition File](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_scratch_orgs_def_file.htm). The number of scratch orgs you can create per day is [limited by Salesforce](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_scratch_orgs.htm).

A single file, called `project-scratch-def.json`, might look like this:

```
{
    "orgName": "Jenkins Claims - (default)",
    "edition": "Developer",
    "namespace": "cve",
    "settings": {
        "orgPreferenceSettings": {
            "s1DesktopEnabled": true,
            "disableParallelApexTesting": true
        }
    }
}
```
Adding a second file called `project-scratch-def.person-accounts.json` (note the added `features` line) that looks like this:
```
{
    "orgName": "Jenkins Claims - (person-accounts)",
    "edition": "Developer",
    "namespace": "cve",
    "features": ["PersonAccounts"],
    "settings": {
        "orgPreferenceSettings": {
            "disableParallelApexTesting": true
        }
    }
  }
  ```
 will result in the org-specific steps running in parallel for both orgs. Adding more files will result in more parallel work.
 
Not everything required can be configured via the `project-scratch-def.json`, so there is also an extension point attribute in the **buildPackagePipeline** called _beforeTestStage_ where a closure can be added that executes arbitrary logic and is passed an `org`. Here is an example of using that extension point, in this case to setup platform encryption:
```
buildPackagePipeline(
    beforeTestStage: { org ->
        if (org.name == 'platform-encryption') {
            sh "sfdx force:user:permset:assign --permsetname Encryption --targetusername ${org.username}"
            sh "sfdx force:data:record:create --sobjecttype TenantSecret --values 'Description=Test' --targetusername ${org.username}"
            shWithResult "echo 'cve.SetupController.updateDefaultFieldEncryptedFlags(true);' | sfdx force:apex:execute --json --targetusername ${org.username}"
        }
    }
)
```

<a name="org"></a>
## Org Bean

The attributes of the [Org](https://github.com/claimvantage/sfdx-jenkins-shared-library/tree/master/src/com/claimvantage/sjsl/Org.groovy) object (in order of usefulness) are:

| Attribute | Description |
|:----------|:------------|
| `name` | Name of the scratch org. It is the unique part of the Scratch Org Definition File name extracted from the `projectScratchDefPath`. |
| `username` | Used to direct SFDX commands to the right org after the org has been created. |
| `password` | Allows interactive login if the org is kep after the build. |
| `instanceUrl` | Allows interactive login if the org is kep after the build. |
| `orgId` | Perhaps useful for looking up the scratch org in e.g. the Dev Hub. |
| `projectScratchDefPath` | The path to the specific Scratch Org Definition File. |
