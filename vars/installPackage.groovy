import com.claimvantage.jsl.Org

def call(Org org, Package package) {
        
    // Name is just for info purposes in e.g. logs

    echo "Install package ${p.name}/${p.versionId}/${p.password} in org ${org.name}"

    shWithStatus "sfdx force:package:install --targetusername ${org.username} --package ${p.versionId} --installationkey ${p.password} --wait 15 --noprompt"

    echo "Installed package"
}
