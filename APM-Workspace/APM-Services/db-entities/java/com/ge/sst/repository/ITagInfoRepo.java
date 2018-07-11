package com.ge.sst.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.ge.sst.entity.TagInfoEntity;
@Repository
public interface ITagInfoRepo extends JpaRepository<TagInfoEntity,Long>{
	
	@Query("SELECT distinct(tagI.tagType) FROM TagInfoEntity tagI ")
	List<String> fetchTagType();

	@Query("SELECT distinct(tagI.tagAliases) FROM TagInfoEntity tagI WHERE tagI.analyticTagMappingEntity.analyticInfoEntity.analyticId=?1 AND tagI.tagType=?2 ")
	List<String> findAliasesByType(Long analyticId, String string);
	
	@Query("SELECT distinct(tagI.tagName) FROM TagInfoEntity tagI WHERE tagI.analyticTagMappingEntity.analyticInfoEntity.analyticId=?1 AND tagI.tagType=?2 ")
	List<String> findtagNameByType(Long analyticId, String string);
	
	@Query("SELECT tagI FROM TagInfoEntity tagI WHERE tagI.analyticTagMappingEntity.analyticInfoEntity.analyticId=?1")
	List<TagInfoEntity> getByAnalyticId(long analyticId);

	List<TagInfoEntity> getTagInfoEntityByAnalyticTagMappingEntityAnalyticInfoEntityAnalyticId(long analyticId);

	@Query("SELECT tagI FROM TagInfoEntity tagI WHERE tagI.analyticTagMappingEntity.analyticInfoEntity.analyticId=?1 GROUP BY tagI.tagName,tagI.tagId  order by tagI.tagId")
	List<TagInfoEntity> findById(long analyticId);
	
	@Query("SELECT tagI FROM TagInfoEntity tagI WHERE tagI.tagId=?1")
	List<TagInfoEntity> findTagById(long tagId);
}
