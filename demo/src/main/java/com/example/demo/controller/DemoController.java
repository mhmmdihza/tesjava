package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Login;
import com.example.demo.repo.LoginRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;



@RestController
@RequestMapping("/")
public class DemoController{
	
	AsyncHttpClient client = Dsl.asyncHttpClient();
	final String BaseClientUrl = "http://dev3.dansmultipro.co.id/api/recruitment/";
	
	@Autowired
	LoginRepo loginRepo;
	
	@PostMapping(value="login")
	public ResponseEntity<Login> login(@RequestBody Login login) {
		Login result = loginRepo.findByUsernameAndPassword(login.getUsername(), login.getPassword());
		if(result==null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<>(HttpStatus.OK).ok(result);
	}
	
	@GetMapping(value="getjobdetailbyid/{id}")
	public ResponseEntity<String> GetJobDetailById(@PathVariable("id") String id) throws InterruptedException, ExecutionException {
		BoundRequestBuilder getRequest = client.prepareGet(BaseClientUrl+"/positions/"+id);
		ListenableFuture<Response> responseFuture = getRequest.execute();
		return new ResponseEntity<>(HttpStatus.valueOf(responseFuture.get().getStatusCode())).ok(responseFuture.get().getResponseBody());
	}
	@GetMapping(value="getjoblist")
	public ResponseEntity<String> GetJobList() throws InterruptedException, ExecutionException {
		BoundRequestBuilder getRequest = client.prepareGet(BaseClientUrl+"/positions.json");
		ListenableFuture<Response> responseFuture = getRequest.execute();
		return new ResponseEntity<>(HttpStatus.valueOf(responseFuture.get().getStatusCode())).ok(responseFuture.get().getResponseBody());
	}
	@GetMapping(value = "/getjobCSV", produces = "text/csv")
	public ResponseEntity<Resource> exportCSV() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		String responseBody = this.GetJobList().getBody();
		TypeReference<List<HashMap<String,String>>> typeRef 
        = new TypeReference<List<HashMap<String,String>>>() {};

        List<HashMap<String,String>> mapping = new ObjectMapper().readValue(responseBody, typeRef); 
		List<String> csvHeaderLs = new ArrayList<String>();
		List<List<String>> csvBody = new ArrayList<>();
		this.fillCsv(mapping, csvHeaderLs, csvBody);
		String[] csvHeader = csvHeaderLs.toArray(new String[0]);
	    ByteArrayInputStream byteArrayOutputStream;
	    try (
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            CSVPrinter csvPrinter = new CSVPrinter(
	                    new PrintWriter(out),
	                    CSVFormat.DEFAULT.withHeader(csvHeader)
	            );
	    ) {
	        for (List<String> record : csvBody)
	            csvPrinter.printRecord(record);
	        csvPrinter.flush();
	        byteArrayOutputStream = new ByteArrayInputStream(out.toByteArray());
	    } catch (IOException e) {
	        throw new RuntimeException(e.getMessage());
	    }
	    InputStreamResource fileInputStream = new InputStreamResource(byteArrayOutputStream);
	    String csvFileName = "response.csv";
	    HttpHeaders headers = new HttpHeaders();
	    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + csvFileName);
	    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");

	    return new ResponseEntity<>(
	            fileInputStream,
	            headers,
	            HttpStatus.OK
	    );
	}
	private void fillCsv(List<HashMap<String, String>> res,List<String> header,List<List<String>> val) {
		for ( String key : res.get(0).keySet() ) {
		    header.add(key);
		}
		List<String> row = new ArrayList<String>();
		for(int x =0;x<res.size();x++ ) {
			row = new ArrayList<String>();
			for (Entry<String, String> entry : res.get(x).entrySet()) {
				  String value = entry.getValue();
				  row.add(value);
			}
			val.add(row);
		}
	}
}