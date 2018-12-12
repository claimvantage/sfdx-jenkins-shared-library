package com.claimvantage.jsl;

class Org implements Serializable {

    // E.g. "encryption"
    String name;
    
    // E.g. "config/project-scratch-def.encryption.json"
    String projectScratchDefPath;
  
    // Result of scratch org creation e.g. "test-drgwjqh3xsn0@example.com" and "g54ncgf4!sdf<2"
    String username;
    String password;
}
