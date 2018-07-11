package com.apm.datarw.repo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.apm.datarw.entity.AnalyticAlarmEntity;

public interface IAnalyticAlarmRepo extends JpaRepository<AnalyticAlarmEntity,Long>{
	
	@Query("SELECT COUNT(alarmE.alarmId) FROM analyticAlarmEntity alarmE WHERE alarmE.analyticAssetMappingEntity.assetId=?1")
	Long fetchCountByAssetName(String assetName);

	List<AnalyticAlarmEntity> getAnalyticAlarmEntityByAnalyticAssetMappingEntityAssetName(String assetId);

	@Query("SELECT alarmE FROM analyticAlarmEntity alarmE order by alarmE.alarmId ASC")
	Page<AnalyticAlarmEntity> getAllByOrder(Pageable  pageRequest);

	

}
