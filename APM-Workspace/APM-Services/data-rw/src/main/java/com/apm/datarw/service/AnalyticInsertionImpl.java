package com.apm.datarw.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.apm.datarw.config.SSTConfigurations;
import com.apm.datarw.dto.AssetDTO;
import com.apm.datarw.dto.NewAnalyticDTO;
import com.apm.datarw.dto.ServiceResponse;
import com.apm.datarw.dto.TagInfoDTO;
import com.apm.datarw.entity.AnalyticAssetMappingEntity;
import com.apm.datarw.entity.AnalyticInfoEntity;
import com.apm.datarw.entity.AnalyticParameterListEntity;
import com.apm.datarw.entity.AnalyticRunEntity;
import com.apm.datarw.entity.AnalyticSurplusConstantsEntity;
import com.apm.datarw.entity.AnalyticTagMappingEntity;
import com.apm.datarw.entity.TagInfoEntity;
import com.apm.datarw.exceptions.OnPremiseException;
import com.apm.datarw.exceptions.ValidationException;
import com.apm.datarw.http.connector.HttpConnector;
import com.apm.datarw.repo.IAnalyticAssetMappingRepo;
import com.apm.datarw.repo.IAnalyticInfoRepo;
import com.apm.datarw.repo.IAnalyticParameterListRepo;
import com.apm.datarw.repo.IAnalyticRunStatus;
import com.apm.datarw.repo.IAnalyticTagMappingRepo;
import com.apm.datarw.repo.IAnalyticsSurplusConstants;
import com.apm.datarw.utils.DataConstants;
import com.apm.datarw.utils.ExternalPropertyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
@Component
public class AnalyticInsertionImpl {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	SSTConfigurations sstConfig;

	@Autowired
	IAnalyticInfoRepo analyticInfoRepo;

	@Autowired
	IAnalyticTagMappingRepo mappingRepo;

	@Autowired
	IAnalyticsSurplusConstants surplusInsertion;

	@Autowired
	IAnalyticParameterListRepo analyticParameterInsertion;

	@Autowired
	IAnalyticRunStatus runStatus;

	/*@Autowired
	IEquipmentInfoRepo equipmentInfo;

	@Autowired
	IEquipmentAnalyticMappingRepo equipmentMapping;
*/
	@Autowired
	IAnalyticAssetMappingRepo analyticAssetMappingRepo;
	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Autowired
	ModelMapper modelMapper;

	public boolean validateAnalyticDetails(NewAnalyticDTO analyticConfiguration) throws ValidationException{
		String analyticPrefix = String.format("[%s-%s]: ", analyticConfiguration.getAnalyticName(), analyticConfiguration.getAnalyticVersion());
		log.info(analyticPrefix+" Validating Input JSON for Analytic.");
		//Data Interval
		if(analyticConfiguration.getDataInterval()<=0){
			log.error(analyticPrefix+" Dataa Interval <= 0 .");
			throw new ValidationException(analyticPrefix + " Data interval should always be greater than zero.");
		}

		//Analyitc Name
		if(checkNull(analyticConfiguration.getAnalyticName())){
			log.error(analyticPrefix+" Analytic Name is NULL. Validation failed.");
			throw new ValidationException(analyticPrefix + "Analytic Name not present in the analytic configuration.");
		}
		if(checkSize(analyticConfiguration.getAnalyticName())){
			log.error(analyticPrefix+" Analytic Name is empty. Validation failed.");
			throw new ValidationException(analyticPrefix + "Analytic Name is empty in the analytic configuration.");
		}

		//Input Tags
		if(checkNull(analyticConfiguration.getConfig().getInput_tags())){
			log.error(analyticPrefix+" Input Tags is NULL. Validation failed.");
			throw new ValidationException(analyticPrefix + "Input Tags not present in the analytic configuration.");
		}
		if(checkSize(analyticConfiguration.getConfig().getInput_tags())){
			log.error(analyticPrefix+" Input Tags contains no data. Validation failed.");
			throw new ValidationException(analyticPrefix + "'input_tags' key present without any data.");
		}

		//Output Tags
		if(checkNull(analyticConfiguration.getConfig().getOutput_tags())){
			log.error(analyticPrefix+" Output Tags is NULL. Validation failed.");
			throw new ValidationException(analyticPrefix + "Input Tags not present in the analytic configuration.");
		}
		if(checkSize(analyticConfiguration.getConfig().getOutput_tags())){
			log.error(analyticPrefix+" Output Tags contains no data. Validation failed.");
			throw new ValidationException(analyticPrefix + "'output_tags' key present without any data.");
		}

		//Tag Aliases
		if(checkNull(analyticConfiguration.getConfig().getTag_aliases())){
			log.error(analyticPrefix+" Tag Aliases is NULL. Validation failed.");
			throw new ValidationException(analyticPrefix + "Tag Aliases not present in the analytic configuration.");
		}
		if(checkSize((List<?>) analyticConfiguration.getConfig().getTag_aliases())){
			log.error(analyticPrefix+" Tag Aliases contains no data. Validation failed.");
			throw new ValidationException(analyticPrefix + "'tag_aliases' present without any data.");
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public JSONObject insertAnlayticData(NewAnalyticDTO newAnalyticInfo) throws Exception {
		
		JSONObject responseObject = null;
		if(sstConfig.isDebugging()){
			newAnalyticInfo.getConfig().getInput_tags().forEach(inputTag ->{
				log.info(String.format("Input Tag:: %s ", inputTag.getName() ));
			});
			newAnalyticInfo.getConfig().getOutput_tags().forEach(outputTag ->{
				log.info(String.format("Output Tag:: %s ", outputTag.getName() ));
			});
			log.info("Analytic Surplus Parameters:: " + newAnalyticInfo.getConfig().getParameters());
			BiConsumer<String, String> mapperPrinter = new BiConsumer<String, String>() {
				@Override
				public void accept(String key, String value) {
					log.info(String.format("Tag Mapping:: %s --> %s ", key, value));

				}
			};
			(newAnalyticInfo.getConfig().getTag_aliases()).forEach(mapperPrinter);
		}
		//Analytic Info Entity
		AnalyticInfoEntity analyticInfo = new AnalyticInfoEntity();
		analyticInfo.setAnalyticDescription(newAnalyticInfo.getAnalyticDescription());
		analyticInfo.setAnalyticName(newAnalyticInfo.getAnalyticName());
		analyticInfo.setAnalyticVersion(newAnalyticInfo.getAnalyticVersion());
		analyticInfo.setAnalyticDescription(newAnalyticInfo.getAnalyticDescription());
		//Validate Data Interval
		if (!this.checkNull(newAnalyticInfo.getDataInterval()) && newAnalyticInfo.getDataInterval()>0){
			analyticInfo.setDataInterval(newAnalyticInfo.getDataInterval());
		}else{
			throw new ValidationException("Data interval is invalid, should be greater than 0 and less than execution frequency.");
		}
		//Validate Analytic Platform
		if(!this.checkNull(newAnalyticInfo.getAnalyticPlatform()) && !this.checkSize(newAnalyticInfo.getAnalyticPlatform())){
			analyticInfo.setAnalyticPlatform(newAnalyticInfo.getAnalyticPlatform());
		}else{
			throw new ValidationException("Anlaytic Platform cannot be null.");
		}
		log.info("User Requested Start Date as :: " + newAnalyticInfo.getStartDate());
		SimpleDateFormat dateFormatter = new SimpleDateFormat(DataConstants.INPUT_DATE_TIME_FMT);
		Date dateObj = null;
		try{
			dateObj = dateFormatter.parse(newAnalyticInfo.getStartDate());
			log.info("Start date successfully parsed:: " + dateObj );
			analyticInfo.setStartTime(dateFormatter.parse(dateFormatter.format(dateObj)));
		}catch(Exception e){
			log.error("Failed to parse input start time");
			throw new ValidationException("Input Data is not in correct format. [mm/dd/yyyy hh:mm:ss]");
		}
		//End Time
		try{
			dateObj = dateFormatter.parse(newAnalyticInfo.getEndDate());
			log.info("End date successfully parsed:: " + dateObj );
			analyticInfo.setEndTime(dateFormatter.parse(dateFormatter.format(dateObj)));
		}catch(Exception e){
			log.error("Failed to parse input end time", e);
		}
		//Insert Offsets
		//Start Offset
		log.info("Start Offset = " + newAnalyticInfo.getStartOffset());
		analyticInfo.setStartOffset(newAnalyticInfo.getStartOffset());
		//End Offset
		log.info("End Offset = " + newAnalyticInfo.getEndOffset());
		analyticInfo.setEndOffset(newAnalyticInfo.getEndOffset());
		//Verify Execution Frequency.
		if(!this.checkNull(newAnalyticInfo.getExecutionFrequency()) && !this.checkSize(newAnalyticInfo.getExecutionFrequency())){
			analyticInfo.setExecutionFrequency(newAnalyticInfo.getExecutionFrequency());
		}else{
			throw new ValidationException("Execution Frequency cannot be null or blank."); 
		}
		//Initialize Response Object
		responseObject = new JSONObject();
		//Insert Analytic Info 
		log.info("Inserted new Analytic, Analytic ID = " + analyticInfoRepo.saveAndFlush(analyticInfo).getAnalyticId());
		try{
			//Input Tags
			JSONObject tagAliases = newAnalyticInfo.getConfig().getTag_aliases();
			newAnalyticInfo.getConfig().getInput_tags().stream().forEach(inputTag -> {
				AnalyticTagMappingEntity entity = new AnalyticTagMappingEntity();
				entity.setAnalyticInfoEntity(analyticInfo);
				//Set Tag Information
				TagInfoEntity tagInfo = new TagInfoEntity();
				tagInfo.setTagName(inputTag.getName());
				if (tagAliases.containsKey(inputTag.getName())){
					//modification for comma separated tag starts here
					String tagName = tagAliases.get(inputTag.getName()).toString();
					if (tagName.contains(",")) {
						String[] commaSeparatedTags = tagName.split(",");
						for (String individualTag : commaSeparatedTags) {
							AnalyticTagMappingEntity entityInner = new AnalyticTagMappingEntity();
							entityInner.setAnalyticInfoEntity(analyticInfo);
							TagInfoEntity tagInfoEntity = new TagInfoEntity();
							tagInfoEntity.setTagName(inputTag.getName());
							log.info("[Mapping Exists] " + inputTag.getName() + " --> Mapped To --> " + individualTag);
							tagInfoEntity.setTagAliases(individualTag);
							tagInfoEntity.setTagDescription(inputTag.getDescription());
							tagInfoEntity.setTagSource("Timeseries");
							tagInfoEntity.setTagType("inputTs");
							entityInner.setTagInfoEntity(tagInfoEntity);
							//Insert Analytic Tag Mapping Entity.
							log.info("Analytic Tag Mapping Entity ID = " + mappingRepo.saveAndFlush(entityInner).getAnalyticTagMappingId());
							log.info("Tag Mapping ID = " + mappingRepo.saveAndFlush(entityInner).getTagInfoEntity().getTagId());
						}
						
					}
					else {
					///////ends here
					
					log.info("[Mapping Exists] " + inputTag.getName() + " --> Mapped To --> " + tagAliases.get(inputTag.getName()).toString());
					tagInfo.setTagAliases(tagAliases.get(inputTag.getName()).toString());
					tagInfo.setTagDescription(inputTag.getDescription());
					tagInfo.setTagSource("Timeseries");
					tagInfo.setTagType("inputTs");
					entity.setTagInfoEntity(tagInfo);
					//Insert Analytic Tag Mapping Entity.
					log.info("Analytic Tag Mapping Entity ID = " + mappingRepo.saveAndFlush(entity).getAnalyticTagMappingId());
					log.info("Tag Mapping ID = " + mappingRepo.saveAndFlush(entity).getTagInfoEntity().getTagId());
					}
				}else{
					log.info("[Mapping Absent] " + inputTag.getName() + " --> Mapped To --> " + inputTag.getName());
					tagInfo.setTagAliases(inputTag.getName());
					tagInfo.setTagDescription(inputTag.getDescription());
					tagInfo.setTagSource("Timeseries");
					tagInfo.setTagType("inputTs");
					entity.setTagInfoEntity(tagInfo);
					//Insert Analytic Tag Mapping Entity.
					log.info("Analytic Tag Mapping Entity ID = " + mappingRepo.saveAndFlush(entity).getAnalyticTagMappingId());
					log.info("Tag Mapping ID = " + mappingRepo.saveAndFlush(entity).getTagInfoEntity().getTagId());
				}
			});
			responseObject.put("input_tags_added", newAnalyticInfo.getConfig().getInput_tags().stream().count());
		}catch(Exception e){
			throw new ValidationException("Failed to store input tags to database.");
		}

		try{
			//Output Tags
			newAnalyticInfo.getConfig().getOutput_tags().stream().forEach(outputTag -> {
				AnalyticTagMappingEntity entity = new AnalyticTagMappingEntity();
				entity.setAnalyticInfoEntity(analyticInfo);
				//Set Tag Information
				TagInfoEntity tagInfo = new TagInfoEntity();
				tagInfo.setTagName(outputTag.getName());
				tagInfo.setTagDescription(outputTag.getDescription());
				tagInfo.setTagSource("Timeseries");
				tagInfo.setTagType("outputTs");
				tagInfo.setTagAliases(outputTag.getName());
				tagInfo.setUnit(outputTag.getUnit());
				tagInfo.setAnalyticTagMappingEntity(entity);
				entity.setTagInfoEntity(tagInfo);
				
				log.info(String.format("Inserting Output Tag:: %s ", tagInfo.getTagName()));
				//Actually Insert the Output Tag
				mappingRepo.saveAndFlush(entity);
				log.info(String.format("Inserted Output Tag:: %s with Tag ID= %s", tagInfo.getTagName(), tagInfo.getTagId()));
			});
			responseObject.put("output_tags_added", newAnalyticInfo.getConfig().getOutput_tags().stream().count());
		}catch(Exception e){
			throw new ValidationException("Failed to store output tags to database.");
		}

		try{
			//Analytic Parameter List
			ObjectMapper mapper = new ObjectMapper();
			//Store Analytic Parameters.
			newAnalyticInfo.getConfig().getParameters().keySet().forEach(key -> {
				//If it is analytic parameters then store it to a different table. 
				if (key.equalsIgnoreCase("analytic_parameters")){
					try{
						List< ? extends Object> parametersList = (List<?>) newAnalyticInfo.getConfig().getParameters().get("analytic_parameters");
						parametersList = (List<TagInfoDTO>) parametersList;
						parametersList.forEach(parameter ->{
							TagInfoDTO tagInfo = mapper.convertValue(parameter, TagInfoDTO.class);
							AnalyticParameterListEntity parameterList = new AnalyticParameterListEntity();
							parameterList.setAnalyticInfoEntity(analyticInfo);
							parameterList.setTagDescription(tagInfo.getDescription());
							parameterList.setTagName(tagInfo.getName());
							parameterList.setTagValue(tagInfo.getValue());
							log.info(String.format("Storing Tag[%s = Value(%s)]) with ID:: %s", parameterList.getTagName(), parameterList.getTagValue(), analyticParameterInsertion.saveAndFlush(parameterList).getTagId()));
						});
					}catch(Exception e){
						log.error("Error converting analytic parameters to List<TagInfoDTO>, format not adhered.", e);
					}
				}else{
					//If it is not "analytic_parameters" then store it as key/value in surplus constants.
					log.info("Found additional parameters, storing as a JSON");
					AnalyticSurplusConstantsEntity surplusConstants = new AnalyticSurplusConstantsEntity();
					surplusConstants.setAnalyticInfoEntity(analyticInfo);
					surplusConstants.setTagName(key);
					try{
						surplusConstants.setTagValue(new JSONArray((List<? extends Object>) newAnalyticInfo.getConfig().getParameters().get(key)).toString());
					}catch(Exception e){
						try{
							surplusConstants.setTagValue(new JSONObject((Map<? extends Object, ? extends Object>) newAnalyticInfo.getConfig().getParameters().get(key)).toString());
						}catch(Exception ex){
							log.error("Failed to convert to JSONObject, storing data as is. ");
							surplusConstants.setTagValue(newAnalyticInfo.getConfig().getParameters().get(key).toString());
						}
					}
					log.info(String.format("Inserting Surplus Constants to Database:: [%s] --> ID=%s", key, surplusInsertion.saveAndFlush(surplusConstants).getTagId()));
				}
			});
			
			// hard code asset ID
			
			/*List<String> assetIds = new ArrayList<String>();
			assetIds.add(0, ExternalPropertyUtil.getPropValueFromExternalConfig("assetId"));
			newAnalyticInfo.setAssetId(assetIds);
			*/
			//*******
			

			
						
			log.info("Asset IDs -------->" + newAnalyticInfo.getAssetId());
			log.info("Proceed to insert asset data:: " + ((! Objects.isNull(newAnalyticInfo.getAssetId())) && newAnalyticInfo.getAssetId().size() > 0));
			List<String> jobIds = new ArrayList<>();
			
			// asset related changes needs to be done here
			if((! Objects.isNull(newAnalyticInfo.getAssetId())) && newAnalyticInfo.getAssetId().size() > 0){
				newAnalyticInfo.getAssetId().stream().forEach(assetId -> {
					String param = "sourcekey=" + newAnalyticInfo.getAssetId().get(0);
					HttpConnector httpConnector = new HttpConnector();
					
					JSONObject jsonObject = new JSONObject();
					try{
						jsonObject = httpConnector.getRequests("/assets/", param);
						log.info("getRequests data");
						log.info(jsonObject.toJSONString());
						String responseStr = jsonObject.toJSONString();
						Gson gson = GsonBuilder.class.newInstance().create();
						ServiceResponse serviceResponse = gson.fromJson(responseStr, ServiceResponse.class);
						log.info("Service response :: " + serviceResponse.toString());
							AnalyticAssetMappingEntity analyticAssetMappingEntity = new AnalyticAssetMappingEntity();
							AssetDTO assetDTO = serviceResponse.getResult();
							analyticAssetMappingEntity.setAnalyticInfoEntity(analyticInfo);
							analyticAssetMappingEntity.setAssetId(assetDTO.getSourceKey());
							analyticAssetMappingEntity.setAssetName(assetDTO.getAssetName());
							analyticAssetMappingEntity.setAssetType(assetDTO.getAssetType());
							try {
								analyticAssetMappingEntity = analyticAssetMappingRepo.saveAndFlush(analyticAssetMappingEntity);
								if (Objects.nonNull(analyticAssetMappingEntity)) {
									log.info("AnalyticAssetMapping data is inserted with id ::" + analyticAssetMappingEntity.getAnalyticAssetMappingId());
									//Create Job for a given analytic id.
									try {
										jobIds.add(this.createAnalyticJobs(analyticInfo, newAnalyticInfo, assetId));
									} catch (ParseException e) {
										log.error("Failed to add job for Asset ID = " + assetId, e);
									}
								}else {
									throw new ValidationException("Error Occured while inserting data for AnalyticAssetMapping");
								}
							}catch (Exception e) {
								log.error(e.getMessage());
							}
							
						}catch (Exception e) {
							log.error(e.getMessage());
						}
				});
			}
			// temp change ends here
			
			//Map Asset IDs
			/*if((! Objects.isNull(newAnalyticInfo.getAssetId())) && newAnalyticInfo.getAssetId().size() > 0){
				newAnalyticInfo.getAssetId().stream().forEach(assetId -> {
					log.info("Asset ID = " + assetId);
					EquipmentInfoEntity equipment = null;
					try{
						equipment = equipmentInfo.getEquipmentByTsn(assetId);
						log.warn("Equipment Entity:: " + equipment);
						if (Objects.isNull(equipment))
							throw new OnPremiseException("Asset ID Does not exist.");
					}catch(Exception e){
						log.warn("Asset ID not found in the database.");
						log.debug("Asset ID not found in the database.", e);
						equipment = new EquipmentInfoEntity();
						//Add Asset ID Mapping.
						equipment.setTsn(assetId);
						equipmentInfo.saveAndFlush(equipment);
					}
					//Check Mapping
					EquipmentAnalyticMappingEntity equipmentMappingEntity = null;
					try{
						equipmentMappingEntity = equipmentMapping.getEquipmentEntity(analyticInfo.getAnalyticId(), assetId);
						log.warn("Equipment Entity:: " + equipmentMappingEntity);
						if (! Objects.isNull(equipmentMappingEntity))
							throw new OnPremiseException("Asset ID Does not exist.");
						else
							equipmentMappingEntity = null;
					}catch(Exception e){
						log.info("Equipment <-> Analytic Mapping exists. No need to insert or update.");
					}
					
					//Equipment Mapping Entity.
					if(Objects.isNull(equipmentMappingEntity)){
						log.info("Inserting Equipment <-> Analytic Mapping");
						equipmentMappingEntity = new EquipmentAnalyticMappingEntity();
						equipmentMappingEntity.setAnalyticInfoEntity(analyticInfo);
						equipmentMappingEntity.setEquipmentInfoEntity(equipment);
						equipmentMapping.saveAndFlush(equipmentMappingEntity);
						log.info("Inserting Equipment <-> Analytic Mapping ::COMPLETED:: ID = " + equipmentMappingEntity.getEquipmentAnalyticMappingId());
					}
					
					//Create Job for a given analytic id.
					try {
						jobIds.add(this.createAnalyticJobs(analyticInfo, newAnalyticInfo, assetId));
					} catch (ParseException e) {
						log.error("Failed to add job for Asset ID = " + assetId, e);
					}
				});
			}*/
			//Map Asset IDs :: END
			
			//Return Job ID: 
			responseObject.put("parameters_added", newAnalyticInfo.getConfig().getParameters().keySet().stream().count());
			responseObject.put("success", true);
			responseObject.put("analyticId", analyticInfo.getAnalyticId());
			responseObject.put("message",String.format("Analytic configuration successfully added. [Job(s) Scheduled: %s]", jobIds));
			return responseObject;
		}catch(Exception e){
			throw new ValidationException("Failed to store analytic parameters.");
		}

	}
	
	public String createAnalyticJobs(AnalyticInfoEntity analyticInfo, NewAnalyticDTO newAnalyticInfo, String assetId) throws ParseException{
		AnalyticRunEntity analyticRun = new AnalyticRunEntity();
		analyticRun.setAnalyticInfoEntity(analyticInfo);
		analyticRun.setIsRunning(false);
		analyticRun.setIsScheduled(true);
		analyticRun.setStartDate(newAnalyticInfo.getStartDate());
		analyticRun.setJobId(UUID.randomUUID().toString());
		analyticRun.setAssetId(assetId);
		//Set Timestamps
		SimpleDateFormat dateParser = new SimpleDateFormat(DataConstants.INPUT_DATE_TIME_FMT);
		dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date startDate = dateParser.parse(newAnalyticInfo.getStartDate());
		//End Date = Start Date (ms) + Execution Frequency (ms)
		Long endEpoch = startDate.getTime() + Long.parseLong(newAnalyticInfo.getExecutionFrequency())*60*1000; 
		analyticRun.setEndDate(dateParser.format(new Date(endEpoch)));
		runStatus.saveAndFlush(analyticRun);
		return analyticRun.getJobId();
	}

	public File convert(MultipartFile file) throws IOException
	{    
		File convFile = new File(file.getOriginalFilename());
		convFile.createNewFile(); 
		FileOutputStream fos = new FileOutputStream(convFile); 
		fos.write(file.getBytes());
		fos.close(); 
		return convFile;
	}


	public org.json.JSONObject storeArtifact(MultipartFile analyticFile ,String analyticName, String analyticVersion) throws JSONException{
		org.json.JSONObject response = new org.json.JSONObject();
		File newfile = null;
		try {
			MultiValueMap<String, Object> requestParts = new LinkedMultiValueMap<>();
			//Convert MultiPart file to File.
			newfile = this.convert(analyticFile);
			//Set Headers
			HttpHeaders headersMap = new HttpHeaders();
			headersMap.setContentType(MediaType.MULTIPART_FORM_DATA);
			//Create Form-Data
			requestParts.add("analyticName", analyticName);
			requestParts.add("analyticVersion", analyticVersion);
			requestParts.add("analyticPackage", new FileSystemResource(newfile.getAbsolutePath()));
			//Rest-Template
			RestTemplate restTemplate = new RestTemplate();
			//Create HttpEntity
			HttpEntity<Object> entity = new HttpEntity<Object>(requestParts, headersMap);
			//Make REST Call.
			response = new org.json.JSONObject(restTemplate.exchange(new URI(sstConfig.getAddAnalyticUrl()), HttpMethod.POST, entity, String.class).getBody());
			response.put("success", true);
		} catch (URISyntaxException | IllegalStateException | IOException e) {
			response.put("message", e.getMessage());
			response.put("error", "ARTIFACT_UPLOAD_FAILED");
			response.put("failure", true);
		}finally{
			if(!Objects.isNull(newfile)){
				try {
					FileUtils.forceDelete(newfile);
					log.info("Analytic artifact deleted.");
				} catch (IOException e) {
					log.error("Failed to delete analytic artifact.");
					log.debug("Failed to delete analytic artifact.", e);
				}
			}
		}
		return response;
	}

	private boolean checkNull(Object toValidate){
		return Objects.isNull(toValidate);
	}
	private boolean checkSize(List<?> listObject){
		return listObject.size()==0;
	}
	private boolean checkSize(String stringObject){
		return stringObject.trim().length()==0;
	}
}
