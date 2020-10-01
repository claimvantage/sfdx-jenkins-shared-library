package com.claimvantage.sjsl
import java.util.regex.Matcher

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
    String alias
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

    /**
     * Extracted from projectScratchDefPath
     * e.g. "content-notes"
     * First naming convention: "project-scratch-def.content-notes.json", not compatible with VS code pattern. Regex: .*?project-scratch-def.(.*?).json
     * Second naming convention: "project-content-notes-scratch-def.json", compatible with VS code pattern. Regex: .*?project-(.*?)-scratch-def.json
     * Default naming definition: "project-scratch-def.json", returns default
     */
    String getName() {
        def pattern1 = projectScratchDefPath =~ /.*?project-scratch-def.(.*?).json/
        def pattern2 = projectScratchDefPath =~ /.*?project-(.*?)-scratch-def.json/
        
        if (pattern1) {
            return pattern1.group(1)
        } else if (pattern2) {
            return pattern2.group(1)
        }
        return 'default'
    }
}
