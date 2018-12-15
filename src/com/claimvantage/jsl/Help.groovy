package com.claimvantage.jsl

// Just a bean
class Help implements Serializable {
  
    String repository
    String rootPageId
    String spaceKey
  
    // For named args case
    Help() {
    }
    
    Help(String repository, String rootPageId, String spaceKey) {
        this.repository = repository
        this.rootPageId = rootPageId
        this.spaceKey = spaceKey
    }
}
