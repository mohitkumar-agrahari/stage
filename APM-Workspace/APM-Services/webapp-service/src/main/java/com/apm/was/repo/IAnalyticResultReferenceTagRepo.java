package com.apm.was.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.apm.was.entity.AnalyticResultReferenceTagEntity;

@Repository
public interface IAnalyticResultReferenceTagRepo extends JpaRepository<AnalyticResultReferenceTagEntity,Long>{

	@Query("SELECT aref FROM  AnalyticResultReferenceTagEntity aref WHERE aref.analyticTagMappingEntity.analyticTagMappingId=?1")
	AnalyticResultReferenceTagEntity fetchById(long analyticTagMappingId);

	AnalyticResultReferenceTagEntity getAnalyticResultReferenceTagEntityByAnalyticTagMappingEntityAnalyticTagMappingId(
			long analyticTagMappingId);

}
