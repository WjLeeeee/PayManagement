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
        }
        failure {
            echo '❌ Deployment failed!'
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
