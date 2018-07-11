package com.apm.datarw.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.apm.datarw.entity.AnalyticAssetMappingEntity;

@Repository
public interface IAnalyticAssetMappingRepo extends JpaRepository<AnalyticAssetMappingEntity, Long>{

	@Query("SELECT aame FROM AnalyticAssetMappingEntity aame WHERE aame.analyticInfoEntity.analyticId=?1 AND aame.assetId=?2")
	AnalyticAssetMappingEntity getAnalyticAssetMapByAnalyticIdAndAssetId(Long analyticId, String assetId);

}
