#!/usr/bin/env groovy

def call(Map parameters = [:], body) {
    
    for (def file : findFiles(glob: 'config/project-scratch-def.*.json')) {
        echo "Found ${file.path}"
        // TODO pull out part that the * matched
        // Probably OK in closure if a def is used to capture the value
        def org = file.name
        withEnv(["ORG = ${org}"]) {
            body()
        }
    }
    
    return this
}
