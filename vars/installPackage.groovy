import com.claimvantage.jsl.Org

def call(Map parameters = [:]) {
    
    // Name is just for info purposes in e.g. logs
    Org org = (Org) parameters.get('org');
    def name = parameters.get('name')
    def versionId = parameters.get('versionId')
    def password = parameters.get('password')
    
    echo "Install package ${name} in org ${org.name}"

    shWithStatus "sfdx force:package:install --targetusername ${org.username} --package ${versionId} --installationkey ${password} --wait 15 --noprompt"
    
    echo "Installed package ${name} in org ${org.name}"
}
