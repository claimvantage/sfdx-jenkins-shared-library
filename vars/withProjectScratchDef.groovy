#!/usr/bin/env groovy

import com.claimvantage.jsl.Org

def call(Map parameters = [:], body) {
    
    for (def file : findFiles(glob: 'config/project-scratch-def.*.json')) {
        echo "Found ${file.path}"
        def org = new Org(file.path)
        withEnv(["ORG = ${org}"]) {
            body()
        }
    }
    
    return this
}
