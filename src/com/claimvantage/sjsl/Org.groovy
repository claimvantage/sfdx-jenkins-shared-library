package com.claimvantage.sjsl

// Holds information about a created scratch org
class Org implements Serializable {
    
    // This value is set
    // E.g. "config/project-scratch-def.encryption.json"
    String projectScratchDefPath
  
    // Filled in by scratch org creation e.g.
    // "00DS0000003O1sNMAS"
    // "test-drgwjqh3xsn0@example.com"
    // "g5$nPgf4!sdf<2"
    // "https://ability-saas-8856-dev-ed.lightning.force.com"
    String orgId
    String username
    String password
    String instanceUrl
    int durationDays = 1
    
    // For named args case
    Org() {
    }
    
    Org(String projectScratchDefPath) {
        this.projectScratchDefPath = projectScratchDefPath;
    }
    
    // Extracted from projectScratchDefPath e.g. "encryption"
    String getName() {
        def parts = projectScratchDefPath.split('\\.')
        println "Length ${parts.length}"
        return parts.length > 2 ? parts[parts.length - 2] : 'default'
    }
    
    // glob can be string (the find files glob) or map where key is branch regex and value is find files glob
    // globv is the same except the branch regex is negated
    static def findFiles(String branch, def glob, def globv) {
        
        Set globSet = globs(branch, glob, false)
        Set globvSet = globs(branch, globv, true)
            
        // ???
        
        return findFiles(glob: glob)
    }
                
    privte static Set globs(String branch, def obj, bool negate) {
        Set s = []
        if (obj instanceof Map) {
            Map m = obj
            m.each { k, v ->
                if (branch.matches(k)) {
                    if (!negate) s.add(v)
                } else {
                    if (negate) s.add(v)
                }
            }
        } else {
            s.add(obj)
        }
        return s
    }              
}
