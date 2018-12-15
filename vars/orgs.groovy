#!/usr/bin/env groovy
import com.claimvantage.jsl.Org

List<Org> call(String glob ) {

    List<Org> orgs = new ArrayList<Org>();
    
    for (def scratchDefFile in findFiles(glob: 'config/project-scratch-def.*.json')) {
        Org org = new Org(scratchDefFile.path)
        orgs.add(org)
    }
    
    return orgs
}
