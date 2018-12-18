
#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {

    Org org = (Org) parameters.org
    String name = parameters.name
    String text = parameters.text
    
    String directory = 'force-app/main/default/triggers'
    shWithStatus "sfdx force:apex:trigger:create -triggername ${name} --outputdir ${directory} --json"
    
    String path = "${directory}/${name}.trigger"
    writeFile file: path, text: text
}
