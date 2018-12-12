#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def glob = 'config/project-scratch-def.*.json'  // Default
def variable = 'org'                            // Default

def call(Map parameters = [:], Closure body = null) {
    
    for (def file : findFiles(glob: glob)) {
        echo "Found ${file.path}"
        def org = new Org(projectScratchDefPath: file.path)
        if (body) {
            body([variable: org])
        }
    }
    
    return this
}
