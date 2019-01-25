#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org
import com.claimvantage.sjsl.Package
import groovy.json.JsonSlurperClassic

def call(Map parameters = [:]) {
    
    Org org = (Org) parameters.org
    
    Package p
    if (parameters.package) {
        // Org and Package object
        p = (Package) parameters.package;
    } else {
        // ... or all separate named parameters
        p = new Package(parameters.versionId, parameters.installationkey)
    }

    def packageLabel = retrievePackageLabel p.versionId

    echo "Installing package ${packageLabel} (${p.versionId}) in org ${org.name}"
    shWithStatus "sfdx force:package:install --targetusername ${org.username} --package ${p.versionId} --installationkey ${p.installationkey} --wait 15 --noprompt"

    echo "Installed package"
}

def retrievePackageLabel(packageVersionId) {
    
    def v = retrievePackageVersion packageVersionId
    def p = retrievePackage v.SubscriberPackageId
    
    String result = "${p.Name} v${v.MajorVersion}.${v.MinorVersion}.${v.PatchVersion}"
    return result
}

def retrievePackageVersion(packageVersionId) {
    packageVersionId = retrieveSfdxAlias packageVersionId
    def subscriberPackageVersion = shWithResult """ \
        sfdx force:data:soql:query \
        --json \
        --usetoolingapi \
        --query " \
            SELECT Name, MajorVersion, MinorVersion, PatchVersion, BuildNumber, SubscriberPackageId
            FROM SubscriberPackageVersion
            WHERE Id = '${packageVersionId}'\"
    """
    return subscriberPackageVersion.records[0]
}

def retrievePackage(packageId) {
    
    def subscriberPackage = shWithResult """ \
        sfdx force:data:soql:query \
        --json \
        --usetoolingapi \
        --query " \
            SELECT Name 
            FROM SubscriberPackage 
            WHERE Id = '${packageId}'\"
    """
    return subscriberPackage.records[0]
}
// force:package:install accepts alias to install it, however currently there is no native way to get the package ID
def retrieveSfdxAlias(versionId) {
    def sfdxProject = 'sfdx-project.json'
    if (fileExists("${sfdxProject}")) {
        def data = readJSON file:"${sfdxProject}"
        return data['packageAliases']["${versionId}"] ? data['packageAliases']["${versionId}"] : versionId
    }
    return versionId
}