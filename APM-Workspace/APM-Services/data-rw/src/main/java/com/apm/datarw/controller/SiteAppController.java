package com.apm.datarw.controller;

import java.util.ArrayList;
import java.util.List;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import com.apm.datarw.dto.NewAnalyticDTO;
import com.apm.datarw.dto.PythonRequestDTO;
import com.apm.datarw.entity.AnalyticInfoEntity;
import com.apm.datarw.exceptions.ValidationException;
import com.apm.datarw.inf.IAnalyticDetailsImpl;
import com.apm.datarw.repo.IAnalyticInfoRepo;
import com.apm.datarw.service.AnalyticDetailsImpl;
import com.apm.datarw.service.AnalyticInsertionImpl;
import com.apm.datarw.utils.ExternalPropertyUtil;

//Objects.requireNonNull(obj, "obj must not be null");

@RestController			
public class SiteAppController {
	@Autowired
	IAnalyticDetailsImpl iAnalyticDetailsImpl;

	@Autowired
	AnalyticInsertionImpl analyticInsertionImpl;
	
	@Autowired
	IAnalyticInfoRepo iAnalyticInfoRepo;

	@RequestMapping(value="/fetchAnalyticConfig",method=RequestMethod.POST,consumes="application/json",produces="application/json",headers ="Accept=application/json")
	public PythonRequestDTO getAnalyticDetails(@RequestBody JSONObject jsonObject){
		return iAnalyticDetailsImpl.getAnalyticDetails(jsonObject);
	}

	private org.json.JSONObject setResponse(BindingResult bindingResult) throws JSONException{
		org.json.JSONObject result = new org.json.JSONObject();
		if (bindingResult.hasErrors()){
			result.put("errors", bindingResult.getAllErrors().stream().map(el -> {
				return el.getDefaultMessage();
			}).collect(Collectors.toList()));
			result.put("errorCondition", true);
		}
		return result;
	}

	private org.json.JSONObject setResponse(String errorMessage) throws JSONException{
		org.json.JSONObject result = new org.json.JSONObject();
		result.put("errors", new JSONArray(String.format("[\"%s\"]", errorMessage)));
		result.put("errorCondition", true);
		return result;
	}
	
	@RequestMapping(value="/addAnalytic",method=RequestMethod.POST)
	public DeferredResult<ResponseEntity<String>> addConfiguration(@RequestBody(required=false) NewAnalyticDTO analyticConfiguration, BindingResult bindingResult) throws Exception {
		String assetid="df3";
		List<String> assetList = new ArrayList<String>();
		assetList.add(assetid);
		analyticConfiguration.setAssetId(assetList);
		System.out.println("test :"+analyticConfiguration.getAssetId());
		DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();
		if (bindingResult.hasErrors()){
			deferredResult.setResult(new ResponseEntity<String>(this.setResponse(bindingResult).toString(), HttpStatus.OK));
		}else{
			try {
				deferredResult.setResult(new ResponseEntity<String>(analyticInsertionImpl.insertAnlayticData(analyticConfiguration).toString(), HttpStatus.OK));
			} catch (ValidationException e) {
				deferredResult.setResult(new ResponseEntity<String>(this.setResponse(e.getMessage()).toString(), HttpStatus.BAD_REQUEST));
			}
		}
		return deferredResult;
	}
	
	@RequestMapping(value="/storeArtifact",method=RequestMethod.POST)
	public DeferredResult<ResponseEntity<String>> addConfiguration(@RequestParam("analyticPackage") MultipartFile analyticPackage, @RequestParam("analyticName") String analyticName,
			@RequestParam("analyticVersion") String analyticVersion) throws JSONException{
		DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();
		org.json.JSONObject response = analyticInsertionImpl.storeArtifact(analyticPackage, analyticName, analyticVersion);
		if(response.has("failure"))
			deferredResult.setResult(new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST));
		else
			deferredResult.setResult(new ResponseEntity<String>(response.toString(), HttpStatus.OK));
		return deferredResult;
	}
}

