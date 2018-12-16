#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Org org) {

    echo "Delete ${org}"
    shWithStatus "sfdx force:org:delete --targetusername ${org.username} --noprompt"
}
