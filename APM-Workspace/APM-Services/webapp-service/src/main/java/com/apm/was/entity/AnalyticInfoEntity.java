package com.apm.was.entity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.apm.was.asme.entity.ASMEReferenceTagsEntity;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "ANALYTIC_INFO")
public class AnalyticInfoEntity {

	private static final String INPUT_DATE_TIME_FMT = "MM/dd/yyyy HH:mm:ss";

	public AnalyticInfoEntity() {
		// Format the added on date format.
		SimpleDateFormat dateFormatter = new SimpleDateFormat(INPUT_DATE_TIME_FMT);
		try {
			this.addedOn = dateFormatter.parse(dateFormatter.format(new Date()));
		} catch (ParseException e) {
			this.addedOn = new Date();
		}
		dateFormatter = null;
	}

	
	/**
	 * adding title,subtitle for analytic inbox view, please ignore
	 * 
	 */
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analytic_info_id_seq")
	@SequenceGenerator(name = "analytic_info_id_seq", sequenceName = "analytic_info_id_seq", allocationSize = 1)
	@Column(name = "ANALYTIC_ID")
	@JsonProperty("analytic_id")
	private long analyticId;
	
	@Transient
	private long id;
	
	public long getId() {
		return analyticId;
	}

	


	@Transient
	private String title;

	/*public void setTitle(String title) {
		this.title = title;
	}*/

	public String getTitle() {
		return analyticName;
	}

	@Transient
	private String subtitle;

	/*public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}*/

	public String getSubtitle() {
		return analyticVersion;
	}

	/*public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}*/

	

	@Column(name = "ANALYTIC_NAME")
	@JsonProperty("analytic_name")
	private String analyticName;

	@Column(name = "ANALYTIC_DESCRIPTION")
	private String analyticDescription;

	@Column(name = "ANALYTIC_SCOPE_LEVEL")
	private String analyticScopeLevel;

	@Column(name = "START_TIME")
	private Date startTime;

	@Column(name = "END_TIME")
	private Date endTime;

	@Column(name = "START_OFFSET")
	private long startOffset;

	@Column(name = "END_OFFSET")
	private long endOffset;

	@Column(name = "EXECUTION_FREQUENCY")
	@JsonProperty("execution_frequency")
	private String executionFrequency;

	@Column(name = "ANALYTIC_PLATFORM")
	@JsonProperty("analytic_platform")
	private String analyticPlatform;

	@Column(name = "IS_STATE_MAINTAINED")
	private short isStateMaintained;

	@Column(name = "DATA_INTERVAL")
	private long DataInterval;

	@Column(name = "ANALYTIC_VERSION")
	@JsonProperty("analytic_version")
	private String analyticVersion;
	

	
	
	@OneToMany(mappedBy = "analyticInfoEntity", cascade = CascadeType.ALL)
	private List<AnalyticTagMappingEntity> analyticTagMappingEntity;


	// added new mapping for AnalyticAssetMappingEntity
	@OneToMany(mappedBy = "analyticInfoEntity", cascade = CascadeType.ALL)
	private List<AnalyticAssetMappingEntity> analyticAssetMappingEntities;

	
	@OneToMany(mappedBy = "analyticInfoEntity", cascade = CascadeType.ALL)
	private List<AnalyticSurplusConstantsEntity> analyticSurplusConstants;

	@OneToMany(mappedBy = "analyticInfoEntity", cascade = CascadeType.ALL)
	private List<AnalyticParameterListEntity> analyticParameterList;
	
	@OneToMany(mappedBy = "analyticInfoEntity", cascade = CascadeType.ALL)
	private List<ASMEReferenceTagsEntity> asmeReferenceTagsEntities;

	@Column(name = "ADDED_ON")
	@JsonProperty("added_on")
	private Date addedOn;


	public List<AnalyticAssetMappingEntity> getAnalyticAssetMappingEntities() {
		return analyticAssetMappingEntities;
	}
	public void setAnalyticAssetMappingEntities(List<AnalyticAssetMappingEntity> analyticAssetMappingEntities) {
		this.analyticAssetMappingEntities = analyticAssetMappingEntities;
	}
	public long getAnalyticId() {
		return analyticId;
	}

	public void setAnalyticId(long analyticId) {
		this.analyticId = analyticId;
	}

	public String getAnalyticName() {
		return analyticName;
	}

	public void setAnalyticName(String analyticName) {
		this.analyticName = analyticName;
	}

	public String getAnalyticDescription() {
		return analyticDescription;
	}

	public void setAnalyticDescription(String analyticDescription) {
		this.analyticDescription = analyticDescription;
	}

	public String getAnalyticScopeLevel() {
		return analyticScopeLevel;
	}

	public void setAnalyticScopeLevel(String analyticScopeLevel) {
		this.analyticScopeLevel = analyticScopeLevel;
	}

	public String getExecutionFrequency() {
		return executionFrequency;
	}

	public void setExecutionFrequency(String executionFrequency) {
		this.executionFrequency = executionFrequency;
	}

	public String getAnalyticPlatform() {
		return analyticPlatform;
	}

	public void setAnalyticPlatform(String analyticPlatform) {
		this.analyticPlatform = analyticPlatform;
	}

	public short getIsStateMaintained() {
		return isStateMaintained;
	}

	public void setIsStateMaintained(short isStateMaintained) {
		this.isStateMaintained = isStateMaintained;
	}

	public List<AnalyticTagMappingEntity> getAnalyticTagMappingEntity() {
		return analyticTagMappingEntity;
	}

	public void setAnalyticTagMappingEntity(List<AnalyticTagMappingEntity> analyticTagMappingEntity) {
		this.analyticTagMappingEntity = analyticTagMappingEntity;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public long getDataInterval() {
		return DataInterval;
	}

	public void setDataInterval(long dataInterval) {
		DataInterval = dataInterval;
	}

	public String getAnalyticVersion() {
		return analyticVersion;
	}

	public void setAnalyticVersion(String analyticVersion) {
		this.analyticVersion = analyticVersion;
	}

	public List<AnalyticSurplusConstantsEntity> getAnalyticSurplusConstants() {
		return analyticSurplusConstants;
	}

	public void setAnalyticSurplusConstants(List<AnalyticSurplusConstantsEntity> analyticSurplusConstants) {
		this.analyticSurplusConstants = analyticSurplusConstants;
	}

	public List<AnalyticParameterListEntity> getAnalyticParameterList() {
		return analyticParameterList;
	}

	public void setAnalyticParameterList(List<AnalyticParameterListEntity> analyticParameterList) {
		this.analyticParameterList = analyticParameterList;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public long getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(long startOffset) {
		this.startOffset = startOffset;
	}

	public long getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(long endOffset) {
		this.endOffset = endOffset;
	}

	public Date getAddedOn() {
		return addedOn;
	}

	public void setAddedOn(Date addedOn) {
		this.addedOn = addedOn;
	}
	
	public List<ASMEReferenceTagsEntity> getAsmeReferenceTagsEntities() {
		return asmeReferenceTagsEntities;
	}
	public void setAsmeReferenceTagsEntities(List<ASMEReferenceTagsEntity> asmeReferenceTagsEntities) {
		this.asmeReferenceTagsEntities = asmeReferenceTagsEntities;
	}

	@Override
	public String toString() {

		return "AnalyticInfoEntity [analyticId=" + analyticId + ", id=" + id + ", title=" + title + ", subtitle="
				+ subtitle + ", analyticName=" + analyticName + ", analyticDescription=" + analyticDescription
				+ ", analyticScopeLevel=" + analyticScopeLevel + ", startTime=" + startTime + ", endTime=" + endTime
				+ ", startOffset=" + startOffset + ", endOffset=" + endOffset + ", executionFrequency="
				+ executionFrequency + ", analyticPlatform=" + analyticPlatform + ", isStateMaintained="
				+ isStateMaintained + ", DataInterval=" + DataInterval + ", analyticVersion=" + analyticVersion
				+ ", assetType=" +  ", imagePath=" +  ", analyticTagMappingEntity="
				+ analyticTagMappingEntity + ", analyticAssetMappingEntities=" + analyticAssetMappingEntities
				+ ", analyticSurplusConstants=" + analyticSurplusConstants + ", analyticParameterList="
				+ analyticParameterList + ", asmeReferenceTagsEntities=" + asmeReferenceTagsEntities + ", addedOn=" + addedOn + "]";
	}


}
