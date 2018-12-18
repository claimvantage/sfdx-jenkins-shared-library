
#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {

    Org org = (Org) parameters.org
    String name = parameters.name
    String text = parameters.text
    
    shWithStatus "sfdx force:apex:trigger:create -n ${name} -d 'force-app/main/default/triggers'"
}
    
