#!/usr/bin/env groovy

def call(String zipfile) {

    echo "Deploy metadata package"

    shWithStatus "sfdx force:mdapi:deploy --zipfile ${zipfile} --wait 15"

    echo "Metadata deployed"
}