package com.apm.was.repo;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.apm.was.entity.AnalyticOutputEntity;

public interface IAnalyticOutputEntity extends JpaRepository<AnalyticOutputEntity,Long>{

	List<AnalyticOutputEntity> getAnalyticOutputEntityByAnalyticInfoEntityAnalyticIdAndAssetId(long analyticId,
			String assetId, PageRequest pageRequest);

	@Query("SELECT distinct(a.tagName) FROM analyticOutputEntity a")
	List<String> fetchTags();

	@Query("SELECT a FROM analyticOutputEntity a WHERE a.tagName=?1 ")
	List<AnalyticOutputEntity> fetchByName(String tagName);

	



	

	

}
