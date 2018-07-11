package com.apm.fas.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name="APM_FEATURE_INFO")
public class APMFeaturesEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="feature_id_seq")
    @SequenceGenerator(
                  name="feature_id_seq",
                  sequenceName="feature_id_seq",
                  allocationSize=1
                  )
	@Column(name="feature_id")
	private long featureId;
	
	@Column(name="nav_id")
	private String navId;

	@Column(name="feature_name")
	private String featureName;
	
	@Column(name="feature_details")
	private String featureDetails;
	
	@Column(name="nav_sequence")
	private short navSequence;

	@OneToOne(mappedBy="sstFeaturesEntity", cascade=CascadeType.ALL)
	private CustomerFeatureMappingEntity customerFeatureMappingEntity;

	public long getFeatureId() {
		return featureId;
	}

	public void setFeatureId(long featureId) {
		this.featureId = featureId;
	}

	public String getFeatureName() {
		return featureName;
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

	public String getFeatureDetails() {
		return featureDetails;
	}

	public void setFeatureDetails(String featureDetails) {
		this.featureDetails = featureDetails;
	}
	
	public CustomerFeatureMappingEntity getCustomerFeatureMappingEntity() {
		return customerFeatureMappingEntity;
	}
	public void setCustomerFeatureMappingEntity(CustomerFeatureMappingEntity customerFeatureMappingEntity) {
		this.customerFeatureMappingEntity = customerFeatureMappingEntity;
	}

	public String getNavId() {
		return navId;
	}
	public void setNavId(String navId) {
		this.navId = navId;
	}

	public short getNavSequence() {
		return navSequence;
	}

	public void setNavSequence(short navSequence) {
		this.navSequence = navSequence;
	}
}
