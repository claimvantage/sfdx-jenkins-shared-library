#!/usr/bin/env groovy

def call(Map parameters = [:]) {
    
    def repository = parameters.get('repository')
    def rootPageId = parameters.get('rootPageId')
    def spaceKey = parameters.get('spaceKey')
    
    echo "Process help from ${repository}"
    
    def jpk = env.JENKINS_PRIVATE_KEY_ID
    def branchName = env.BRANCH_NAME
    def buildNumber = env.BUILD_NUMBER
    def confluenceCredentialsId = env.JENKINS_CONFLUENCE_CREDENTIALS_ID
    
    if (BRANCH_NAME == 'master') {

        withCredentials([sshUserPrivateKey(credentialsId: jpk, keyFileVariable: 'jenkins_private_key')]) {

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

        withCredentials([usernameColonPassword(credentialsId: confluenceCredentialsId, variable: 'USERPASS')]) {

            echo "... extract from Confluence"

            sh """
            curl -u "$USERPASS" "https://wiki.claimvantage.com/rest/scroll-html/1.0/sync-export?exportSchemeId=-7F00010101621A20869A6BA52BC63995&rootPageId=${rootPageId}" > exportedHelp.zip
            """
        }

        withCredentials([sshUserPrivateKey(credentialsId: jpk, keyFileVariable: 'jenkins_private_key')]) {

            echo "... run fixer"

            // Backslashes needed for $ that are not tokens
            sh """
            java -jar hf.jar -s exportedHelp.zip -t optimizedHelp.zip -k ${spaceKey}
            if [ -d ${repository} ]; then rm -rf ${repository}; fi
            eval \$(ssh-agent -s)
            ssh-add ${jenkins_private_key}
            git clone git@github.com:claimvantage/${repository}.git
            which unzip || ( apt-get update -y && apt-get install unzip -y )
            unzip -o optimizedHelp.zip -d ${repository}
            """

            echo "... commit if necessary"

            // Avoid build breaking when nothing has changed so nothing to commit
            sh """
            cd ${repository}
            git add --all
            git config user.name "Jenkins"
            git config user.email "jenkins@claimvantage.com"
            if [ -z "\$(git status --porcelain)" ]; then
                echo "No help changes to commit"
            else 
                echo "Help changes to commit"
                git commit -m "Committed by Jenkins >> ${branchName} b#${buildNumber}"
                eval \$(ssh-agent -s)
                ssh-add ${jenkins_private_key}
                git push
            fi
            cd ..
            rm -rf ${repository}
            """
        }
    } else {
        echo "... not processed because branch name was ${branchName}"
    }
    
    return this
}
