package org.jenkinsci.plugins.knappsack;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class KnappsackRecorder extends Recorder {

    private String userName;
    private Secret userPassword;
    private String artifactPath;

    @DataBoundConstructor
    public KnappsackRecorder(String userName, Secret userPassword, String artifactPath) {
        this.userName = userName;
        this.userPassword = userPassword;
        this.artifactPath = artifactPath;
    }

    @Override
    public BuildStepDescriptorImpl getDescriptor() {
        return (BuildStepDescriptorImpl)super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (build.getResult().isWorseOrEqualTo(Result.FAILURE))
            return false;

        EnvVars vars = build.getEnvironment(listener);
        String workspace = vars.expand("$WORKSPACE");
        Collection files = findIpaOrApkFiles(artifactPath, workspace);
        return super.perform(build, launcher, listener);
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class BuildStepDescriptorImpl extends BuildStepDescriptor<Publisher>
    {
        public BuildStepDescriptorImpl() {
            super(KnappsackRecorder.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            // XXX is this now the right style?
            req.bindJSON(this,json);
            save();
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Push to Knappsack";
        }
    }

    private Collection<File> findIpaOrApkFiles(String filePath, String remoteWorkspace) {
        Collection<File> files;
        if (filePath != null && !filePath.trim().isEmpty()) {
            String[] extensions = {"ipa", "apk", "jar", "war"};
            boolean recursive = true;
            files = FileUtils.listFiles(new File(filePath), extensions, recursive);
//            files = Collections.singleton(new File(filePath));
        } else {
            String[] extensions = {"ipa", "apk", "jar", "war"};
            boolean recursive = true;
            files = FileUtils.listFiles(new File(remoteWorkspace), extensions, recursive);
        }
        return files;
    }

    public String getUserName() {
        return userName;
    }

    public Secret getUserPassword() {
        return userPassword;
    }
}
