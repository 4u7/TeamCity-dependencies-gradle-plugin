package com.github.jk1.tcdeps.model

import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

/**
 * Stands for changing module version limited to a particular TeamCity feature branch
 */
class BranchScopedArtifactVersion extends ArtifactVersion {

    final def branch

    BranchScopedArtifactVersion(String version, String branch) {
        super(version)
        if (branch == null || branch.isEmpty()) {
            throw new InvalidUserDataException("branch should not be empty")
        }
        this.branch = branch
    }

    @Override
    def resolve(Project project, String btid) {
        String request = url(project.teamcityServer.url, btid)
        String response = "No response recorded. Rerun with --stacktrace to see an exception."
        try {
            HttpURLConnection connection = request.toURL().openConnection()
            response = connection.inputStream.withReader { Reader reader -> reader.text }
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                project.logger.info("$version has been resolved as $response in $branch")
                version = response
                changing = false
            } else {
                String message = "Unable to resolve $version in $branch.\nRequest: GET $request \nServer response: \n $response"
                throw new GradleException(message)
            }
        } catch (Exception e) {
            throw new GradleException("Unable to resolve $version in $branch. Request: GET $request", e)
        }
    }

    def url(String server, String btid) {
        def branchEncoded = URLEncoder.encode(branch, "utf-8");
        def query = "buildType:$btid,branch:$branchEncoded${versionPlaceholders[version]}/number"
        return "${server}/guestAuth/app/rest/builds/$query"
    }
}
