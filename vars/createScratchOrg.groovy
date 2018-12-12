#!/usr/bin/env groovy

// Global???
SFDC_USERNAME = ''

def call() {

    echo "Create scratch org"
    
    def HUB_ORG = env.HUB_ORG_DH
    def SFDC_HOST = env.SFDC_HOST_DH
    def JWT_KEY_CRED_ID = env.JWT_CRED_ID_DH
    def CONNECTED_APP_CONSUMER_KEY = env.CONNECTED_APP_CONSUMER_KEY_DH
    
    def SFDC_USERNAME
    
    withCredentials([file(credentialsId: JWT_KEY_CRED_ID, variable: 'jwt_key_file')]) {

        shWithStatus "sfdx force:auth:jwt:grant --clientid ${CONNECTED_APP_CONSUMER_KEY} --username ${HUB_ORG} --jwtkeyfile ${jwt_key_file} --setdefaultdevhubusername --instanceurl ${SFDC_HOST}"
        
        def obj = shWithResult "sfdx force:org:create --definitionfile config/project-scratch-def.json --json --setdefaultusername"
        SFDC_USERNAME = obj.result.username
        
        echo "Username for scratch org is ${SFDC_USERNAME}"
    }
    
    return this
}
