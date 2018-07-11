package com.apm.datarw.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.ValidationException;
import javax.xml.crypto.Data;

import org.apache.commons.lang.time.DateUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.apm.datarw.config.SSTConfigurations;
import com.apm.datarw.dto.AlarmDTO;
import com.apm.datarw.dto.AnalyticOutputDataDTO;
import com.apm.datarw.dto.PythonRequestDTO;
import com.apm.datarw.dto.ValidateAnalyticRequestDTO;
import com.apm.datarw.entity.AnalyticAlarmEntity;
import com.apm.datarw.entity.AnalyticAlarmScangroupEntity;
import com.apm.datarw.entity.AnalyticAssetMappingEntity;
import com.apm.datarw.entity.AnalyticInfoEntity;
import com.apm.datarw.entity.AnalyticResultState;
import com.apm.datarw.entity.AnalyticRunEntity;
import com.apm.datarw.exceptions.OnPremiseException;
import com.apm.datarw.inf.IAnalyticDetailsImpl;
import com.apm.datarw.kairosdb.AddDatapointsIntoKairosDB;
import com.apm.datarw.kairosdb.MapToTimeSeriesFormatImpl;
import com.apm.datarw.repo.IAlarmScangroupData;
import com.apm.datarw.repo.IAnalyticAlarmRepo;
import com.apm.datarw.repo.IAnalyticAssetMappingRepo;
import com.apm.datarw.repo.IAnalyticInfoRepo;
import com.apm.datarw.repo.IAnalyticOutputRepo;
import com.apm.datarw.repo.IAnalyticResultStateRepo;
import com.apm.datarw.repo.IAnalyticRunStatus;
import com.apm.datarw.repo.ITagInfoRepo;
import com.apm.datarw.utils.DataConstants;
import com.apm.datarw.utils.ExternalPropertyUtil;
import com.apm.datarw.utils.HistorianWrapper;

@Service
@Transactional
public class ExecutionDataStorageImpl {
	private static final Logger log = LoggerFactory.getLogger(ExecutionDataStorageImpl.class);

	@Autowired
	IAnalyticInfoRepo analyticRepo;

	@Autowired
	IAnalyticOutputRepo analyticOutputEntity;

	@Autowired
	IAnalyticRunStatus runStatus;

	@Autowired
	EntityManager entityManager;

	@Autowired
	ITagInfoRepo iTagInfoRepo;

	@Autowired
	SSTConfigurations sstConfigurations;

	@Autowired
	IAnalyticDetailsImpl analyticDetails;

	@Autowired
	IAnalyticResultStateRepo ianalyticResultStateRepo;

	/*@Autowired
	IEquipmentAnalyticMappingRepo equipmentRepo;
*/
	@Autowired
	IAnalyticAlarmRepo alarmRepo;

	@Autowired
	IAlarmScangroupData scangroupRepo;
	
	/*@Autowired
	IEquipmentInfoRepo iEquipmentInfoRepo;
*/
	@Autowired
	IAnalyticAssetMappingRepo analyticAssetMappingRepo;
	
	public ExecutionDataStorageImpl(){

	}

	private AnalyticInfoEntity getAnalyticEntity(Long analyticId){
		AnalyticInfoEntity analyticEntity = null;
		try{
			analyticEntity = analyticRepo.getOne(analyticId);
			log.info("Analytic Info:: " + analyticEntity.toString());
		}catch(Exception e){
			log.error("Analytic not found with ID = " + analyticId);
		}
		return analyticEntity;
	}

	private AnalyticRunEntity updateAnalyticExecution(AnalyticRunEntity analyticInfo) throws ParseException{
		//Remove entity object from Persistence.
		entityManager.detach(analyticInfo);
		//Update Job ID
		analyticInfo.setJobId(UUID.randomUUID().toString());
		String analyticPrefix = String.format("[%s-%s][%s]", analyticInfo.getAnalyticInfoEntity().getAnalyticName(), analyticInfo.getAnalyticInfoEntity().getAnalyticVersion(), analyticInfo.getAssetId());
		log.info(String.format("Parsing metadata for %s", analyticPrefix));
		SimpleDateFormat dateParser = new SimpleDateFormat(DataConstants.INPUT_DATE_TIME_FMT);
		dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
		log.info(analyticPrefix + " passed previous execution.");
		Date startDate = dateParser.parse(analyticInfo.getStartDate());
		//If the end time is not null.  
		if(! Objects.isNull(analyticInfo.getAnalyticInfoEntity().getEndTime())){
			//End Time :: Not Blank
			if (!analyticInfo.getAnalyticInfoEntity().getEndTime().equals("")){
				log.info("User Defined End Date = " + analyticInfo.getAnalyticInfoEntity().getEndTime());
				log.info(analyticInfo.getAnalyticInfoEntity().getEndTime().before(startDate)?"::UPDATE:: End Date < Start Date":"::UPDATE:: Start Date < End Date");
				if (analyticInfo.getAnalyticInfoEntity().getEndTime().before(startDate)){
					log.info("Analytic start date has exceeded the user defined end date, stopping updation for future jobs. Analytic ID = " + analyticInfo.getAnalyticInfoEntity().getAnalyticId());
					//Return null here itself so that nothing is carried to repository.
					return null;
				}
			}
		}
		log.debug(analyticPrefix + " Old Start Date = " + analyticInfo.getStartDate());
		Date endDate = dateParser.parse(analyticInfo.getEndDate());
		log.debug(analyticPrefix + " Old End Date = " + analyticInfo.getEndDate());
		//Convert Minutes to Milliseconds.
		startDate = new Date(startDate.getTime() + Long.parseLong(analyticInfo.getAnalyticInfoEntity().getExecutionFrequency())*60*1000);
		endDate = new Date(endDate.getTime() + Long.parseLong(analyticInfo.getAnalyticInfoEntity().getExecutionFrequency())*60*1000);
		//Set new Start/End Date
		analyticInfo.setStartDate(dateParser.format(startDate));
		analyticInfo.setEndDate(dateParser.format(endDate));
		log.info(analyticPrefix + " New Start Date = " + analyticInfo.getStartDate());
		log.info(analyticPrefix + " New End Date = " + analyticInfo.getEndDate());
		//Set Analytic Status isRunning = false because we want the scheduler to pick it up.
		analyticInfo.setIsRunning(false);
		analyticInfo.setIsScheduled(true);
		return analyticInfo;
	}

	private void createScheduledJob(String oldJobId){
		//Get the Analytic Run Job Object from Database.
		log.info(String.format("Marking %s job as complete.", oldJobId));
		AnalyticRunEntity analyticRunStatus = runStatus.getJobForJobId(oldJobId);
		analyticRunStatus.setIsRunning(false);
		analyticRunStatus.setIsScheduled(false);
		analyticRunStatus.setJobStatus(DataConstants.SUCCESS);
		runStatus.saveAndFlush(analyticRunStatus);
		log.info(String.format("::COMPLETED:: Marking %s job as complete.", oldJobId));
		try {
			//Update the analytic job status
			AnalyticRunEntity newRun = this.updateAnalyticExecution(analyticRunStatus);
			if (Objects.isNull(newRun)){
				//Purposely sending null in case the Start Date > User defined End Date.
				log.info("Analytic execution lifecycle complete. Analytic = " + analyticRunStatus.getAnalyticInfoEntity().getAnalyticName());
			}else{
				log.info("New Run Job:: " + newRun.toString());
				runStatus.save(newRun);
			}
		} catch (ParseException e) {
			log.error("Failed to create a new job.", e);
		}
		//Commit Data
		runStatus.flush();
	}

	@Async("processExecutor")
	public void storeOutputData(AnalyticOutputDataDTO analyticOutput){
		SimpleDateFormat dateFormat = new SimpleDateFormat(DataConstants.ISO_DATE_TIME_FMT);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		AnalyticInfoEntity analyticInfoEntity = getAnalyticEntity(analyticOutput.getAnalyticId());		
		if(Objects.isNull(analyticInfoEntity)){
			throw new ValidationException("Analytic Info not found. Invalid Analytic ID :: " + analyticOutput.getAnalyticId());
		}else{
			try{
				String analyticOutputString = analyticOutput.getOutputData();
				//EquipmentAnalyticMappingEntity equipmentAnalyticMapping = null;
				AnalyticAssetMappingEntity analyticAssetMappingEntity = null;
				AnalyticResultState stateEntity = null;
				//Find Equipment & Analytic Mapping 
				try{
					//equipmentAnalyticMapping = equipmentRepo.getEquipmentEntity(analyticOutput.getAnalyticId(), analyticOutput.getAssetId());
					analyticAssetMappingEntity = analyticAssetMappingRepo.getAnalyticAssetMapByAnalyticIdAndAssetId(analyticOutput.getAnalyticId(), analyticOutput.getAssetId());
					//We need not do anything if the mapping does not exist. 
					if(Objects.isNull(analyticAssetMappingEntity)){
						throw new OnPremiseException("Equipment <-> Analytic mapping does not exist in database.");
					}
					//final EquipmentAnalyticMappingEntity equipMapping = equipmentAnalyticMapping;
					final AnalyticAssetMappingEntity analyticAssetMappingEntity2 = analyticAssetMappingEntity;
					//Alarms Insertion
					if(!Objects.isNull(analyticOutput.getAlarms())){
						List<AlarmDTO> alarms = analyticOutput.getAlarms();
						alarms.stream().forEach(alarm -> {
							AnalyticAlarmEntity alarmEntity = new AnalyticAlarmEntity();
							//alarmEntity.setEquipmentAnalyticMappingEntity(equipMapping);
							alarmEntity.setAnalyticAssetMappingEntity(analyticAssetMappingEntity2);
							log.info("Alarm Name = " + alarm.getName());
							alarmEntity.setAlarmName(alarm.getName());
							//If Asset ID is null or blank then use parent id.
							try{
								if(alarm.getAssetId().trim().equals("")){
									alarm.setAssetId(analyticOutput.getAssetId());
								}
							}catch(Exception e){
								alarm.setAssetId(analyticOutput.getAssetId());
							}

							try {
								alarmEntity.setAlarmTimestamp(dateFormat.parse(alarm.getDatetime()));
							} catch (Exception e) {
								log.error("Failed to parse timestamp :: " + alarm.getDatetime(), e);
							}
							//Set Asset ID in entity.
							//alarmEntity.setEquipmentAnalyticMappingEntity(equipMapping);
							alarmEntity.setAnalyticAssetMappingEntity(analyticAssetMappingEntity2);
							alarmRepo.save(alarmEntity);
							log.info("Alarm Inserted with ID :: " + alarmEntity.getAlarmId());
							try{
								alarm.getScanGroup().entrySet().stream().forEach(scangroup -> {
									AnalyticAlarmScangroupEntity scangroupEntity = new AnalyticAlarmScangroupEntity();
									scangroupEntity.setAnalyticAlarmEntity(alarmEntity);
									scangroupEntity.setTagName(scangroup.getKey());
									scangroupEntity.setTagValue(scangroup.getValue());
									scangroupRepo.saveAndFlush(scangroupEntity);
									log.info("Inserted Scangroup Data. ID :: " + scangroupEntity.getScangroupId());
								});
							}catch(Exception e){
								log.error("No Scan Group Data for Alarm:: " + alarm.getName());
							}finally{
								alarmRepo.flush();
							}
						});
					}
					
				}catch(Exception e){
					log.error("Asset ID <-> Analytic ID mapping not found.");
					log.debug("Failed to get analytic and asset mapping.", e);
				}
				//State Insertion
				try{
					log.info(String.format("Searching for Analytic ID:: %s; Asset ID:: %s", analyticOutput.getAnalyticId(), analyticOutput.getAssetId()));
					stateEntity = ianalyticResultStateRepo.getResultStateEntityValue(analyticOutput.getAnalyticId(), analyticOutput.getAssetId());
					log.info("State data present, ID:: " + stateEntity.getAnalyticResultStateId());
				}catch(Exception e){
					log.error("Equipment <-> Asset Mapping absent ::State data not found:: ", e);
				}

				//If state is inserted then update the data.
				if (!Objects.isNull(stateEntity)){
					log.info("Inserting State Data --> ");
					stateEntity = ianalyticResultStateRepo.findOne(stateEntity.getAnalyticResultStateId());
					stateEntity.setValue(analyticOutput.getStateData());
					ianalyticResultStateRepo.saveAndFlush(stateEntity);
					log.info("Inserting State Data --> ::COMPLETED:: ID= "+ stateEntity.getAnalyticResultStateId());
				}

				//If state is not inserted then create a new record.
				if(!Objects.isNull(analyticAssetMappingEntity) && Objects.isNull(stateEntity)){
					log.info("Inserting new state data information.");
					stateEntity = new AnalyticResultState();
					//stateEntity.setEquipmentAnalyticMappingEntity(equipmentAnalyticMapping);
					stateEntity.setAnalyticAssetMappingEntity(analyticAssetMappingEntity);
					stateEntity.setValue(analyticOutput.getStateData());
					ianalyticResultStateRepo.saveAndFlush(stateEntity);
					log.info("Inserting State Data --> ::COMPLETED:: ID= "+ stateEntity.getAnalyticResultStateId());
				}

				//State Insertion:: End
			//	log.info("Total Result Rows = " + analyticResults.size());
				//analyticOutputEntity.save(analyticResults);// data get inserted into analytic_output_result table
				AddDatapointsIntoKairosDB addDatapointsIntoKairosDB = new AddDatapointsIntoKairosDB();
				//get analytic name and passit to kairosdb
				//AnalyticInfoEntity analyticInfoEntity2 = analyticRepo.getOne(analyticOutput.getAnalyticId());

				if(analyticOutputString != null) {
					try {
					addDatapointsIntoKairosDB.addDatapointsIntoKairosDB(analyticOutputString,analyticInfoEntity.getAnalyticName());
					log.info(String.format("Output Data Written for Analytic ID:: %s [%s]", analyticOutput.getAnalyticId(), analyticOutput.getAssetId()));
					}catch (Exception e) {
						log.warn("No output data available for analytic id :: " + analyticOutput.getAnalyticId());
						log.error(e.getMessage());
					}
				}
				else{
					log.warn("No output data available for analytic id :: " + analyticOutput.getAnalyticId());
				}
				//analyticOutputEntity.flush();
				
				log.info("Creating a new job for the analytic " + analyticOutput.getAnalyticId());
				this.createScheduledJob(analyticOutput.getJobId());
				log.info("Creating a new job for the analytic :: COMPLETED " + analyticOutput.getAnalyticId());
			}catch(Exception e){
				log.error("Failed to parse analytic output data to JSON", e);
			}
		}
	}

	private Map<String, String> getDataFromHistorian(AnalyticRunEntity job, Long startDate, Long endDate){
		Map<String, String> historianData = null;
		List<String> tagTypeList= iTagInfoRepo.findtagNameByType(job.getAnalyticInfoEntity().getAnalyticId(), "inputTs");
		if(!Objects.isNull(tagTypeList) && tagTypeList.size()>0){
			try {
				historianData = (HistorianWrapper.getDataMap(tagTypeList, job.getAssetId(), startDate, endDate, job.getAnalyticInfoEntity().getDataInterval(), sstConfigurations.getServerAddress(), sstConfigurations.getServerUsername(), sstConfigurations.getServerPassword()));
			} catch (Exception e) {
				log.error("Failed to fetch data from Proficy Historian", e);
			}
		}
		return historianData;
	}

	
	/*
	 * Get data from Central Historian
	 * */
	
	public Map<String,String> fetchCentralHistorianData(AnalyticRunEntity job) throws ParseException {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(DataConstants.INPUT_DATE_TIME_FMT);
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		Map<String,String> responseMap= new HashMap<>();
		if(job!=null){
			long startDate= dateFormatter.parse(job.getStartDate()).getTime();
			long endDate=(dateFormatter.parse(job.getEndDate()).getTime());
			log.info("startDate :: "+""+startDate);
			log.info("endDate :: "+""+endDate);
			log.info("historian server ip address :: \t" + sstConfigurations.getServerAddress());
			
					String assetId = job.getAssetId();
					List<String> tagTypeList =iTagInfoRepo.findAliasesByType(job.getAnalyticInfoEntity().getAnalyticId(),"inputTs");		
					log.info("tagTypeList size :: " + tagTypeList.size());
						if(tagTypeList.size()>0&&tagTypeList!=null){
							try {
								responseMap=HistorianWrapper.getDataMap(tagTypeList,assetId,startDate,endDate,job.getAnalyticInfoEntity().getDataInterval(),
										sstConfigurations.getServerAddress(),
										sstConfigurations.getServerUsername(),
										sstConfigurations.getServerPassword());
							} catch (Exception e) {
								log.error(e.getMessage());
							}
						}
		}		
		return responseMap;
	}
	
	/**
	 * We will run the scheduler every <b>60s</b> to run scheduled jobs. This duration can also be increased to a higher number
	 * depending on the minimum duration. 
	 */
	@Scheduled(fixedDelay= 60000L)
	public void runScheduler(){
		log.info("-----> Starting Scheduler ----->");
		//If you convert new Date() to epoch, raw offset is reduced. Thereby subtracting 330 minutes.
		Long currentTime = new Date().getTime() + TimeZone.getDefault().getRawOffset();
		log.info("Current Epoch Timestamp = " + currentTime);
		SimpleDateFormat dateFormatter = new SimpleDateFormat(DataConstants.INPUT_DATE_TIME_FMT);
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			runStatus.fetchJobsToStart().forEach(job -> {
				try {
					PythonRequestDTO pythonRequest = null;
					Long analyticEndTime = dateFormatter.parse(job.getEndDate()).getTime();
					log.info("End Time = " + analyticEndTime);
					if(currentTime >= analyticEndTime){
						log.info(String.format("Scheduling:: [%s-%s]", job.getAnalyticInfoEntity().getAnalyticName(), job.getAnalyticInfoEntity().getAnalyticVersion()));

						// hist to csv changes starts here **********
						//Map<String, String> historianData = this.getDataFromHistorian(job, dateFormatter.parse(job.getStartDate()).getTime(), analyticEndTime);
						// hist to csv changes ends here **********

						pythonRequest = analyticDetails.facadeForScheduler(job);
						pythonRequest.setAnalyticVersion(job.getAnalyticInfoEntity().getAnalyticVersion());
						pythonRequest.setAnalyticId(String.valueOf(job.getAnalyticInfoEntity().getAnalyticId()));
						pythonRequest.setAnalyticName(job.getAnalyticInfoEntity().getAnalyticName());
						pythonRequest.setAssetId(job.getAssetId());
						pythonRequest.setJobId(job.getJobId());
						pythonRequest.setValidationRequest(false);
						String stateValue = null;
						try{
							//stateValue = ianalyticResultStateRepo.getEquipmentEntity(job.getAssetId(), job.getAnalyticInfoEntity().getAnalyticId()).getValue();
							stateValue = ianalyticResultStateRepo.getResultStateEntityValue(job.getAnalyticInfoEntity().getAnalyticId(), job.getAssetId()).getValue();
							pythonRequest.putStateInfo("state", stateValue);
						}catch(Exception e){
							log.error("State data is absent in the database.");
						}
						if (! Objects.isNull(stateValue)){
							pythonRequest.putStateInfo("state", stateValue);
							log.info("::COMPLETE:: State data written in request.");
						}
				

						// hist to csv changes starts here **********
						//if (historianData.containsKey("success")){
						
						byte[] array = null;
							try {
							array = Files.readAllBytes(new File(ExternalPropertyUtil.getPropValueFromApplicationConfig("inputDataFile")).toPath());
							}catch (Exception e) {
								setIsRunningAsFalseByJobId(job.getAnalyticInfoEntity().getAnalyticId(), job.getJobId());
							}
							String input_data = new String(array);
							pythonRequest.setInput_data(input_data);
							
							// hist to csv changes ends here **********
							
							//If we get input data, only then should we execute the anlaytic.
							try{
								log.info("Requesting execution for Analytic ID:: " + pythonRequest.getAnalyticId());
								RestTemplate rest = new RestTemplate();
								//call to python to execute analytics
								rest.postForObject(sstConfigurations.getAsyncRunUrl(), new JSONObject(pythonRequest.toString()), String.class);
								job.setIsRunning(true);
								//Update Run Status
								this.runStatus.saveAndFlush(job);
								rest = null;
							}catch(Exception e){
								log.error("Failed to execute analytic :: " + pythonRequest.getAnalyticName());
								job.setIsRunning(false);
								job.setJobStatus(DataConstants.FAILURE);
								this.runStatus.saveAndFlush(job);
							}
						//}
						//histWrapper.getDataMap(tagList, assetId, startDate, endDate, interval, serverAddress, serverUserName, serverPassword)
					}else{
						log.info(String.format("[%s-%s] is for the future, ignoring the job.", job.getAnalyticInfoEntity().getAnalyticName(), job.getAnalyticInfoEntity().getAnalyticVersion()));
					}
				} catch (ParseException e) {
					log.error("Failed to parse start date.", e);
				}
			});
		} catch (Exception e) {
			log.error("Failed to schedule jobs.", e);
		}
		log.info("-----> Shutting Down Scheduler ----->");
	}

	public JSONObject validateAnalytic(ValidateAnalyticRequestDTO validateAnalyticRequestDTO, MultipartFile inputData) throws JSONException {
		log.info("inside validateAnalytic method");
		JSONObject jsonObject = new JSONObject();
		PythonRequestDTO pythonRequest = new PythonRequestDTO();
		String inputString = null;
		File inputFile = null;
		try {
			inputFile = this.convert(inputData);
			byte[] array = Files.readAllBytes(inputFile.toPath());
			inputString = new String(array);
		} catch (IOException e1) {
			log.error(e1.getMessage());
		}		
		try{
			pythonRequest.setAnalyticId(ExternalPropertyUtil.getPropValueFromExternalConfig("validationAnalyticId"));
			pythonRequest.setAnalyticName(validateAnalyticRequestDTO.getAnalytic_name());
			pythonRequest.setAnalyticVersion(validateAnalyticRequestDTO.getAnalytic_version());
			pythonRequest.setAssetId(validateAnalyticRequestDTO.getAsset_id());
			pythonRequest.setConfig(validateAnalyticRequestDTO.getConfig());
			pythonRequest.setInput_data(inputString);
			pythonRequest.setJobId(UUID.randomUUID().toString());
			pythonRequest.setValidationRequest(true);
			pythonRequest.putStateInfo("state", "");
			JSONObject jsonObject2 = new JSONObject(pythonRequest);
			log.info("Requesting execution for Analytic ID:: " + pythonRequest.getAnalyticId());
			RestTemplate rest = new RestTemplate();
			//call to python to execute analytics
			rest.postForObject(sstConfigurations.getAsyncRunUrl(), jsonObject2.toString(), String.class);
			rest = null;
			jsonObject.put("success", "Your analytic validation request is being queued");
			jsonObject.put("message", "Use jobId to get your validaton Request output data");
			jsonObject.put("jobId", pythonRequest.getJobId());
		}catch(Exception e){
			jsonObject.put("failure", "Failed to execute analytic :: " + pythonRequest.getAnalyticName());
			log.error("Failed to execute analytic :: " + pythonRequest.getAnalyticName());
		}
		finally{
			if(!Objects.isNull(inputFile)){
				try {
					FileUtils.forceDelete(inputFile);
					log.info("Analytic artifact deleted.");
				} catch (IOException e) {
					log.error("Failed to delete analytic artifact.");
					log.debug("Failed to delete analytic artifact.", e);
				}
			}
		}
		return jsonObject;
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

	@SuppressWarnings("unchecked")
	public org.json.simple.JSONObject getValReqOutputData(String jobId) throws org.json.simple.parser.ParseException, IOException {
		org.json.simple.JSONObject outputData = new org.json.simple.JSONObject();
		byte[] array = null;
		try {
			try {
			array = Files.readAllBytes(new File(jobId+".json").toPath());
			}catch (Exception e) {
				outputData.put("error", "output is not present for jobId "+jobId);
			}
			if(!Objects.isNull(array)) {
			String str = new String(array);
			org.json.simple.JSONObject outData = new org.json.simple.JSONObject((org.json.simple.JSONObject) new JSONParser().parse(str));
			outputData = outData;
			}
			else {
				outputData.put("error", "output is not present");
			}
		}
		finally {
			byte[] byteArray = null;
			try {
				byteArray = Files.readAllBytes(new File(jobId+".json").toPath());
			} catch (IOException e1) {
				outputData.put("error", "output is not present");
			}
			if(!Objects.isNull(byteArray)){
				try {
					FileUtils.forceDelete(new File(jobId+".json"));
					log.info("Analytic output data file deleted");
				} catch (IOException e) {
					log.error("Failed to delete analytic output data file");
					log.debug("Failed to delete analytic output data file", e);
				}
			}
		}
		return outputData;
	}

	public void setIsRunningAsFalseByJobId(long analyticId, String jobId) {
		try {
			AnalyticRunEntity analyticRunEntity = runStatus.getAnalyticByAnalyticIdAndJobId(analyticId, jobId);
				if(!(Objects.isNull(analyticRunEntity))) {
					analyticRunEntity.setIsRunning(false);
					analyticRunEntity.setJobStatus(DataConstants.FAILURE);
					runStatus.saveAndFlush(analyticRunEntity);
				}
				else {
					log.error("Failed to update is_running status for jobId :: " + jobId);
					throw new SQLException();
				}
		}
		catch (Exception e) {
			log.error("Failed to update is_running status for jobId :: " + jobId +"\n" + e.getMessage());
		}
	}
}
