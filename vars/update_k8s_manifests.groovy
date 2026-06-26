#!/usr/bin/env groovy
/**
 * Update Kubernetes manifests with new image tags for QBShop
 */
def call(Map config = [:]) {
    def imageTag       = config.imageTag ?: error("Image tag is required")
    def manifestsPath  = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName    = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail   = config.gitUserEmail ?: 'jenkins@ci.local'
    def appImage       = config.appImage ?: error("App image name is required")
    def migrationImage = config.migrationImage ?: error("Migration image name is required")
    def repoUrl        = config.repoUrl ?: error("Repo URL is required")

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([
        usernamePassword(
            credentialsId: gitCredentials,
            usernameVariable: 'GIT_USERNAME',
            passwordVariable: 'GIT_PASSWORD'
        )
    ]) {

        sh """
            git config user.name '${gitUserName}'
            git config user.email '${gitUserEmail}'
        """

        sh """
            # Update app deployment image
            sed -i "s|image: .*qbshop-app:.*|image: ${appImage}:${imageTag}|g" ${manifestsPath}/08-qbshop-deployment.yaml

            # Update migration job image
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: .*qbshop-migration:.*|image: ${migrationImage}:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update QBShop image tags to ${imageTag} [ci skip]"
            fi
        """

        // safer push without interpolation warning
        withEnv([
            "REPO_URL=${repoUrl.replace('https://', '')}"
        ]) {
            sh '''
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@${REPO_URL}
                git push origin HEAD:${GIT_BRANCH}
            '''
        }
    }
}
