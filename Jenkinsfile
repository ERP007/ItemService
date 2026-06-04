pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        SERVICE_NAME = 'item-service'
        SERVICE_DIR = '/home/taehyung/apps/msa-server/item-service'
        INFRA_DIR = '/home/taehyung/apps/msa-server/infra'
        COMPOSE_FILE = 'docker-compose.yml'
        COMPOSE_PROJECT = 'msa-server'
        API_BASE_URL = 'https://api.erp007.xyz'
    }

    stages {
        stage('Test and package') {
            steps {
                sh 'docker build --target build -t "erp007-ci-check-${SERVICE_NAME}:${BUILD_NUMBER}" .'
            }
        }

        stage('Sync server repos') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-kt-jenkins-pat', usernameVariable: 'GITHUB_USERNAME', passwordVariable: 'GITHUB_TOKEN')]) {
                    sh '''
                        set -eu
                        auth_header="$(printf '%s:%s' "$GITHUB_USERNAME" "$GITHUB_TOKEN" | base64 | tr -d '\\n')"

                        cd "$INFRA_DIR"
                        git -c "http.extraHeader=Authorization: Basic ${auth_header}" fetch origin main
                        git checkout main
                        git -c "http.extraHeader=Authorization: Basic ${auth_header}" pull --ff-only origin main

                        cd "$SERVICE_DIR"
                        git -c "http.extraHeader=Authorization: Basic ${auth_header}" fetch origin main
                        git checkout main
                        git -c "http.extraHeader=Authorization: Basic ${auth_header}" pull --ff-only origin main
                    '''
                }
            }
        }

        stage('Deploy service') {
            when { branch 'main' }
            steps {
                sh '''
                    set -eu
                    cd "$INFRA_DIR"
                    ./scripts/init-server-secrets.sh
                    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT" config >/tmp/msa-server-compose.yml
                    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT" up -d item-postgres redis
                    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT" up -d --build --no-deps "$SERVICE_NAME"
                '''
            }
        }

        stage('Health check') {
            when { branch 'main' }
            steps {
                sh '''
                    set -eu
                    curl -fsS --retry 10 --retry-delay 3 --max-time 10 "${API_BASE_URL}/api/items/health" >/dev/null
                '''
            }
        }
    }

    post {
        always {
            sh '''
                docker image rm "erp007-ci-check-${SERVICE_NAME}:${BUILD_NUMBER}" >/dev/null 2>&1 || true
            '''
        }
    }
}
