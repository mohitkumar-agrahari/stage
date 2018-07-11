package com.apm.datarw.controller;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import javax.validation.Valid;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import com.apm.datarw.dto.AnalyticOutputDataDTO;
import com.apm.datarw.dto.ValidateAnalyticRequestDTO;
import com.apm.datarw.entity.AnalyticAlarmEntity;
import com.apm.datarw.repo.IAnalyticAlarmRepo;
import com.apm.datarw.repo.IAnalyticResultStateRepo;
import com.apm.datarw.repo.IAnalyticRunStatus;
import com.apm.datarw.service.AnalyticInsertionImpl;
import com.apm.datarw.service.ExecutionDataStorageImpl;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;

@CrossOrigin(origins = "http://localhost:5010")
@RestController
public class AnalyticExecutionController<T> {
	
	private static final Logger log = LoggerFactory.getLogger(AnalyticExecutionController.class);

	@Autowired
	ExecutionDataStorageImpl dataStore;

	@Autowired
	IAnalyticRunStatus runStatus;
	
	@Autowired
	IAnalyticResultStateRepo stateRepo;
	
	@Autowired
	IAnalyticAlarmRepo alarmRepo;
	
	@Autowired
	AnalyticInsertionImpl analyticInsertionImpl;
	
	@RequestMapping(value="/test",method=RequestMethod.GET)
	public String test(){
		AnalyticAlarmEntity alarm = new AnalyticAlarmEntity();
		alarm.setAlarmTimestamp(new Date());
		alarm.setAlarmName("ALARM_NAME");
		alarmRepo.saveAndFlush(alarm);
		return "lol";
	}

	@RequestMapping(value="/storeData",method=RequestMethod.POST)
	public DeferredResult<ResponseEntity<?>> storeAnalyticData(@RequestBody(required=false) AnalyticOutputDataDTO analyticOutput, BindingResult bindingResult){
		DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<ResponseEntity<?>>();
		if(analyticOutput.getError()==null) {
			if(!bindingResult.hasErrors()){
				if(analyticOutput.isValidationRequest()==false) {
				log.info("in storeData block\n");	
				try {
				dataStore.storeOutputData(analyticOutput);
				deferredResult.setResult(new ResponseEntity<String>("{\"success\":true, \"message\":\"Analytic output results write queued.\"}", HttpStatus.OK));
				}
				catch (Exception e) {
					try{
						dataStore.setIsRunningAsFalseByJobId(analyticOutput.getAnalyticId(), analyticOutput.getJobId());
						log.info("IsRunning for analytic with jobId= "+analyticOutput.getJobId() +" is set to false");
					}catch (Exception ex) {
						log.error(ex.getMessage());
					}
				}
				}
			}
		}else{
			deferredResult.setResult(new ResponseEntity<String>("{\"failure\":true, \"error\":\"Analytic output results absent.\"}", HttpStatus.OK));
			try{
				dataStore.setIsRunningAsFalseByJobId(analyticOutput.getAnalyticId(), analyticOutput.getJobId());
				log.info("IsRunning for analytic with jobId= "+analyticOutput.getJobId() +" is set to false");
			}catch (Exception e) {
				log.error(e.getMessage());
			}
			log.error("Analytic with id = "+analyticOutput.getAnalyticId() +" is failed");
			}
		return deferredResult;
	}
	
	@RequestMapping(value="/validateAnalytic",method=RequestMethod.POST)
	public DeferredResult<ResponseEntity<?>> validateAnalytic(
			@RequestParam("analyticPackage") MultipartFile analyticPackage,
			@RequestParam("inputData") MultipartFile inputData,
			@RequestParam("config") String configString
			) throws JsonParseException, JsonMappingException, IOException, JSONException{
		
		ValidateAnalyticRequestDTO validateAnalyticRequestDTO = new Gson().fromJson(configString, ValidateAnalyticRequestDTO.class);
		DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<ResponseEntity<?>>();
		org.json.JSONObject response = analyticInsertionImpl.storeArtifact(analyticPackage, validateAnalyticRequestDTO.getAnalytic_name(), validateAnalyticRequestDTO.getAnalytic_version());
		if(response.has("failure"))
			deferredResult.setResult(new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST));
		else {
			JSONObject jsonObject = dataStore.validateAnalytic(validateAnalyticRequestDTO,inputData);
			deferredResult.setResult(new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK));
			
		}
		return deferredResult;
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/storeValidateData",method=RequestMethod.POST)
	public DeferredResult<ResponseEntity<String>> storeValidateData(@RequestBody(required=false) AnalyticOutputDataDTO analyticOutput, BindingResult bindingResult){
		DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<ResponseEntity<String>>();
		if(analyticOutput.getError()==null) {
		if(!bindingResult.hasErrors()){
			if(analyticOutput.isValidationRequest()==true) {
				 ObjectMapper mapper = new ObjectMapper();
				 mapper.enable(SerializationFeature.INDENT_OUTPUT);
				 try {
					mapper.writeValue(new File(analyticOutput.getJobId()+".json"), analyticOutput);
					deferredResult.setResult(new ResponseEntity<String>( "{\"success\":\"Analytic output data is written in "+ analyticOutput.getJobId()+".json file\"}", HttpStatus.OK ));
				} catch (IOException e) {
					log.error(e.getMessage());
				}				    
				log.info("Analytic with id = "+analyticOutput.getAnalyticId() +" is executed successfully");
			}
			}
		}else{
			deferredResult.setResult(new ResponseEntity<String>("{\"failure\":true, \"error\":\"Analytic output results absent.\"}", HttpStatus.OK));
			 ObjectMapper mapper = new ObjectMapper();
			 mapper.enable(SerializationFeature.INDENT_OUTPUT);
			 try {
				mapper.writeValue(new File(analyticOutput.getJobId()+".json"), analyticOutput);
				deferredResult.setResult(new ResponseEntity<String>("{\"failure\":true, \"error\":\"Analytic output results absent.\"}", HttpStatus.OK));
			} catch (IOException e) {
				e.printStackTrace();
			}				    
			log.info("Analytic with id = "+analyticOutput.getAnalyticId() +" is failed");
		}
		return deferredResult;
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/getValReqOutputData",method=RequestMethod.GET)
	public org.json.simple.JSONObject getValReqOutputData(@RequestParam String jobId) throws ParseException{
	
		org.json.simple.JSONObject outputData = new org.json.simple.JSONObject();
		try {
			if(!Objects.isNull(jobId)) {
				outputData = dataStore.getValReqOutputData(jobId);
				if(outputData.containsKey("error")) {
					outputData.put("error", "Analytic output data not found");
				}
			}	
			else {
				outputData.put("error", "jobId can not empty");
			}
		}
		catch (Exception e) {
			outputData.put("error", "Analytic output data not found");
		}
		return outputData;
	}
	
}
