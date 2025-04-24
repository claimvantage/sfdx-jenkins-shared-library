#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Org org) {

    echo "Delete ${org}"
    shWithStatus "sf org delete scratch --targetusername ${org.username} --no-prompt"
}
