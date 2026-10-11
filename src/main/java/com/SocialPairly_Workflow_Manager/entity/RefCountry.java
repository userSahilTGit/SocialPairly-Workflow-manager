package com.SocialPairly_Workflow_Manager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ref_countries")
public class RefCountry {

    @Id
    @Column(name = "code", columnDefinition = "char(2)", nullable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "postal_regex", length = 120)
    private String postalRegex;

    /** International dialing prefix, e.g. +91 (BUG-1570). */
    @Column(name = "phone_code", length = 8)
    private String phoneCode;

    /** Strict national mobile digit length (excluding dial code), e.g. 10 for India. */
    @Column(name = "phone_national_length")
    private Integer phoneNationalLength;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPostalRegex() { return postalRegex; }
    public void setPostalRegex(String postalRegex) { this.postalRegex = postalRegex; }

    public String getPhoneCode() { return phoneCode; }
    public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }

    public Integer getPhoneNationalLength() { return phoneNationalLength; }
    public void setPhoneNationalLength(Integer phoneNationalLength) { this.phoneNationalLength = phoneNationalLength; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
