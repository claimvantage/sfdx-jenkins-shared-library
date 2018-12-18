
#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:]) {
    
    def org = parameters.org
    def name = parameters.name
    def text = parameters.text
    
    echo "Create trigger ${name} in org ${org.name}"
    
    String directory = 'force-app/main/default/triggers'
    shWithStatus "sfdx force:apex:trigger:create -triggername ${name} --outputdir ${directory}"
    echo "... empty trigger created in org ${org.name}"
    
    String path = "${directory}/${name}.trigger"
    writeFile(file: path, text: text)
    echo "... trigger text written in org ${org.name}"
}
