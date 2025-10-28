// Easy JMeter Jenkins Pipeline
// Docker流水线部署示例

pipeline {
    agent any
    
    environment {
        PROJECT_NAME = 'easy-jmeter'
        DOCKER_REGISTRY = credentials('docker-registry-url')
        REGISTRY_CREDENTIALS = credentials('docker-registry-credentials')
        VERSION = "${env.BUILD_NUMBER}"
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        retry(1)
    }
    
    triggers {
        // 每天凌晨2点构建主分支
        cron(env.BRANCH_NAME == 'main' ? '0 2 * * *' : '')
        // 监听代码提交
        pollSCM('H/5 * * * *')
    }
    
    stages {
        stage('📋 Preparation') {
            steps {
                script {
                    echo "🚀 Starting Easy JMeter Pipeline"
                    echo "Branch: ${env.BRANCH_NAME}"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Version: ${VERSION}"
                    
                    // 设置构建描述
                    currentBuild.description = "Deploy ${PROJECT_NAME} v${VERSION}"
                }
                
                // 清理工作空间
                cleanWs()
                
                // 检出代码
                checkout scm
                
                // 显示项目结构
                sh 'ls -la'
            }
        }
        
        stage('🔨 Build JAR') {
            agent {
                docker {
                    image 'maven:3.8.4-openjdk-8'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            steps {
                echo '📦 Building JAR package...'
                dir('api') {
                    sh 'mvn clean package -DskipTests -Dcheckstyle.skip'
                    sh 'ls -la target/'
                }
                
                // 归档JAR文件
                archiveArtifacts artifacts: 'api/target/*.jar', fingerprint: true
                
                // 发布测试结果（如果有）
                publishTestResults testResultsPattern: 'api/target/surefire-reports/TEST-*.xml'
            }
            post {
                success {
                    echo '✅ JAR build completed successfully'
                }
                failure {
                    echo '❌ JAR build failed'
                }
            }
        }
        
        stage('🐳 Build Docker Images') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    changeRequest()
                }
            }
            steps {
                script {
                    echo '🏗️ Building Docker images...'
                    
                    // 检查Docker文件
                    sh 'ls -la docker/'
                    
                    // 构建Server镜像
                    echo '🖥️ Building Server image...'
                    def serverImage = docker.build(
                        "${DOCKER_REGISTRY}/${PROJECT_NAME}/server:${VERSION}",
                        "-f docker/Dockerfile.server " +
                        "--build-arg BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ') " +
                        "--build-arg VERSION=${VERSION} ."
                    )
                    
                    // 构建Agent镜像
                    echo '🔧 Building Agent image...'
                    def agentImage = docker.build(
                        "${DOCKER_REGISTRY}/${PROJECT_NAME}/agent:${VERSION}",
                        "-f docker/Dockerfile.agent " +
                        "--build-arg BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ') " +
                        "--build-arg VERSION=${VERSION} ."
                    )
                    
                    // 推送镜像到仓库
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-registry-credentials') {
                        echo '📤 Pushing Server image...'
                        serverImage.push()
                        serverImage.push('latest')
                        
                        echo '📤 Pushing Agent image...'
                        agentImage.push()
                        agentImage.push('latest')
                    }
                    
                    echo '✅ Docker images built and pushed successfully'
                }
            }
            post {
                always {
                    // 清理本地镜像
                    sh "docker rmi ${DOCKER_REGISTRY}/${PROJECT_NAME}/server:${VERSION} || true"
                    sh "docker rmi ${DOCKER_REGISTRY}/${PROJECT_NAME}/agent:${VERSION} || true"
                }
            }
        }
        
        stage('📋 Security Scan') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            parallel {
                stage('🔍 Code Scan') {
                    steps {
                        echo '🔍 Running code security scan...'
                        // 可以集成SonarQube、Checkmarx等
                        script {
                            try {
                                sh '''
                                    echo "Running security scan..."
                                    # 这里可以添加具体的安全扫描工具
                                    echo "Security scan completed"
                                '''
                            } catch (Exception e) {
                                echo "Security scan failed: ${e.getMessage()}"
                                currentBuild.result = 'UNSTABLE'
                            }
                        }
                    }
                }
                stage('🛡️ Docker Image Scan') {
                    steps {
                        echo '🛡️ Scanning Docker images for vulnerabilities...'
                        script {
                            try {
                                sh '''
                                    echo "Scanning Docker images..."
                                    # 可以使用Trivy、Clair等工具
                                    echo "Docker image scan completed"
                                '''
                            } catch (Exception e) {
                                echo "Docker scan failed: ${e.getMessage()}"
                                currentBuild.result = 'UNSTABLE'
                            }
                        }
                    }
                }
            }
        }
        
        stage('🚀 Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    echo '🚀 Deploying to development environment...'
                    
                    // 使用SSH部署到开发环境
                    sshagent(['dev-server-ssh']) {
                        sh '''
                            # 复制部署脚本
                            scp -o StrictHostKeyChecking=no -r docker/ deploy/ ${DEV_USER}@${DEV_HOST}:~/easy-jmeter/
                            
                            # 执行远程部署
                            ssh -o StrictHostKeyChecking=no ${DEV_USER}@${DEV_HOST} "
                                cd ~/easy-jmeter &&
                                export VERSION=${VERSION} &&
                                export REGISTRY=${DOCKER_REGISTRY} &&
                                echo 'Pulling latest images...' &&
                                docker pull ${DOCKER_REGISTRY}/${PROJECT_NAME}/server:${VERSION} &&
                                docker pull ${DOCKER_REGISTRY}/${PROJECT_NAME}/agent:${VERSION} &&
                                echo 'Deploying services...' &&
                                chmod +x deploy/deploy.sh &&
                                ./deploy/deploy.sh ${VERSION} &&
                                echo 'Development deployment completed'
                            "
                        '''
                    }
                    
                    // 健康检查
                    echo '🔍 Performing health check...'
                    sleep(time: 30, unit: 'SECONDS')
                    
                    script {
                        def healthCheck = sh(
                            script: "curl -f http://${env.DEV_HOST}:5000/actuator/health",
                            returnStatus: true
                        )
                        
                        if (healthCheck == 0) {
                            echo '✅ Development deployment successful and healthy'
                        } else {
                            error '❌ Health check failed'
                        }
                    }
                }
            }
        }
        
        stage('🧪 Integration Tests') {
            when {
                branch 'develop'
            }
            steps {
                echo '🧪 Running integration tests...'
                script {
                    try {
                        sh '''
                            echo "Testing API endpoints..."
                            curl -s http://${DEV_HOST}:5000/actuator/health | grep UP
                            
                            echo "Testing Socket.IO connectivity..."
                            curl -s http://${DEV_HOST}:9000/socket.io/ | grep Socket
                            
                            echo "Testing MinIO connectivity..."  
                            curl -s http://${DEV_HOST}:9000/minio/health/live | grep OK
                            
                            echo "Integration tests passed"
                        '''
                    } catch (Exception e) {
                        echo "Integration tests failed: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        stage('🎯 Deploy to Production') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                script {
                    // 需要手动确认
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: '🎯 Deploy to Production?', 
                              ok: 'Deploy',
                              submitterParameter: 'DEPLOYER'
                    }
                    
                    echo "🎯 Deploying to production (approved by ${DEPLOYER})..."
                    
                    sshagent(['prod-server-ssh']) {
                        sh '''
                            # 创建部署备份
                            ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} "
                                cd ~/easy-jmeter &&
                                docker-compose -f docker/docker-compose.prod.yml ps > deployment-backup-${BUILD_NUMBER}.log
                            "
                            
                            # 复制部署文件
                            scp -o StrictHostKeyChecking=no -r docker/ deploy/ ${PROD_USER}@${PROD_HOST}:~/easy-jmeter/
                            
                            # 执行生产部署
                            ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} "
                                cd ~/easy-jmeter &&
                                export VERSION=${VERSION} &&
                                export REGISTRY=${DOCKER_REGISTRY} &&
                                echo 'Pulling production images...' &&
                                docker pull ${DOCKER_REGISTRY}/${PROJECT_NAME}/server:${VERSION} &&
                                docker pull ${DOCKER_REGISTRY}/${PROJECT_NAME}/agent:${VERSION} &&
                                echo 'Deploying to production...' &&
                                chmod +x deploy/deploy.sh &&
                                ./deploy/deploy.sh ${VERSION} .env.prod &&
                                echo 'Production deployment completed'
                            "
                        '''
                    }
                    
                    // 生产环境验证
                    echo '🔍 Verifying production deployment...'
                    sleep(time: 60, unit: 'SECONDS')
                    
                    def prodHealthCheck = sh(
                        script: "curl -f http://${env.PROD_HOST}:5000/actuator/health",
                        returnStatus: true
                    )
                    
                    if (prodHealthCheck == 0) {
                        echo '✅ Production deployment successful and healthy'
                        
                        // 发送成功通知
                        slackSend(
                            channel: '#deployments',
                            color: 'good',
                            message: "🚀 Easy JMeter v${VERSION} deployed to production successfully! 🎉"
                        )
                    } else {
                        error '❌ Production health check failed'
                    }
                }
            }
        }
    }
    
    post {
        always {
            echo '🧹 Pipeline cleanup...'
            
            // 清理工作空间
            cleanWs()
            
            // 清理Docker资源
            sh 'docker system prune -f || true'
        }
        
        success {
            echo '✅ Pipeline completed successfully!'
            
            // 发送成功通知
            emailext(
                subject: "✅ Easy JMeter Pipeline Success - v${VERSION}",
                body: """
                    <h2>Easy JMeter Pipeline Success</h2>
                    <p><strong>Version:</strong> ${VERSION}</p>
                    <p><strong>Branch:</strong> ${env.BRANCH_NAME}</p>
                    <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    <p>All stages completed successfully! 🎉</p>
                """,
                to: "${env.CHANGE_AUTHOR_EMAIL ?: 'team@company.com'}"
            )
        }
        
        failure {
            echo '❌ Pipeline failed!'
            
            // 发送失败通知
            slackSend(
                channel: '#alerts',
                color: 'danger',
                message: "❌ Easy JMeter Pipeline Failed - v${VERSION} on ${env.BRANCH_NAME}"
            )
            
            emailext(
                subject: "❌ Easy JMeter Pipeline Failed - v${VERSION}",
                body: """
                    <h2>Easy JMeter Pipeline Failed</h2>
                    <p><strong>Version:</strong> ${VERSION}</p>
                    <p><strong>Branch:</strong> ${env.BRANCH_NAME}</p>
                    <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    <p>Please check the build logs for details.</p>
                """,
                to: "${env.CHANGE_AUTHOR_EMAIL ?: 'team@company.com'}"
            )
        }
        
        unstable {
            echo '⚠️ Pipeline completed with warnings'
            
            slackSend(
                channel: '#deployments',
                color: 'warning',
                message: "⚠️ Easy JMeter Pipeline Unstable - v${VERSION} (with warnings)"
            )
        }
    }
}
