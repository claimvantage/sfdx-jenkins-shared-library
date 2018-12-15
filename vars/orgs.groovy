#!/usr/bin/env groovy
import com.claimvantage.jsl.Org
import java.util.ArrayList
import java.util.ArrayList

List<Org> call(Map parameters = [:]) {
    
    def glob = parameters.glob
    if (!glob) glob = 'config/project-scratch-def.*.json'

    List<Org> orgs = new ArrayList<Org>();
    
    for (def scratchDefFile in findFiles(glob: glob)) {
        Org org = new Org(scratchDefFile.path)
        orgs.add(org)
    }
    
    return orgs
}
