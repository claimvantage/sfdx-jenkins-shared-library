#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Map parameters = [:]) {
    
    Org org = (Org) parameters.org
        
    // Name is just for info purposes in e.g. logs
    def name = parameters.name
    def versionId = parameters.versionId
    def password = parameters.password

    echo "Install package ${name}/${versionId}/${password} in org ${org.name}"

    shWithStatus "sfdx force:package:install --targetusername ${org.username} --package ${versionId} --installationkey ${password} --wait 15 --noprompt"

    echo "Installed package"
}
