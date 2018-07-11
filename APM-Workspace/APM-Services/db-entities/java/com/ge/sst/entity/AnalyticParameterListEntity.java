package com.ge.sst.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Ankita Srivastava
 *
 */
@Entity(name="analyticParameterList")
@Table(name="ANALYTIC_PARAMETER_LIST")
public class AnalyticParameterListEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1062966380738827269L;

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="analytic_params_info_id_seq")
	@SequenceGenerator(
			name="analytic_params_info_id_seq",
			sequenceName="analytic_params_info_id_seq",
			allocationSize=1
			)
	@Column(name="TAG_ID")
	private long tagId;

	@Column(name="TAG_NAME", nullable=false, columnDefinition="text")
	private String tagName;

	@Column(name="TAG_VALUE")
	private String tagValue;

	@Column(name="TAG_DESCRIPTION")
	private String tagDescription;

	@ManyToOne
	@JoinColumn(name = "ANALYTIC_ID", referencedColumnName = "ANALYTIC_ID")
	@JsonIgnore
	private AnalyticInfoEntity analyticInfoEntity;

	public long getTagId() {
		return tagId;
	}

	public void setTagId(long tagId) {
		this.tagId = tagId;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getTagValue() {
		return tagValue;
	}

	public void setTagValue(String tagValue) {
		this.tagValue = tagValue;
	}

	public String getTagDescription() {
		return tagDescription;
	}

	public void setTagDescription(String tagDescription) {
		this.tagDescription = tagDescription;
	}

	public AnalyticInfoEntity getAnalyticInfoEntity() {
		return analyticInfoEntity;
	}

	public void setAnalyticInfoEntity(AnalyticInfoEntity analyticInfoEntity) {
		this.analyticInfoEntity = analyticInfoEntity;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}



}
