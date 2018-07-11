package com.apm.fas.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.apm.fas.dto.NavigationDTO;
import com.apm.fas.entity.CustomerFeatureMappingEntity;
import com.apm.fas.inf.IFeatureAvailabilityService;
import com.apm.fas.repo.ICustomerFeatureMappingRepo;

@Service
public class FeatureAvailabilityServiceImpl implements IFeatureAvailabilityService {
	
	private static final Logger log = LoggerFactory.getLogger(FeatureAvailabilityServiceImpl.class);
	
	@Autowired
	ICustomerFeatureMappingRepo custFeatureRepo;
	
	public List<NavigationDTO> getEnabledFeatures(long userGroupId) {
		
		List<NavigationDTO> navigationDTOs = new ArrayList<>();
		try {
		List<CustomerFeatureMappingEntity> customerFeatureMappingEntities = custFeatureRepo.getEnabledFeatures(userGroupId);
		if(!(Objects.isNull(customerFeatureMappingEntities))) {
		customerFeatureMappingEntities.stream().forEach(customerFeatureMappingEntity -> {
			NavigationDTO navigationDTO = new NavigationDTO();
			navigationDTO.setLabel(customerFeatureMappingEntity.getSstFeaturesEntity().getFeatureName());
			navigationDTO.setId(customerFeatureMappingEntity.getSstFeaturesEntity().getNavId());
			navigationDTO.setIcon(customerFeatureMappingEntity.getSstFeaturesEntity().getFeatureDetails());
			log.info("enabled Features are :: " + customerFeatureMappingEntity.getSstFeaturesEntity().getFeatureName());
			navigationDTOs.add(navigationDTO);
		});
		}
		else {
			throw new Exception();
		}
		}catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return navigationDTOs;
	}

}
