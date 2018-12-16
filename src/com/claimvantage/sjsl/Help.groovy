package com.claimvantage.sjsl

// Just a bean
class Help implements Serializable {
  
    String spaceKey
    String rootPageId
    String repository
  
    // For named args case
    Help() {
    }
    
    Help(String spaceKey, String rootPageId, String repository) {
        this.spaceKey = spaceKey
        this.rootPageId = rootPageId
        this.repository = repository
    }
}
