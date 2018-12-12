package com.claimvantage.jsl;

class Org implements Serializable {
    
    // E.g. "config/project-scratch-def.encryption.json"
    String projectScratchDefPath;
  
    // Result of scratch org creation e.g. "test-drgwjqh3xsn0@example.com" and "g54ncgf4!sdf<2"
    String username;
    String password;
    
    // Make it easy to see org name in messages such as stage messages
    String toString() {
        return getName();
    }
    
    // Extracted from projectScratchDefPath e.g. "encryption"
    String getName() {
        def parts = projectScratchDefPath.split('\.')
        return parts.length > 2 ? parts[parls.length - 2] : null
    }
}
