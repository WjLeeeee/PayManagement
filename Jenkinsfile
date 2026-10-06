pipeline {
    agent any

    environment {
        KEYSTORE_PASSWORD = credentials('keystore-password')
        KEY_ALIAS = credentials('key-alias')
        KEY_PASSWORD = credentials('key-password')
        HOLIDAY_API_KEY = credentials('HOLIDAY_API_KEY')
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
                    file(credentialsId: 'play-store-credentials', variable: 'PLAY_CREDENTIALS'),
                    file(credentialsId: 'google-services-json', variable: 'GOOGLE_SERVICES')
                ]) {
                    sh '''
                        # Copy credentials
                        cp $KEYSTORE_FILE KeyStorePath.jks
                        cp $PLAY_CREDENTIALS play-store-credentials.json
                        cp $GOOGLE_SERVICES androidApp/google-services.json

                        # Create local.properties with Android SDK path and API keys
                        echo "sdk.dir=/Users/leewoojin/Library/Android/sdk" > local.properties
                        echo "HOLIDAY_API_KEY=$HOLIDAY_API_KEY" >> local.properties
                    '''
                }
            }
        }

        stage('Bump Version Code') {
            steps {
                echo 'Bumping version code...'
                script {
                    // Shell로 현재 versionCode 추출
                    def currentVersion = sh(
                        script: "grep 'versionCode = ' androidApp/build.gradle.kts | sed 's/[^0-9]*//g'",
                        returnStdout: true
                    ).trim().toInteger()

                    def newVersion = currentVersion + 1

                    // sed로 versionCode 업데이트
                    sh "sed -i '' 's/versionCode = ${currentVersion}/versionCode = ${newVersion}/' androidApp/build.gradle.kts"

                    echo "Version code bumped: ${currentVersion} → ${newVersion}"

                    // 환경 변수로 저장
                    env.NEW_VERSION_CODE = newVersion.toString()
                }
            }
        }

        stage('Build AAB') {
            steps {
                echo 'Building Release AAB...'
                sh './gradlew clean :androidApp:bundleRelease'
            }
        }

        stage('Commit Version Code') {
            steps {
                echo 'Pushing version code bump...'
                withCredentials([usernamePassword(credentialsId: 'github-token', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        git config user.email "jenkins@local"
                        git config user.name "Jenkins CI"
                        git add androidApp/build.gradle.kts
                        git commit -m "Chore: bump versionCode to ${NEW_VERSION_CODE}" || echo "No changes to commit"
                        git push https://${GIT_USER}:${GIT_TOKEN}@github.com/WjLeeeee/PayManagement.git HEAD:develop
                    '''
                }
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
                rm -f androidApp/google-services.json
                rm -f local.properties
            '''
        }
    }
}
