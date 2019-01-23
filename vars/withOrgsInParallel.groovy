#!/usr/bin/env groovy
import com.claimvantage.sjsl.Org

def call(Map parameters = [:], Closure body = null) {
    
    def g = parameters.glob
    def glob = g instanceof Map ? g[env.BRANCH_NAME] : g
    if (!glob) glob = 'config/project-scratch-def.*.json'
    echo "Finding scratch def files using expression ${glob}"
    
    def perOrgStages = [:]
    for (def scratchDefFile in findFiles(glob: glob)) {
        echo "Found scratch def file ${scratchDefFile.path}"
        Org org = new Org("${scratchDefFile.path}")
        perOrgStages["${org.name}"] = {
            body(org)
        }
    }
    parallel perOrgStages
}
