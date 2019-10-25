#!groovy

//String podTemplateConcat = "${serviceName}-${buildNumber}-${uuid}"
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
                containerTemplate(name: 'gradle', image: 'gradle:5.5.1-jdk8', command: 'cat', ttyEnabled: true,
                        resourceRequestCpu: '800m',
                        resourceLimitCpu: '1500m',
                        resourceRequestMemory: '2048Mi',
                        resourceLimitMemory: '3072Mi'),
                containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:v2.14.2', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.8', command: 'cat', ttyEnabled: true),
                containerTemplate(name: 'httpie', image: 'blacktop/httpie', command: 'cat', ttyEnabled: true)

        ],
        imagePullSecrets: ["regcred"],
        volumes: [
                hostPathVolume(mountPath: '/home/gradle/.gradle', hostPath: '/tmp/jenkins/.gradle'),
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

        stage('Configure') {
            container('docker') {
                sh 'echo "Initialize environment"'
                sh """
                QUAY_USER=\$(cat "/etc/.dockercreds/username")
                cat "/etc/.dockercreds/password" | docker login -u \$QUAY_USER --password-stdin quay.io
                """
            }
        }

        parallel 'Checkout Infra': {
            stage('Checkout Infra') {
                sh 'mkdir -p ~/.ssh'
                sh 'ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts'
                sh 'ssh-keyscan -t rsa git.epam.com >> ~/.ssh/known_hosts'

                dir('kubernetes') {
                    git branch: "master", url: 'https://github.com/reportportal/kubernetes.git'
                }

                dir('reportportal-ci') {
                    git credentialsId: 'epm-gitlab-key', branch: "master", url: 'git@git.epam.com:epmc-tst/reportportal-ci.git'
                }

            }
        }, 'Checkout Service': {
            stage('Checkout Service') {
                dir('app') {
                    checkout scm
                }
            }
        }

        def test = load "${ciDir}/jenkins/scripts/test.groovy"
        def utils = load "${ciDir}/jenkins/scripts/util.groovy"
        def helm = load "${ciDir}/jenkins/scripts/helm.groovy"

        utils.scheduleRepoPoll()
        helm.init()


        dir('app') {
            try {
                container('gradle') {
                    stage('Build App') {
                        sh "gradle build --full-stacktrace -P buildNumber=$srvVersion"
                    }
                    stage('Test') {
                        sh "gradle test --full-stacktrace"
                    }
                    stage('Security/SAST') {
                        sh "gradle dependencyCheckAnalyze"
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
        }
        stage('Deploy to Dev Environment') {
            container('helm') {
                dir('kubernetes/reportportal/v5') {
                    sh 'helm dependency update'
                }
                sh "helm upgrade --reuse-values --set uat.repository=$srvRepo --set uat.tag=$srvVersion --wait -f ./reportportal-ci/rp/values-ci.yml reportportal ./kubernetes/reportportal/v5"
            }
        }
        stage('Execute DVT Tests') {
            def srvUrl
            container('kubectl') {
                def srvName = utils.getServiceName(k8sNs, "uat")
                srvUrl = utils.getServiceEndpoint(k8sNs, srvName)
            }
            if (srvUrl == null) {
                error("Unable to retrieve service URL")
            }
            container('httpie') {
                def snapshotVersion = utils.readProperty("app/gradle.properties", "version")
                test.checkVersion("http://$srvUrl", "$snapshotVersion-$srvVersion")
            }
        }

        stage('Deploy to QA Environment') {

        }

        stage('Execute functional tests') {

        }

    }

}
