package org.jenkinsci.plugins.knappsack;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Calendar;

public class KnappsackRecorder extends Recorder {

    private String userName;
    private Secret userPassword;
    private String knappsackURL;
    private String artifactDirectory;
    private String artifactFile;
    private String applicationID;

    @DataBoundConstructor
    public KnappsackRecorder(String userName, Secret userPassword, String knappsackURL, String artifactDirectory, String artifactFile, String applicationID) {
        this.userName = userName;
        this.userPassword = userPassword;
        if (knappsackURL != null && !knappsackURL.isEmpty()) {
            this.knappsackURL = knappsackURL;
        } else {
            this.knappsackURL = "https://app.knappsack.com";
        }
        this.artifactDirectory = artifactDirectory;
        this.artifactFile = artifactFile;
        this.applicationID = applicationID;
    }

    @Override
    public BuildStepDescriptorImpl getDescriptor() {
        return (BuildStepDescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (build.getResult().isWorseOrEqualTo(Result.FAILURE)) {
            return false;
        }

        EnvVars vars = build.getEnvironment(listener);
        String workspace = vars.expand("$WORKSPACE");
        File file = findInstallationFile(artifactDirectory, artifactFile, workspace);
        uploadFile(file, build);

        return super.perform(build, launcher, listener);
    }

    @Extension
    public static final class BuildStepDescriptorImpl extends BuildStepDescriptor<Publisher> {
        public BuildStepDescriptorImpl() {
            super(KnappsackRecorder.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();
            return true;
        }

        public String getDisplayName() {
            return "Push to Knappsack";
        }

//        TODO Dynamically populate applications
//        public ListBoxModel doFillApplicationItems() {
//            ListBoxModel m = new ListBoxModel();
//            return m;
//        }
    }

    private File findInstallationFile(String artifactDirectory, String artifactFile, String workspace) {

        if (!artifactDirectory.endsWith(System.getProperty("file.separator"))) {
            artifactDirectory = artifactDirectory + System.getProperty("file.separator");
        }

        File file = null;
        if (!artifactDirectory.trim().isEmpty() && !artifactFile.trim().isEmpty()) {
            File directory = new File(artifactDirectory);
            file = getFile(directory, artifactFile);
        }

        //if the file is still null, look in the workspace
        if (file == null) {
            File directory = new File(workspace);
            file = getFile(directory, artifactFile);
        }

        return file;
    }

    private File getFile(File directory, String artifactFile) {
        File file = null;
        FileFilter fileFilter = new WildcardFileFilter(artifactFile);
        File[] filesArray = directory.listFiles(fileFilter);

        if (filesArray.length > 0) {
            file = filesArray[0];
        }

        return file;
    }

    private void uploadFile(File file, AbstractBuild<?, ?> build) {
        TokenResponse tokenResponse = getTokenResponse();
        String url = knappsackURL + "/api/v1/applicationVersions";

        FormDataMultiPart part = new FormDataMultiPart();
        part.field("applicationId", applicationID);

        int buildNumber = build.getNumber();
        int month = build.getTimestamp().get(Calendar.MONTH);
        int day = build.getTimestamp().get(Calendar.DAY_OF_MONTH);
        int year = build.getTimestamp().get(Calendar.YEAR);
        String versionName = buildNumber + "." + month + "." + day + "." + year;

        part.field("versionName", versionName);

        StringBuilder changeListSB = new StringBuilder();
        if (!build.getChangeSet().isEmptySet()) {
            boolean hasManyChangeSets = build.getChangeSet().getItems().length > 1;
            for (ChangeLogSet.Entry entry : build.getChangeSet()) {
                changeListSB.append("\n");
                if (hasManyChangeSets) {
                    changeListSB.append("* ");
                }
                changeListSB.append(entry.getAuthor()).append(": ").append(entry.getMsg());
            }
        }

        String changeList = changeListSB.toString();
        if (changeList.isEmpty()) {
            changeList = "No changes listed";
        }

        part.field("recentChanges", changeList);
        part.field("appState", "GROUP_PUBLISH");

        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("installationFile", file, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        part.bodyPart(fileDataBodyPart);

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        WebResource webResource = client.resource(url);

        String token = tokenResponse.getAccess_token();
        ClientResponse response = webResource
                .type(MediaType.MULTIPART_FORM_DATA_TYPE).header("Authorization", "Bearer " + token)
                .post(ClientResponse.class, part);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
        }
    }

    private TokenResponse getTokenResponse() {
        String url = knappsackURL + "/oauth/token";
        url = url + "?client_id=mobile_api_client&client_secret=kzI7QNsbne8KOlS&grant_type=password&username=" + userName + "&password=" + userPassword;
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
        }

        return response.getEntity(TokenResponse.class);
    }

    public String getUserName() {
        return userName;
    }

    public Secret getUserPassword() {
        return userPassword;
    }

    public String getKnappsackURL() {
        return knappsackURL;
    }

    public String getArtifactDirectory() {
        return artifactDirectory;
    }

    public String getArtifactFile() {
        return artifactFile;
    }

    public String getApplicationID() {
        return applicationID;
    }
}
