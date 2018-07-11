package com.ge.sst.entity;

import javax.persistence.CascadeType;
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
import com.ge.sst.helper.entities.AuditEntity;
/**
 * Analytic Scangroup Entity
 * Responsible for storing the scangroup data related to alarms.
 * 
 * @author Ankita Srivastava
 *
 */
@Entity(name="analyticScangroupEntity")
@Table(name="ANALYTIC_SCANGROUP")
public class AnalyticAlarmScangroupEntity extends AuditEntity{

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="analytic_scangroup_id_seq")
	@SequenceGenerator(
			name="analytic_scangroup_id_seq",
			sequenceName="analytic_scangroup_id_seq",
			allocationSize=1
			)
	@Column(name="SCANGROUP_ID")
	private long scangroupId;

	@Column(name="TAG_NAME")
	private String tagName;

	@Column(name="TAG_VALUE")
	private String tagValue;

	@ManyToOne(cascade=CascadeType.ALL)
	@JoinColumn(name = "ALARM_ID", referencedColumnName = "ALARM_ID")
	@JsonIgnore
	private AnalyticAlarmEntity analyticAlarmEntity;

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

	public AnalyticAlarmEntity getAnalyticAlarmEntity() {
		return analyticAlarmEntity;
	}

	public void setAnalyticAlarmEntity(AnalyticAlarmEntity analyticAlarmEntity) {
		this.analyticAlarmEntity = analyticAlarmEntity;
	}

	public long getScangroupId() {
		return scangroupId;
	}

	public void setScangroupId(long scangroupId) {
		this.scangroupId = scangroupId;
	}

	@Override
	public String toString() {
		return "AnalyticAlarmScangroupEntity [scangroupId=" + scangroupId + ", tagName=" + tagName + ", tagValue="
				+ tagValue + ", analyticAlarmEntity=" + analyticAlarmEntity + "]";
	}



}
