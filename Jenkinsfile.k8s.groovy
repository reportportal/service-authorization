#!groovy
@Library('commons') _

def label = "worker-${UUID.randomUUID().toString()}"
println("label")
println("${label}")

podTemplate(
        label: "${label}",
        containers: [
                containerTemplate(name: 'docker', image: 'docker:dind', ttyEnabled: true, alwaysPullImage: true, privileged: true,
                        command: 'dockerd --host=unix:///var/run/docker.sock --host=tcp://0.0.0.0:2375 --storage-driver=overlay',
                        resourceRequestCpu: '300m',
                        resourceLimitCpu: '500m',
                        resourceRequestMemory: '512Mi',
                        resourceLimitMemory: '1024Mi'),
                containerTemplate(name: 'gradle', image: 'gradle:5.5.1-jdk11', command: 'cat', ttyEnabled: true,
                        resourceRequestCpu: '800m',
                        resourceLimitCpu: '1500m',
                        resourceRequestMemory: '2048Mi',
                        resourceLimitMemory: '3072Mi'),
                containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:v3.1.1', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.8', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'httpie', image: 'blacktop/httpie', command: 'cat', ttyEnabled: true)

        ],
        imagePullSecrets: ["regcred"],
        volumes: [
                emptyDirVolume(memory: false, mountPath: '/var/lib/docker'),
                secretVolume(mountPath: '/etc/.dockercreds', secretName: 'docker-creds')
        ]
) {

    node("${label}") {

        def srvRepo = "quay.io/reportportal/service-authorization"
        def srvVersion = "BUILD-${env.BUILD_NUMBER}"
        def tag = "$srvRepo:$srvVersion"

        def k8sDir = "kubernetes"
        def ciDir = "reportportal-ci"
        def appDir = "app"
        def k8sNs = "reportportal"

        parallel 'Checkout Infra': {
            stage('Checkout Infra') {
                sh 'mkdir -p ~/.ssh'
                sh 'ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts'
                sh 'ssh-keyscan -t rsa git.epam.com >> ~/.ssh/known_hosts'

                dir(k8sDir) {
                    git branch: "master", url: 'https://github.com/reportportal/kubernetes.git'
                }

                dir(ciDir) {
                    git credentialsId: 'epm-gitlab-key', branch: "master", url: 'git@git.epam.com:epmc-tst/reportportal-ci.git'
                }

            }
        }, 'Checkout Service': {
            stage('Checkout Service') {
                dir(appDir) {
                    checkout scm
                }
            }
        }

        util.scheduleRepoPoll()
        dockerUtil.init()
        helm.init()

        dir(appDir) {
            try {
                container('gradle') {
                    withEnv(['K8S=true']) {
                        stage('Build App') {
                            sh "gradle --build-cache build --full-stacktrace -P gcp -P buildNumber=$srvVersion"
                        }
                        stage('Test') {
                            sh "gradle --build-cache test --full-stacktrace"
                        }
                    }
                }
            } finally {
//                junit 'build/reports/**/*.xml'
                dependencyCheckPublisher pattern: 'build/reports/dependency-check-report.xml'

            }

            container('docker') {
                stage('Create Docker Image') {
                    sh "docker build -f docker/Dockerfile-dev-release -t $tag ."
                    sh "docker push $tag"

                }
            }

            sast('reportportal_services_sast', 'rp/carrier/config.yaml', 'service-authorization', false)
        }

        stage('Deploy to Dev') {
            helm.deploy("$k8sDir/reportportal/v5", ["uat.repository": srvRepo, "uat.tag": srvVersion], false) // without wait
        }

        stage('DVT Test') {
            def snapshotVersion = util.readProperty("app/gradle.properties", "version")
            helm.testDeployment("reportportal", "reportportal-uat", "$snapshotVersion-$srvVersion")
        }
    }

}
