package com.gigmed.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import javassist.bytecode.stackmap.TypeData.ClassName;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gigmed.riskengine.common.C;
import com.gigmed.riskengine.common.Util;
import com.gigmed.riskengine.dao.Anomalyresult;
import com.gigmed.riskengine.repository.RiskEngineRepository;
import com.google.gson.Gson;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;



@Path("/")
public class RestAPI {
	private static final Logger log = Logger.getLogger( ClassName.class.getName() );
	
	private static final String RISK_ENGINE = "riskEngineRepository";
	private static final String APPLICATION_CONFIG_XML = "application-config.xml";
	private RiskEngineRepository engine = new RiskEngineRepository();

	@POST
	@Path("/insertRecord")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertRecord(@FormParam("datasetID") String datasetID, @FormParam("diagnosisCode") String diagnosisCode, @FormParam("drugName") String drugName, @FormParam("returnIsAnomaly") boolean returnIsAnomaly) {
		Anomalyresult result = engine.insertRecord(datasetID, diagnosisCode, drugName, returnIsAnomaly, C.DIAGNOSIS_CODE_TYPE_ICD9_GENERALIZED);

		// return HTTP response 200 in case of success
		return Response.status(200).entity(new Gson().toJson(result)).build();
	}
	
	@POST
	@Path("/getPrescriptionRisk")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPrescriptionRisk(@FormParam("datasetID") String datasetID, @FormParam("diagnosisCode") String diagnosisCode, @FormParam("drugName") String drugName) {
		Anomalyresult result = engine.checkAnomaly(datasetID, diagnosisCode, drugName);

		// return HTTP response 200 in case of success
		return Response.status(200).entity(new Gson().toJson(result)).build();
	}
	
	@POST
	@Path("/getAnomaliesByCommercialDrugName")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAnomaliesByCommercialDrugName(@FormParam("datasetID") String datasetID) {
		ArrayList<Anomalyresult> anomalies = engine.getAnomaliesByCommercialDrugName(datasetID,true);

		// return HTTP response 200 in case of success
		return Response.status(200).entity(new Gson().toJson(anomalies)).build();
	}
	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("datasetID") String datasetID,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
//		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(APPLICATION_CONFIG_XML);
//
//		engine =(RiskEngineRepository)applicationContext.getBean(RISK_ENGINE); 
					
		engine.loadCache();
		
		datasetID = fileDetail.getFileName();
				
		long startTime = System.currentTimeMillis();
		boolean result = engine.importInputDatasetCSVFromInputStream(datasetID,uploadedInputStream);
		long timeTakenToImportFile = System.currentTimeMillis()-startTime;
		log.info("timeTakenToImportFile="+timeTakenToImportFile/1000);
		
		startTime = System.currentTimeMillis();
		result = engine.runCalculationOnDBWithSpecificDataSetID(datasetID);
		long timeTakenToCalculateData = System.currentTimeMillis()-startTime;
		log.info("timeTakenToCalculateData="+timeTakenToCalculateData/1000);
		
		startTime = System.currentTimeMillis();
		Response response = getAnomaliesByCommercialDrugName(datasetID);
		long timeTakenToGetAnomalies = System.currentTimeMillis()-startTime;
		log.info("timeTakenToGetAnomalies="+timeTakenToGetAnomalies/1000);
		
		engine.saveCache();
		engine.saveToDB(datasetID);
		
		try {
			Util.sendEmail("ari@volcoff.com", "upload completed - dataset ID "+datasetID, "upload completed - dataset ID "+datasetID );
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return response;
	}
	
	@POST
	@Path("/runCalculation")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response runCalculation(@FormParam("datasetID") String datasetID) {
		
		boolean result = engine.runCalculationOnDBWithSpecificDataSetID(datasetID);		

		return Response.status(200).entity(new Gson().toJson(result)).build();

	}
}