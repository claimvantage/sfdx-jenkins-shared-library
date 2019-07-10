package com.claimvantage.sjsl

// Just a bean
class Help implements Serializable {
  
    String spaceKey
    String rootPageId
    String repository
    Boolean forceDownloadHelpFixer
    String[] helpFixerParams


    // For named args case
    Help() {
    }

    Help(String spaceKey, String rootPageId, String repository, String... helpFixerParams) {
        this(spaceKey, rootPageId, repository, false, helpFixerParams)
    }
    
    Help(String spaceKey, String rootPageId, String repository, Boolean forceDownloadHelpFixer, String... helpFixerParams) {
        this.spaceKey = spaceKey
        this.rootPageId = rootPageId
        this.repository = repository
        this.forceDownloadHelpFixer = forceDownloadHelpFixer
        this.helpFixerParams = helpFixerParams
    }
}
