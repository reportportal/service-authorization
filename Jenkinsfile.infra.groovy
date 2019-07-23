#!groovy

//String podTemplateConcat = "${serviceName}-${buildNumber}-${uuid}"
def label = "worker-${UUID.randomUUID().toString()}"
println("label")
println("${label}")

podTemplate(
        label: "${label}",
        containers: [
//                containerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:3.27-1-alpine', args: '${computer.jnlpmac} ${computer.name}'),
                containerTemplate(name: 'jdk', image: 'openjdk:8-jdk-alpine', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.8', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:latest', command: 'cat', ttyEnabled: true)
        ],
//        imagePullSecrets: ["regcred"],
        volumes: [
//                hostPathVolume(hostPath: '/data/volumes/tools/.m2/repository', mountPath: '/root/.m2/repository'),
//        persistentVolumeClaim(claimName: 'repository', mountPath: '/root/.m2/repository'),
                    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
//        hostPathVolume(mountPath: '/home/root/.gradle', hostPath: '/tmp/jenkins/.gradle'),
//                    hostPathVolume(mountPath: '/home/gradle/.gradle', hostPath: '/tmp/jenkins/.gradle')
        ]
) {

    node("${label}") {

        properties([
                pipelineTriggers([
                        pollSCM('H/10 * * * *')
                ])
        ])

        //load "$JENKINS_HOME/jobvars.env"


        stage('Checkout Infra') {
            sh 'mkdir -p ~/.ssh'
            sh 'ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts'
//            dir('kubernetes') {
//                git branch: "v5", url: 'https://github.com/reportportal/reportportal.git'
//
//            }
            dir('kubernetes') {
                git branch: "v5", url: 'https://github.com/reportportal/kubernetes.git'

            }
        }

        stage('Configure') {
            //sh "echo $QUAY_TOKEN | docker login -u $QUAY_USER --password-stdin quay.io"
        }

        stage('Checkout App') {
            dir('app') {
                checkout scm
            }
        }

        stage('Build') {
            dir('app') {
                container('jdk') {
                    stage('Build App') {
                        sh "./gradlew build --full-stacktrace"
                    }
                    stage('Test') {
                        sh "./gradlew test --full-stacktrace"
                    }
                    stage('Security/SAST') {
                        sh "./gradlew dependencyCheckAnalyze"
                    }
                }
            }
            stage('Build Docker Image') {
                dir('app') {
                    container('docker') {
                        //                    docker.withServer("$DOCKER_HOST") {
                        sh "docker build -f docker/Dockerfile-develop -t quay.io/reportportal/service-api:BUILD-${env.BUILD_NUMBER} ."
                        sh "docker push quay.io/reportportal/service-api:BUILD-${env.BUILD_NUMBER}"
//                    }
                    }

                }
            }
        }

    }
}

