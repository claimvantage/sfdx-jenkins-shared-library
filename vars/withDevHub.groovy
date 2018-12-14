#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Closure body) {
    
    echo "Devhub context"
    
    /*
    if (body) {
        def HUB_ORG = env.HUB_ORG_DH
        def SFDC_HOST = env.SFDC_HOST_DH
        def JWT_KEY_CRED_ID = env.JWT_CRED_ID_DH
        def CONNECTED_APP_CONSUMER_KEY = env.CONNECTED_APP_CONSUMER_KEY_DH

        withCredentials([file(credentialsId: JWT_KEY_CRED_ID, variable: 'jwt_key_file')]) {
            // shWithStatus "sfdx force:auth:jwt:grant --clientid ${CONNECTED_APP_CONSUMER_KEY} --username ${HUB_ORG} --jwtkeyfile ${jwt_key_file} --setdefaultdevhubusername --instanceurl ${SFDC_HOST}"
            def config = [:]
            body.resolveStrategy = Closure.DELEGATE_FIRST
            body.delegate = config
            
            
            
            body()
        }
    }
    */
}
