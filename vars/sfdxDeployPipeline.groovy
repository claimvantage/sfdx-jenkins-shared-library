#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {
    def packagesToInstall = parameters.packagesToInstall ?: []
    def sfdxUrlCredentialId = parameters.sfdxUrlCredentialId
    def unlockedPackagesToInstall = parameters.unlockedPackagesToInstall ?: []

    def deploymentOrg = new Org()

    pipeline {
        node {
            
            // We don't want the same deployment to run multiple times at same time
            // We also want to make sure we don't starve the job queue (limiting job to run up to a certain time)
            throttle([]) {
                timeout(time: 4, unit: 'HOURS') {

                    stage("Checkout") {
                        checkout(scm: scm)
                    }
                    stage("Authenticate to org") {
                        // TODO: add argument for credential id(s).
                        // TODO: not sure if is better to check if needs to be authenticated, first.
                        // TODO: not sure if needs to set as the default user
                        withCredentials([file(credentialsId: sfdxUrlCredentialId, variable: 'SFDX_URL')]) {
                            def authenticationResult = shWithResult('sfdx force:auth:sfdxurl:store --setalias="$JOB_NAME" --setdefaultusername --sfdxurlfile=$SFDX_URL --json')
                            deploymentOrg.alias = "${env.JOB_NAME}"
                            deploymentOrg.username = authenticationResult.username
                            deploymentOrg.orgId = authenticationResult.orgId
                            deploymentOrg.instanceUrl = authenticationResult.instanceUrl
                            echo("Successfully authorized ${authenticationResult.username} with org ID ${authenticationResult.orgId}")
                        }
                    }
                    stage("Install packages") {
                        if (packagesToInstall) {
                            def installedPackages = retrieveInstalledPackages()

                            for (p in packagesToInstall) {
                                if (shouldInstallPackage(packageVersionId: p.versionId, installedPackages: installedPackages)) {
                                    installPackage(org: deploymentOrg, package: p)
                                }
                            }
                        } else {
                            echo "No packages to install."
                        }
                    }

                    stage("Install Unlocked Packages") {
                        // Separate stage due to:
                        // 1. Salesforce not allowing querying package information without installation key - in case of any
                        // 2. We potentially can keep re-installing it, no harm, potentially desirable
                        // 3. Usually the Packages installed on previous step are dependencies for the packages at this stage
                        // 4. Some options such as upgradetype only available for Unlocked Packages
                        if (unlockedPackagesToInstall) {
                            for (p in unlockedPackagesToInstall) {
                                echo "Installing Unlocked Package: ${p.versionId}"
                                def authenticationResult = shWithResult(
                                    """sfdx force:package:install \
                                        --package="${p.versionId}" \
                                        --installationkey="${p.installationkey}" \
                                        --noprompt \
                                        --securitytype=AllUsers \
                                        --targetusername="${deploymentOrg.username}" \
                                        --wait=120 \
                                        --json
                                    """)
                            }
                        }
                    }

                    stage("Install unmanaged code") {
                        // TODO: define parameters.
                        // TODO: consider if multiple folders are supported
                        // sh "sfdx force:source:deploy --sourcepath ${DEPLOYDIR} --json --targetusername ${SF_ALIAS} --testlevel ${TEST_LEVEL}"
                    }

                    stage("Logout org") {
                        shWithStatus("sfdx force:auth:logout --noprompt --targetusername=${deploymentOrg.username}")
                    }

                    stage("Clean") {
                        // Always remove workspace and don't fail the build for any errors
                        cleanWs notFailBuild: true
                    }
                }
            }
        }
    }
}

def shouldInstallPackage(Map parameters = [:]) {
    def packageVersionId = parameters.packageVersionId
    def versionPossibleToInstall = retrievePackageVersionString(packageVersionId)
    def packageDefinition = retrievePackage(packageVersionId)
    def packageNamespace = packageDefinition.NamespacePrefix
    def packageName = packageDefinition.Name

    def installedPackages = parameters.installedPackages
    def installedVersion = installedPackages[packageNamespace]

    def result = isVersionPossibleToInstallMostRecent(versionPossibleToInstall, installedVersion)
    echo """
        Name: ${packageName}, Namespace: ${packageNamespace}
        Version to Install ${versionPossibleToInstall} > Installed Version ${installedVersion} ? ${result}"""

    return result
}

def retrievePackageVersionString(packageVersionId) {
    def v = retrievePackageVersion(packageVersionId)
    String result = "${v.MajorVersion}.${v.MinorVersion}.${v.PatchVersion}.${v.BuildNumber}"
    return result
}

def retrievePackageVersion(packageVersionIdOrAlias) {
    def packageVersionId = retrieveSfdxAlias(packageVersionIdOrAlias)
    def subscriberPackageVersion = shWithResult(
        """ \
        sfdx force:data:soql:query \
        --json \
        --usetoolingapi \
        --query " \
            SELECT Name, MajorVersion, MinorVersion, PatchVersion, BuildNumber, SubscriberPackageId
            FROM SubscriberPackageVersion
            WHERE Id = '${packageVersionId}'\"
        """)
    return subscriberPackageVersion.records[0]
}

// force:package:install accepts alias to install it, however currently there is no native way to get the package ID
def retrieveSfdxAlias(packageVersionIdOrAlias) {
    def packageVersionId = packageVersionIdOrAlias;
    def sfdxProject = 'sfdx-project.json'
    if (fileExists("${sfdxProject}")) {
        def data = readJSON(file:"${sfdxProject}")
        def isVersionIdAliasSet = data['packageAliases'] != null && data['packageAliases']["${packageVersionIdOrAlias}"]
        packageVersionId = isVersionIdAliasSet ? data['packageAliases']["${packageVersionIdOrAlias}"] : packageVersionIdOrAlias
    }
    return packageVersionId
}

def retrievePackage(packageVersionId) {
    def p = retrievePackageVersion(packageVersionId)
    def subscriberPackage = shWithResult """ \
        sfdx force:data:soql:query \
        --json \
        --usetoolingapi \
        --query " \
            SELECT Name, NamespacePrefix
            FROM SubscriberPackage 
            WHERE Id = '${p.SubscriberPackageId}'\"
    """
    return subscriberPackage.records[0]
}

def retrieveInstalledPackages() {
    // Using Tooling API instead of sfdx force:package:installed:list due to Salesforce query timeout issues
    def installedPackagesResults = shWithResult """ \
        sfdx force:data:soql:query \
        --json \
        --usetoolingapi \
        --query="
            SELECT
                Id,
                SubscriberPackage.NamespacePrefix,
                SubscriberPackage.Name,
                SubscriberPackageVersionId
                FROM InstalledSubscriberPackage"
    """

    def installedPackages = [:]
    for (installedSubscriberPackage in installedPackagesResults.records) {
        def packageVersionId = installedSubscriberPackage.SubscriberPackageVersionId
        def packageVersionString = retrievePackageVersionString(packageVersionId)
        def packageNamespace = installedSubscriberPackage.SubscriberPackage.NamespacePrefix
        installedPackages[packageNamespace] = packageVersionString
    }
    
    return installedPackages
}

def Boolean isVersionPossibleToInstallMostRecent(String versionPossibleToInstall, String installedVersion) {
    List verA = versionPossibleToInstall.tokenize('.')
    List verB = installedVersion.tokenize('.')
    def commonIndices = Math.min(verA.size(), verB.size())

    for (int i = 0; i < commonIndices; ++i) {
        def numA = verA[i].toInteger()
        def numB = verB[i].toInteger()
        // println("comparing $numA and $numB")

        if (numA != numB) {
            return numA > numB
        }
    }

    // If we got this far then all the common indices are identical, so whichever version is longer must be more recent
    return verA.size() > verB.size()
}