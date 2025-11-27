
// declarative pipeline
// desired result: Github merge/commit on said branch (after CI comp) ->  pull latest code + author + version ->
// build new image -> clean old cont -> new cont -> if success, clean old image
// if fail, use old image and delete new one -> telegram message (every outcome)

pipeline {
    agent { label 'test-env' } // gira solo su nodo test-env

    // trigger: git push origin HEAD:test --tags
    //TBD: if success docker compose build nginx....
    // /root/diario-di-bordo-infr/frontend
    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'test', description: 'Branch to watch and pull')
        string(name: 'REPO_URL', defaultValue: 'git@github.com:gerserk/argo-events-telegram-warn.git', description: 'GitHub repository URL')
        string(name: 'APP_PATH', defaultValue: '/root/frontend', description: 'Path to application on host')
        string(name: 'REPO_NAME', defaultValue:'argo-events-telegram-warn', description:'name of repo')
    }

    stages {
        stage('pre execution notification') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'telegram-secrets',
                    usernameVariable: 'TELEGRAM_CHAT_ID',
                    passwordVariable: 'TELEGRAM_BOT_TOKEN'
                )]){
                    sh """
                        MSG="🚨 New deploy of <metti front o backend> starting!"
                        TELEGRAM_API="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage" 

                        curl -s -X POST "\${TELEGRAM_API}" \
                            -d chat_id="${TELEGRAM_CHAT_ID}" \
                            -d text="\${MSG}" \
                            -d parse_mode="Markdown" \
                            -H "Content-Type: application/x-www-form-urlencoded"
                    """
                }
            }
        }

        stage('Update Code') {
            steps {
                sshagent(['github-key']) {
                    script {
                        echo "Pulling from branch: ${params.BRANCH_NAME}"
                        echo "Repository: ${params.REPO_URL}"

                        dir("${params.APP_PATH}") {
                            sh """

                                if [ ! -d ${REPO_NAME} ]; then
                                    git clone ${params.REPO_URL}
                                fi
                                cd ${params.REPO_NAME}
                                echo $pwd
                                git fetch --prune
                                git fetch --tags
                                git checkout ${BRANCH_NAME}
                                VERSION=\$(git tag -l '${BRANCH_NAME}*' | sort -V | tail -1)
                                echo \$VERSION
                                git checkout \$VERSION
                            """
                        }                    
                    }
                }
            }
        }
        
    }
    
    post {
        success {
        script {
            withCredentials([usernamePassword(
                credentialsId: 'telegram-secrets',
                usernameVariable: 'TELEGRAM_CHAT_ID',
                passwordVariable: 'TELEGRAM_BOT_TOKEN'
            )]) {
                dir("${params.APP_PATH}/${params.REPO_NAME}") {
                    def commitMsg = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
                    def author = sh(returnStdout: true, script: 'git log -1 --pretty=%an').trim()
                    def version = sh(returnStdout: true, script: """git tag -l '${params.BRANCH_NAME}*' | sort -V | tail -1""").trim()
                    
                    sh """
                        cd /root/diario-di-bordo-infr
                        docker compose up -d --build nginx
                        docker image prune -a
                    """

                    sh """
                        MSG="Deploy of <metti front o back> succeeded! 
                        Author: ${author}
                        Version: ${version}
                        Commit: ${commitMsg}
                        "
                        TELEGRAM_API="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" 
                        curl -s -X POST "\${TELEGRAM_API}" \
                            -d chat_id="${TELEGRAM_CHAT_ID}" \
                            -d text="\${MSG}" \
                            -d parse_mode="Markdown" \
                            -H "Content-Type: application/x-www-form-urlencoded"
                    """
                }
            }
        }
    }
        // TBD:
        // - manda anche log come allegato
        // - rollback a versione precedente
        failure {
            script {
            withCredentials([usernamePassword(
                credentialsId: 'telegram-secrets',
                usernameVariable: 'TELEGRAM_CHAT_ID',
                passwordVariable: 'TELEGRAM_BOT_TOKEN'
            )]) {
                dir("${params.APP_PATH}/${params.REPO_NAME}") {
                    def logs = currentBuild.rawBuild.getLog(1000)
                    def logFile = "latestJknsLogs.log"
                    writeFile file: logFile, text: logs.join('\n')

                    sh """

                        cat ${logFile}
                        MSG="Something went wrong! depoly failed"
                        TELEGRAM_API="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" 
                        curl -s -X POST "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendDocument" \
                        -F chat_id="$TELEGRAM_CHAT_ID" \
                        -F document=@"${logFile}"  \
                        -F caption="🚨 UI version ${params.vers_ui} deploy Failed - See attached log"

                    """
                }
            }
        }
        }
    }
}