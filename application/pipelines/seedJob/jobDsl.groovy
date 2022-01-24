// Ref source code / examples: https://github.com/jenkinsci/job-dsl-plugin/wiki/Tutorial---Using-the-Jenkins-Job-DSL
// Ref groovy code syntax: https://riptutorial.com/groovy/example/18003/iterate-over-a-collection
// Ref jobDSL doc: http://your_jenkins_ip:8080/plugin/job-dsl/api-viewer/index.html
// Online playground: https://groovyide.com/playground

String deployFolder = "deploy"
String provisionFolder = "provision"
String buildFolder = "build"
String githubCredID = "github-cred"
String githubDefaultBranch = "sprint-2"

def rootFolders = [
    deployFolder,
    provisionFolder,
    buildFolder
]
def environments = [
    "dev",
    "staging",
]
def provisionJobs = [
    "kafka",
    "elasticsearch",
    "redis"
]
def deploymentJobs = [
    "hcx-api": ["autoTriggerPath":"build/hcx-api"],
    "payer-api": [],
    "api-gateway": [],
    "provider-api": [],
    "claimsjob": [],
    "coverageeligibility": [],
    "statusjob": [],
    "searchjob": [],
    "searchresponsejob": [],
    "paymentsjob": [],
    "preauthjob": [],
    "keycloak": ["artifactVersion":"No"],
    "kafka-topics": [],
    "hcx-registry": [],
    "audit-indexer": [],
    "secor": ["autoTriggerPath":"build/secor"],
]
def buildJobs = [
    "hcx-api": [
        "repo": "https://github.com/Swasth-Digital-Health-Foundation/hcx-platform",
        "scriptPath": "hcx-apis/Jenkinsfile"
    ],
    "api-gateway": [
        "repo": "https://github.com/Swasth-Digital-Health-Foundation/hcx-platform",
        "scriptPath": "api-gateway/Jenkinsfile"
    ],
    "pipeline-jobs": [
        "repo": "https://github.com/Swasth-Digital-Health-Foundation/hcx-platform",
        "scriptPath": "hcx-pipeline-jobs/Jenkinsfile"
    ],
    "hcx-registry": [
        "repo": "https://github.com/shiva-rakshith/sunbird-rc-core-0.0.1",
        "scriptPath": "java/registry/Jenkinsfile"
    ],
    "secor": [
        "repo": "https://github.com/Swasth-Digital-Health-Foundation/hcx-devops",
        "scriptPath": "application/pipelines/Build/Secor/Jenkinsfile"
    ]
]

buildJobTemplate = {
        jobName,repo,jenkinsFilePath ->
        pipelineJob("$buildFolder/$jobName") {
          definition {
            cpsScm {
              scm {
                git {
                  remote {
                    url("$repo")
                  }
                  branch("*/sprint-2")
                }
              }
              lightweight()
              scriptPath("$jenkinsFilePath")
            }
          }
        }
}

provisionJobTemplate = {
        env, provisionJobName ->
        pipelineJob("$provisionFolder/$env/$provisionJobName") {
          definition {
            cpsScm {
              scm {
                git {
                  remote {
                    url('https://github.com/Swasth-Digital-Health-Foundation/hcx-devops')
                    credentials("github-cred")
                  }
                  branch("*/${githubDefaultBranch}")
                }
              }
              lightweight()
              scriptPath("application/pipelines/provision/${provisionJobName}/Jenkinsfile")
            }
          }
        }

}
deployJobTemplate = {
        env, deployJobName ->
        pipelineJob("$deployFolder/$env/$deployJobName.key") {
          println("deploy job: "+deployJobName)
          if (deployJobName.value['autoTriggerPath']) {
              triggers {
                upstream(deployJobName.value['autoTriggerPath'], 'SUCCESS')
              }
          }
          parameters {
            string {
                name("artifact_version")
                defaultValue(deployJobName.value['artifactVersion'] ?: "")
                description("Artifact version to deploy")
                // Strip whitespace from the beginning and end of the string.
                trim(true)
            } 
          }
          definition {
            cpsScm {
              scm {
                git {
                  remote {
                    url('https://github.com/Swasth-Digital-Health-Foundation/hcx-devops')
                    credentials("github-cred")
                  }
                  branch("*/${githubDefaultBranch}")
                }
              }
              lightweight()
              scriptPath("application/pipelines/deploy/${deployJobName.key}/Jenkinsfile")
            }
          }
        }
}

// Crating root folders
rootFolders.each {
    rootFolder ->
    // If no variable defined, default will be `it`
    // But that variable has some issues with the folder() method
    // If used $it, some random string created as folder name
    println "$rootFolder"
    folder("$rootFolder") {
        displayName("$rootFolder")
        description("$rootFolder")
    }
}

// Creating Deployment Folders
environments.each {
    env ->
    folder("$deployFolder/$env") {
        description("Folder for $env")
    }
    folder("$provisionFolder/$env") {
        description("Folder for $env")
    }
}

// Creating build jobs
buildJobs.each {
    jobName ->
    println(jobName)
    buildJobTemplate(jobName.key, buildJobs[jobName.key].repo, buildJobs[jobName.key].scriptPath)
}

// Creating provision jobs
provisionJobs.each {
    provisionJobName ->
    environments.each {
        env ->
        provisionJobTemplate(env, provisionJobName)
    }
}

// Creating deployment jobs
deploymentJobs.each {
    deployJobName ->
    environments.each {
        env ->
        deployJobTemplate(env,deployJobName)
    }
}
