#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Map parameters = [:], Closure body = null) {
    
    def glob = parameters.glob
    if (!glob) glob = 'config/project-scratch-def.*.json'
    echo "Finding scratch def files using expression ${glob}"
    
    withCredentials([file(credentialsId: env.JWT_CRED_ID_DH, variable: 'jwt_key_file')]) {
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
}
