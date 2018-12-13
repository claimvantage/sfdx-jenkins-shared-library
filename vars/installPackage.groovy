import com.claimvantage.jsl.Org

// Name is just for info purposes
def call(Map parameters = [:]) {
    
    def name = parameters.get('name')
    def versionId = parameters.get('versionId')
    def password = parameters.get('password')
    
    echo "Called............"

    // echo "Install package ${name} in org ${org.name}"

    // shWithStatus "sfdx force:package:install --targetusername ${org.username} --package ${versionId} --installationkey ${password} --wait 15 --noprompt"
    
    // echo "Installed package ${name} in org ${org.name}"
}
