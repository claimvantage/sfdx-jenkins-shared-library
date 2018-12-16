#!/usr/bin/env groovy
import com.claimvantage.jsl.Org
import com.claimvantage.jsl.Package

def call(Map parameters = [:]) {
    
    Org org = (Org) parameters.org
    
    Package p
    if (parameters.package) {
        // Org and Package object
        p = (Package) parameters.package;
    } else {
        // ... or all separate named parameters
        p = new Package(parameters.label, parameters.versionId, parameters.password)
    }

    echo "Install package ${p.label}/${p.versionId}/${p.password} in org ${org.name}"

    shWithStatus "sfdx force:package:install --targetusername ${org.username} --package ${p.versionId} --installationkey ${p.password} --wait 15 --noprompt"

    echo "Installed package"
}
