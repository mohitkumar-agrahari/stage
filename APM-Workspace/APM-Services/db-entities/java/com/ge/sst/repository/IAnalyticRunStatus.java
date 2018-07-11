package com.ge.sst.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ge.sst.entity.AnalyticRunEntity;

public interface IAnalyticRunStatus extends JpaRepository<AnalyticRunEntity, String>{

	@Query("SELECT runEntity FROM AnalyticRunEntity runEntity WHERE runEntity.isRunning=true")
	List<AnalyticRunEntity> fetchRunningJobs();

	/**
	 * If the analytic job <b>is not running</b> but <b>is scheduled</b>.
	 * @return List of AnalyticRunEntity
	 */
	@Query("SELECT runEntity FROM AnalyticRunEntity runEntity WHERE runEntity.isRunning=false and runEntity.isScheduled=true")
	List<AnalyticRunEntity> fetchJobsToStart();
	
	@Query("SELECT runEntity FROM AnalyticRunEntity runEntity WHERE runEntity.isRunning=true and runEntity.analyticInfoEntity.analyticId=?1 and runEntity.assetId=?2")
	AnalyticRunEntity getJobForAnalyticId(Long analyticId, String assetId);
	
	@Query("SELECT runEntity FROM AnalyticRunEntity runEntity WHERE runEntity.jobId=?1")
	AnalyticRunEntity getJobForJobId(String jobId);

	@Query("SELECT runEntity FROM AnalyticRunEntity runEntity WHERE runEntity.analyticInfoEntity.analyticId=?1 and runEntity.jobId=?2")
	AnalyticRunEntity getAnalyticByAnalyticIdAndJobId(Long analyticId, String jobId);
	
	@Query("SELECT max(runEntity.startDate) FROM AnalyticRunEntity runEntity where runEntity.analyticInfoEntity.analyticId=?1 ")
	String fetchLatestJobTimeByAnalyticId(long analyticId);

	AnalyticRunEntity getAnalyticRunEntityBystartDate(String latestDate);

	AnalyticRunEntity getAnalyticRunEntityByEndDate(String latestDate);
	
}
