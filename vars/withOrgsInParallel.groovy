#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:], Closure body = null) {
    
    def g = parameters.glob
    def glob = g instanceof Map ? g[env.BRANCH_NAME] : g
    if (!glob) glob = 'config/project-scratch-def.*.json'
    echo "Finding scratch def files using expression ${glob}"
    
    // Create closures
    def delaySeconds = 0
    def perOrgStages = [:]
    for (def scratchDefFile in findFiles(glob: glob)) {
        def seconds = delaySeconds
        echo "Found scratch def file ${scratchDefFile.path}; will delay ${seconds} seconds"
        Org org = new Org("${scratchDefFile.path}")
        perOrgStages["${org.name}"] = {
            echo "Delaying ${seconds} seconds to spread parallel load"
            sleep seconds
            body(org)
        }
        delaySeconds += 180
    }
    
    // Run the closures in parallel
    parallel perOrgStages
}
