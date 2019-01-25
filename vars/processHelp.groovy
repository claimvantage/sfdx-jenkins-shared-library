#!/usr/bin/env groovy
import com.claimvantage.sjsl.Help

def call(Map parameters = [:]) {
    
    Help h
    if (parameters.help) {
        h = (Help) parameters.help;
    } else {
        h = new Help(parameters.spaceKey, parameters.rootPageId, parameters.repository)
    }
    
    String branch = parameters.branch ?: 'master'
    
    echo "Process help ${h.spaceKey}/${h.rootPageId} into ${h.repository} only for branch ${branch}"
    
    if (env.BRANCH_NAME == branch) {

        sshagent (credentials: [env.GITHUB_CREDENTIAL_ID]) {

            echo "... make sure fixer Jar is present"

            // Backslashes needed for $ that are not tokens inside double quotes
            sh '''
            if [ ! -d help-fixer-2 ]; then \
                git clone --depth 1 git@github.com:claimvantage/help-fixer-2.git; \
                cd help-fixer-2; \
                export MAVEN_OPTS="-Xmx256m -XX:MaxPermSize=128m"; \
                mvn package; \
                HF_VERSION=`mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q`; \
                mv "target/ant-help-fixer2-\$HF_VERSION.jar" ../hf.jar; \
                cd ..;
            fi
            '''
        }

        withCredentials([usernameColonPassword(credentialsId: env.CONFLUENCE_CREDENTIAL_ID, variable: 'USERPASS')]) {

            echo "... extract from Confluence"

            sh """
            curl -u "$USERPASS" "https://wiki.claimvantage.com/rest/scroll-html/1.0/sync-export?exportSchemeId=-7F00010101621A20869A6BA52BC63995&rootPageId=${h.rootPageId}" > exportedHelp.zip
            """
        }

        sshagent (credentials: [env.GITHUB_CREDENTIAL_ID]) {

            echo "... run fixer"

            // Backslashes needed for $ that are not tokens inside all of this script
            sh """
            java -jar hf.jar -s exportedHelp.zip -t optimizedHelp.zip -k ${h.spaceKey}
            if [ -d ${h.repository} ]; then rm -rf ${h.repository}; fi
            git clone git@github.com:claimvantage/${h.repository}.git
            which unzip || ( apt-get update -y && apt-get install unzip -y )
            unzip -o optimizedHelp.zip -d ${h.repository}
            """

            echo "... commit if necessary"

            // Avoid build breaking when nothing has changed so nothing to commit
            sh """
            cd ${h.repository}
            git add --all
            git config user.name "Jenkins"
            git config user.email "jenkins@claimvantage.com"
            if [ -z "\$(git status --porcelain)" ]; then
                echo "No help changes to commit"
            else 
                echo "Help changes to commit"
                git commit -m "Committed by Jenkins >> ${env.BRANCH_NAME} b#${env.BUILD_NUMBER}"
                git push
            fi
            cd ..
            rm -rf ${h.repository}
            """
        }
    } else {
        echo "... not processed because branch name was ${env.BRANCH_NAME}"
    }
    
    return this
}
