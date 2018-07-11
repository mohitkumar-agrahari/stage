package com.apm.datarw.kairosdb;

import java.text.ParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MapAndAddDataToKairosDB {
	
	private static final Logger log=LoggerFactory.getLogger(MapAndAddDataToKairosDB.class);
	
	public String mapAndAddDataToKairosDB(JSONArray inputData, String analyticName) throws ParseException {
		log.info("mapAndAddDataToKairosDB method is called");
		String response = null;
		JSONArray returnList = new JSONArray();
		FutureTask<JSONObject> ftask = null;
		try {
			JSONObject jsonObj = inputData.getJSONObject(0);
		        Object[] keys = jsonObj.keySet().toArray();
		        for(int j = 0; j < keys.length; j++) {
		        	String keyValue = (String) keys[j];
		        	if(!(keyValue.equalsIgnoreCase("DateTime"))) {
		        		try {
		        		MapToTimeSeriesFormatImpl mapToTimeSeriesFormatUtil = new MapToTimeSeriesFormatImpl(keyValue,inputData,analyticName);
		        		ftask = new FutureTask<JSONObject>(mapToTimeSeriesFormatUtil);
		    			ExecutorService ser=Executors.newFixedThreadPool(1);
		    			ser.submit(ftask);
		    			JSONObject jsonObject = ftask.get();
		    			returnList.put(jsonObject);
		    			log.info("mapped for tag ::"+keyValue);
		    			ser.shutdown();
		        		}
		        		catch (Exception e) {
		        			e.printStackTrace();
						}
		        	}
		        }		   
		        	if(ftask.isDone()) {
		        		 try {
		        		AddDatapointsIntoKairosDBHelper addDatapointsIntoKairosDBUtil = new AddDatapointsIntoKairosDBHelper(returnList);
		        		FutureTask<String> futureTask = new FutureTask<String>(addDatapointsIntoKairosDBUtil);
		        		ExecutorService exeserv = Executors.newFixedThreadPool(1);
		        		exeserv.submit(futureTask);
		        		response = futureTask.get();
		        		exeserv.shutdown();
		        		} catch (Exception e) {
		 					e.printStackTrace();
		 				}
		        	}
		} catch (JSONException ex) {
		    ex.printStackTrace();
		    log.error("Error has occured::"+ex.getMessage());
		}
		
		return response;
	}	
}
