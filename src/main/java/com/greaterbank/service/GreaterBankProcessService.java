package com.greaterbank.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GreaterBankProcessService{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Value("${csv.file.location.pending}")
	private String directorypath;
	
	@Value("${csv.file.location.processed}")
	private String directorypathProcessed;
	
	@Value("${csv.file.location.reports}")
	private String reportFilePath;
	
	@Value("${csv.file.location.report.name}")
	private String reportFileName;
	
	private String cronExpression = "";
	private String filePath;
	private String fileExtenstion;
	private String fileName;
	
	public void initSetUp(){		
		
	}
	
	/**********************************************************************
	 * Code to read the csv file and create a cron expression, although the
	 * dynamically generated cron expression isnt used **
	 *********************************************************************/
	public String parseForCsvFiles(String parentDirectory){
        File[] filesInDirectory = new File(parentDirectory).listFiles();
        Integer hour = 0;
        Integer minute = 0;
        for(File f : filesInDirectory){
            if(f.isDirectory()){
                parseForCsvFiles(f.getAbsolutePath());
            }
            filePath = f.getAbsolutePath();
            fileExtenstion = filePath.substring(filePath.lastIndexOf(".") + 1,filePath.length());
            fileName = f.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
            String[] fileParts = fileName.split("_");
            String datetime = fileParts[3];
            String time = String.format("%1$tH:%1$tM:%1$tS", Long.parseLong(datetime));
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            try {
				date = sdf.parse(time);
				Calendar calendar = GregorianCalendar.getInstance();
				calendar.setTime(date);
				hour = calendar.get(Calendar.HOUR);
				minute = calendar.get(Calendar.MINUTE);
				minute = minute + 10;
			} catch (ParseException e) {				
				e.printStackTrace();
			}
            if("csv".equals(fileExtenstion)){
                System.out.println("CSV file found -> " + filePath + System.currentTimeMillis());
                cronExpression = "0" +" " +hour.toString()+" "+ minute.toString()+" " +"*"+" "+"*"+" "+"*";
            }else{
            	cronExpression = "0,30 * * * * *";
            }
        }
        return cronExpression;
    }	
	
	/*************************************************************************
	 * A job is scheduled to run every minute. It checks for a csv in the
	 * pending folder. if there is a file, it executes a report, else it waits
	 * for a new file. **
	 *************************************************************************/
	@Scheduled(cron = "0 0/1 * * * *")
	public void cronJob() {
		logger.info("> cronJob started");
		cronExpression = parseForCsvFiles(directorypath);
		if(!(cronExpression.equals(""))){
			String line = "";
			String cvsSplitBy = ",";
			BigDecimal totalCreditAmt = new BigDecimal(0.0);/** total credit amount **/
			BigDecimal totalDebitAmt = new BigDecimal(0.0);/** total debit amount **/
			Long totalAcctsProcessed = 0L;/** total accounts in the csv **/
			Long totalAcctsSkipped = 0L;/** total accounts skipped **/
			try {
				BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
				try {
					while ((line = bufferedReader.readLine()) != null) {
						String[] accounts = line.split(cvsSplitBy);
						boolean isNumeric = accounts[0].chars().allMatch( Character::isDigit );
						if(isNumeric){						
							Double amount = Double.parseDouble(accounts[1]);
							if(amount < 0){
								totalDebitAmt = totalDebitAmt.add(new BigDecimal(amount * (-1)));
							}else{
								totalCreditAmt = totalCreditAmt.add(new BigDecimal(amount));
							}
							totalAcctsProcessed = totalAcctsProcessed + 1;
						}else{
							totalAcctsProcessed = totalAcctsProcessed + 1;
							totalAcctsSkipped = totalAcctsSkipped + 1;
						}
					}
					bufferedReader.close();
					File fileDir = new File(reportFilePath);				
					File reportFile = new File(fileDir.getAbsolutePath(),reportFileName+"_"+System.currentTimeMillis()+".txt");
					File[] filesInDirectory = new File(directorypath).listFiles();
					for(File f : filesInDirectory){
						Files.copy(f.toPath(), (new File(directorypathProcessed+ "/" + f.getName())).toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					Arrays.stream(new File(directorypath).listFiles()).forEach(File::delete);
					
					if(!reportFile.exists()){
			             try {
			            	 reportFile.createNewFile();
			            	 FileOutputStream fos = new FileOutputStream(reportFile);
			            	 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
			            	 writer.write("File Processed: " + fileName);
			            	 writer.newLine();
			            	 writer.write("Total Accounts: " + totalAcctsProcessed.toString());
			            	 writer.newLine();
			            	 writer.write("Total Credits: " + totalCreditAmt.toString());
			            	 writer.newLine();
			            	 writer.write("Total Debits: " + totalDebitAmt.toString());
			            	 writer.newLine();
			            	 writer.write("Skipped Transactions: " + totalAcctsSkipped.toString());
			            	 writer.close();
			            } catch (IOException e) {
			                e.printStackTrace();
			            }
			        }
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			logger.info("< cronJob completed");
			cronExpression = "";
		}else{
			logger.info("< cronJob waiting for file");
		}
	}
}
