package com.claimvantage.sjsl

// Just a bean
class Package implements Serializable {
  
    String label
    String versionId
    String password
  
    // For named args case
    Package() {
    }
    
    Package(String label, String versionId, String password) {
        this.label = label
        this.versionId = versionId
        this.password = password
    }
}
