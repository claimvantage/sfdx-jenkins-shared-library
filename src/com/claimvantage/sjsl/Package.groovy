package com.claimvantage.sjsl

// Just a bean
class Package implements Serializable {
  
    String versionId
    String installationkey
  
    // For named args case
    Package() {
    }
    
    Package(String versionId, String installationkey) {
        this.versionId = versionId
        this.installationkey = installationkey
    }

    // Deprecated - use Package(String versionId, String installationkey) instead
    Package(String label, String versionId, String installationkey) {
        this.versionId = versionId
        this.installationkey = installationkey
    }
}
