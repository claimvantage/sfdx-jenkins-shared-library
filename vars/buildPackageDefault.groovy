#!/usr/bin/env groovy
import com.claimvantage.jsl.Help
import com.claimvantage.jsl.Package

def call(Map parameters = [:]) {
    
    Help h
    if (parameters.help) {
        h = (Help) parameters.help;
    }
    
    Package[] ps
    if (parameters.packages) {
        ps = (Package[]) parameters.packages;
    }
}
