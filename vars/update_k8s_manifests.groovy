#!/usr/bin/env groovy
/**
 * Update Kubernetes manifests with new image tags for QBShop
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@ci.local'
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    withCredentials([
        usernamePassword(
            credentialsId: gitCredentials,
            usernameVariable: 'GIT_USERNAME',
            passwordVariable: 'GIT_PASSWORD'
        )
    ]) {
        // Configure Git
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """
        sh """
            # Update main QBShop application deployment
            sed -i "s|image: satyamsri/qbshop-app:.*|image: satyamsri/qbshop-app:${imageTag}|g" ${manifestsPath}/08-qbshop-deployment.yaml
            # Update migration job if present
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: satyamsri/qbshop-migration:.*|image: satyamsri/qbshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            # Check if changes occurred
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update QBShop image tags to ${imageTag} [ci skip]"
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Satyams-git/Qualibytes-Ecommerce.git
                git push origin HEAD:\${GIT_BRANCH}
            fi
        """
    }
}
