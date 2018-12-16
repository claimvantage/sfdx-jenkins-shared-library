#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Map parameters = [:]) {
    
    Org org = (Org) parameters.org
    
    Package p
    if (parameters.package) {
        p = (Package) parameters.package;
    } else {
        p = new Package(parameters.name, parameters.versionId, parameters.password)
    }

    echo "Install package ${p.name}/${p.versionId}/${p.password} in org ${org.name}"

    shWithStatus "sfdx force:package:install --targetusername ${org.username} --package ${p.versionId} --installationkey ${p.password} --wait 15 --noprompt"

    echo "Installed package"
}
