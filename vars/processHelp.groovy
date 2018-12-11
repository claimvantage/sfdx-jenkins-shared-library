
#!/usr/bin/env groovy

def call(helpRepositoryName, rootPageId, spaceKey) {

    echo "Process help"
    
    def JPK = env.JENKINS_PRIVATE_KEY_ID
    def BRANCH_NAME = env.BRANCH_NAME
    def BUILD_NUMBER = env.BUILD_NUMBER

    withCredentials([sshUserPrivateKey(credentialsId: JPK, keyFileVariable: 'jenkins_private_key')]) {
    
        echo "... make sure fixer Jar is present"
        
        sh '''
        eval $(ssh-agent -s)
        ssh-add ${jenkins_private_key}
        if [ ! -d help-fixer-2 ]; then \
            git clone --depth 1 git@github.com:claimvantage/help-fixer-2.git; \
            cd help-fixer-2; \
            mvn package; \
            def HF_VERSION = `mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q`; \
            mv "target/ant-help-fixer2-$HF_VERSION.jar" ../hf.jar; \
            cd ..;
        fi
        '''
    }
    
    withCredentials([usernameColonPassword(credentialsId: CONFLUENCE_CRED_ID, variable: 'USERPASS')]) {
    
        echo "... extract from Confluence"
        
        sh """
        curl -u "$USERPASS" "https://wiki.claimvantage.com/rest/scroll-html/1.0/sync-export?exportSchemeId=-7F00010101621A20869A6BA52BC63995&rootPageId=${rootPageId}" > exportedHelp.zip
        """
    }

    withCredentials([sshUserPrivateKey(credentialsId: JPK, keyFileVariable: 'jenkins_private_key')]) {
    
        echo "... run fixer"
    
        // Backslashes needed for $ that are not tokens
        sh """
        java -jar hf.jar -s exportedHelp.zip -t optimizedHelp.zip -k ${spaceKey}
        if [ -d ${helpRepositoryName} ]; then rm -rf ${helpRepositoryName}; fi
        eval \$(ssh-agent -s)
        ssh-add ${jenkins_private_key}
        git clone git@github.com:claimvantage/${helpRepositoryName}.git
        which unzip || ( apt-get update -y && apt-get install unzip -y )
        unzip -o optimizedHelp.zip -d ${helpRepositoryName}
        """
        
        echo "... commit if necessary"

        // Avoid build breaking when nothing has changed so nothing to commit
        sh """
        cd ${helpRepositoryName}
        git add --all
        git config user.name "Jenkins"
        git config user.email "jenkins@claimvantage.com"
        if [ -z "\$(git status --porcelain)" ]; then
            echo "No help changes to commit"
        else 
            echo "Help changes to commit"
            git commit -m "Committed by Jenkins >> ${BRANCH_NAME} b#${BUILD_NUMBER}"
            eval \$(ssh-agent -s)
            ssh-add ${jenkins_private_key}
            git push
        fi
        cd ..
        rm -rf ${helpRepositoryName}
        """
    }
    
    return this
}
