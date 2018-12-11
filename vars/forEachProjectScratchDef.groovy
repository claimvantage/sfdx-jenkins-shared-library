#!/usr/bin/env groovy

def call(Map parameters = [:], body) {
    
    for (def file : findFiles(glob: 'config/project-scratch-def.*.json')) {
        echo "Found ${file.path}"
        body()
    }
    
    return this
}
