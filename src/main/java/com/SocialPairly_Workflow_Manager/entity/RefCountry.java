package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ref_countries")
public class RefCountry {

    @Id
    @Column(name = "code", length = 2, nullable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "postal_regex", length = 120)
    private String postalRegex;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPostalRegex() { return postalRegex; }
    public void setPostalRegex(String postalRegex) { this.postalRegex = postalRegex; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
