#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org
import com.claimvantage.sjsl.Package

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

    def packageLabel = retrievePackageLabel p.versionId, org

    echo "Installing package ${packageLabel} (${p.versionId}) in org ${org.name}"
    shWithStatus "sfdx force:package:install --targetusername ${org.username} --package \"${p.versionId}\" --installationkey ${p.installationkey} --wait 30 --noprompt"

    echo "Installed package"
}

def retrievePackageLabel(packageVersionId, org) {
    
    def v = retrievePackageVersion packageVersionId, org
    def p = retrievePackage v.SubscriberPackageId, org
    
    String result = "${p.Name} v${v.MajorVersion}.${v.MinorVersion}.${v.PatchVersion}"
    return result
}

def retrievePackageVersion(packageVersionId, org) {
    packageVersionId = retrieveSfdxAlias packageVersionId
    def subscriberPackageVersion = shWithResult """ \
        sfdx force:data:soql:query \
        --targetusername ${org.username} \
        --json \
        --usetoolingapi \
        --query " \
            SELECT Name, MajorVersion, MinorVersion, PatchVersion, BuildNumber, SubscriberPackageId
            FROM SubscriberPackageVersion
            WHERE Id = '${packageVersionId}'\"
    """
    return subscriberPackageVersion.records[0]
}

def retrievePackage(packageId, org) {
    
    def subscriberPackage = shWithResult """ \
        sfdx force:data:soql:query \
        --targetusername ${org.username} \
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
        def isVersionIdAliasSet = data['packageAliases'] != null && data['packageAliases']["${versionId}"]
        return isVersionIdAliasSet ? data['packageAliases']["${versionId}"] : versionId
    }
    return versionId
}
