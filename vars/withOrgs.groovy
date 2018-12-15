#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

def call(Closure body = null) {
    
    def variable = 'ORG'
    
    def workspaceRoot = "${env.WORKSPACE}"
    def perOrgStages = [:]
    for (def scratchDefFile in findFiles(glob: 'config/project-scratch-def.*.json')) {
        Org org = new Org(scratchDefFile.path)
        perOrgStages["${org.name}"] = {
            ws(dir: "${workspaceRoot}/${org.name}") {
                withCredentials([file(credentialsId: env.JWT_CRED_ID_DH, variable: 'jwt_key_file')]) {
                    if (body) {
                        def config = [:]
                        body.resolveStrategy = Closure.DELEGATE_FIRST
                        body.delegate = config
                        body()
                    }
                }
            }
        }
    }
    parallel perOrgStages
    
    /*
    for (def file : findFiles(glob: glob)) {
        echo "Found ${file.path}"
        def org = new Org(projectScratchDefPath: file.path)
        if (body) {
            body([variable: org])
        }
    }
    
    return this
    */
}
