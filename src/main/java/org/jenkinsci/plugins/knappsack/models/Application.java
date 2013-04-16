package org.jenkinsci.plugins.knappsack.models;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Application {

    private Long id;
    private String name;
    private String description;
    private Long groupId;
    private Long activeOrganizationId;
    private String activeOrganizationName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getActiveOrganizationId() {
        return activeOrganizationId;
    }

    public void setActiveOrganizationId(Long activeOrganizationId) {
        this.activeOrganizationId = activeOrganizationId;
    }

    public String getActiveOrganizationName() {
        return activeOrganizationName;
    }

    public void setActiveOrganizationName(String activeOrganizationName) {
        this.activeOrganizationName = activeOrganizationName;
    }
}