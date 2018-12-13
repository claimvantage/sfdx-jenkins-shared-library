package com.claimvantage.jsl

class Org implements Serializable {
    
    Org(String projectScratchDefPath) {
        this.projectScratchDefPath = projectScratchDefPath;
    }
    
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
    
    // Make it easy to see org name in messages such as stage messages
    String toString() {
        return getName()
    }
    
    // Extracted from projectScratchDefPath e.g. "encryption"
    String getName() {
        def parts = projectScratchDefPath.split('\\.')
        return parts.length > 2 ? parts[parts.length - 2] : '_default_'
    }
}
