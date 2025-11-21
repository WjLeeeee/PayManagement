pipeline {
    agent any

    environment {
        KEYSTORE_PASSWORD = credentials('keystore-password')
        KEY_ALIAS = credentials('key-alias')
        KEY_PASSWORD = credentials('key-password')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }

        stage('Setup Credentials') {
            steps {
                echo 'Setting up credentials...'
                withCredentials([
                    file(credentialsId: 'keystore-file', variable: 'KEYSTORE_FILE'),
                    file(credentialsId: 'play-store-credentials', variable: 'PLAY_CREDENTIALS')
                ]) {
                    sh '''
                        # Copy credentials
                        cp $KEYSTORE_FILE KeyStorePath.jks
                        cp $PLAY_CREDENTIALS play-store-credentials.json

                        # Create local.properties with Android SDK path
                        echo "sdk.dir=/Users/leewoojin/Library/Android/sdk" > local.properties
                    '''
                }
            }
        }

        stage('Bump Version Code') {
            steps {
                echo 'Bumping version code...'
                script {
                    def buildFile = 'androidApp/build.gradle.kts'
                    def content = readFile(buildFile)

                    // 현재 versionCode 추출
                    def matcher = content =~ /versionCode = (\d+)/
                    if (matcher.find()) {
                        def currentVersion = matcher[0][1].toInteger()
                        def newVersion = currentVersion + 1

                        // versionCode 업데이트
                        content = content.replaceFirst(/versionCode = \d+/, "versionCode = ${newVersion}")
                        writeFile file: buildFile, text: content

                        echo "Version code bumped: ${currentVersion} → ${newVersion}"

                        // 환경 변수로 저장 (디스코드 알림에서 사용)
                        env.NEW_VERSION_CODE = newVersion.toString()
                    }
                }
            }
        }

        stage('Build AAB') {
            steps {
                echo 'Building Release AAB...'
                sh './gradlew clean :androidApp:bundleRelease'
            }
        }

        stage('Deploy to Play Store') {
            steps {
                echo 'Deploying to Play Store Internal Track...'
                sh './gradlew :androidApp:publishReleaseBundle'
            }
        }
    }

    post {
        success {
            echo '✅ Deployment successful!'
            echo 'AAB uploaded to Play Store Internal Track (COMPLETED)'

            script {
                // Git에 versionCode 변경사항 커밋
                withCredentials([usernamePassword(credentialsId: 'github-token', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        git config user.email "jenkins@local"
                        git config user.name "Jenkins CI"
                        git add androidApp/build.gradle.kts
                        git commit -m "Chore: bump versionCode to ${NEW_VERSION_CODE}" || echo "No changes to commit"
                        git push https://${GIT_USER}:${GIT_TOKEN}@github.com/WjLeeeee/PayManagement.git HEAD:develop
                    '''
                }

                withCredentials([string(credentialsId: 'discord-webhook', variable: 'WEBHOOK_URL')]) {
                    def message = """
{
  "embeds": [{
    "title": "✅ 배포 성공!",
    "description": "PayManagement 앱이 성공적으로 배포되었습니다.",
    "color": 65280,
    "fields": [
      {"name": "📱 앱 이름", "value": "PayManagement", "inline": true},
      {"name": "📦 배포 트랙", "value": "Internal Testing", "inline": true},
      {"name": "🔢 버전", "value": "versionCode ${env.NEW_VERSION_CODE}", "inline": true},
      {"name": "✨ 상태", "value": "COMPLETED (자동 출시)", "inline": true},
      {"name": "⏰ 배포 시간", "value": "${new Date().format('yyyy-MM-dd HH:mm:ss')}", "inline": false},
      {"name": "📥 앱 다운로드", "value": "[Play Store에서 다운로드](https://play.google.com/apps/internaltest/4701606329859714866)", "inline": false}
    ],
    "footer": {"text": "Jenkins CI/CD"},
    "timestamp": "${new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")}"
  }]
}
"""
                    sh """
                        curl -H "Content-Type: application/json" \
                             -X POST \
                             -d '${message}' \
                             \${WEBHOOK_URL}
                    """
                }
            }
        }

        failure {
            echo '❌ Deployment failed!'

            script {
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'WEBHOOK_URL')]) {
                    def message = """
{
  "embeds": [{
    "title": "❌ 배포 실패!",
    "description": "PayManagement 앱 배포 중 오류가 발생했습니다.",
    "color": 16711680,
    "fields": [
      {"name": "📱 앱 이름", "value": "PayManagement", "inline": true},
      {"name": "⚠️ 상태", "value": "빌드 실패", "inline": true},
      {"name": "⏰ 실패 시간", "value": "${new Date().format('yyyy-MM-dd HH:mm:ss')}", "inline": false},
      {"name": "🔗 에러 로그", "value": "[Jenkins에서 확인](${env.BUILD_URL}console)", "inline": false}
    ],
    "footer": {"text": "Jenkins CI/CD"},
    "timestamp": "${new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")}"
  }]
}
"""
                    sh """
                        curl -H "Content-Type: application/json" \
                             -X POST \
                             -d '${message}' \
                             \${WEBHOOK_URL}
                    """
                }
            }
        }

        cleanup {
            echo 'Cleaning up credentials...'
            sh '''
                rm -f KeyStorePath.jks
                rm -f play-store-credentials.json
                rm -f local.properties
            '''
        }
    }
}
