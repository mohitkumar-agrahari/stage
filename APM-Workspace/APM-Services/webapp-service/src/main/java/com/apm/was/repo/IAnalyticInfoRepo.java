package com.apm.was.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.apm.was.entity.AnalyticInfoEntity;

@Repository
public interface IAnalyticInfoRepo extends JpaRepository<AnalyticInfoEntity,Long>{
	List<AnalyticInfoEntity> findAll();

	AnalyticInfoEntity getAnalyticInfoEntityByAnalyticName(String analyticName);

	

	@Query("SELECT ae FROM AnalyticInfoEntity ae WHERE ae.analyticName=?1")
	AnalyticInfoEntity fetchByAnalyticName(String analyticName);
}
