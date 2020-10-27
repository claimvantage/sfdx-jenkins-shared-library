#!/usr/bin/env groovy

def call(Map parameters = [:]) {
    def packagesToInstall = parameters.packagesToInstall ?: []

    pipeline {
        node {
            // We want to set some properties, such as parameters
            // properties(
            //     propertiesConfigured
            // )
            
            // We don't want the same deployment to run multiple times at same time
            // We also want to make sure we don't starve the job queue (limiting job to run up to a certain time)
            throttle {
                timeout(time: 4, unit: 'HOURS') {
                    stage("Checkout") {
                        checkout(scm: scm)
                    }
                    stage("Install packages") {
                        // TODO: do we need to set targetusername??
                        def installedPackagesResult = shWithResult("sfdx force:package:installed:list --json")

                        for (p in packagesToInstall) {
                            if (shouldInstallPackage(packageVersionId: 'cvb v19', installedPackages: installedPackagesResult)) {
                                echo "Yes"
                            } else {
                                echo "No"
                            }
                        }
                    }

                    stage("Install unmanaged code") {
                    }
                }
            }
        }
    }
}

def shouldInstallPackage(Map parameters = [:]) {
    def packageVersionId = parameters.packageVersionId
    def versionPossibleToInstall = retrievePackageVersionString(packageVersionId)
    def namespace = retrievePackage(packageVersionId)

    def installedPackages = parameters.installedPackages
    for (p in installedPackages) {
        if (p.SubscriberPackageNamespace == namespace) {
            return versionPossibleToInstall > p.SubscriberPackageVersionNumber
        }
    }

    // If not installed, should install
    return true
}

def retrievePackageVersionString(packageVersionId) {
    def v = retrievePackageVersion(packageVersionId)
    String result = "${v.MajorVersion}.${v.MinorVersion}.${v.PatchVersion}.{v.BuildNumber}"
    return result
}

def retrievePackageVersion(packageVersionId) {
    packageVersionId = retrieveSfdxAlias packageVersionId
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
def retrieveSfdxAlias(versionId) {
    def sfdxProject = 'sfdx-project.json'
    if (fileExists("${sfdxProject}")) {
        def data = readJSON(file:"${sfdxProject}")
        def isVersionIdAliasSet = data['packageAliases'] != null && data['packageAliases']["${versionId}"]
        return isVersionIdAliasSet ? data['packageAliases']["${versionId}"] : versionId
    }
    return versionId
}

def retrievePackage(packageVersionId) {
    def p = retrievePackageVersion(packageVersionId)
    def subscriberPackage = shWithResult """ \
        sfdx force:data:soql:query \
        --json \
        --usetoolingapi \
        --query " \
            SELECT Name 
            FROM SubscriberPackage 
            WHERE Id = '${p.SubscriberPackageId}'\"
    """
    return subscriberPackage.records[0].Name
}