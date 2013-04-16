package org.jenkinsci.plugins.knappsack;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.knappsack.models.Application;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class KnappsackBuildStepDescriptor extends BuildStepDescriptor<Publisher> {
    public KnappsackBuildStepDescriptor() {
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

    public ListBoxModel doFillApplicationItems(@QueryParameter String userName, @QueryParameter Secret userPassword, @QueryParameter String knappsackURL) {
        ListBoxModel m = new ListBoxModel();

        if (!userName.isEmpty() && !userPassword.getEncryptedValue().isEmpty() && !knappsackURL.isEmpty()) {
            KnappsackAPI knappsackAPI = new KnappsackAPI(knappsackURL, userName, userPassword);
            Application[] applications = knappsackAPI.getApplications();
            for (Application application : applications) {
                m.add(application.getName(), application.getId().toString());
            }
        }
        return m;
    }
}

