package edu.ucsb.cs.course_support;

import java.util.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import com.amazonaws.services.s3.event.S3EventNotification.*;
import com.amazonaws.services.rekognition.*;
import com.amazonaws.services.rekognition.model.*;

public class LamRekog {

    LambdaLogger logger = null;
    public void handler(S3Event event, Context context) {
	/* Invokes the function in Lambda 
           (triggered by an S3 update (source event)) 
         */
        try{ 
            logger = context.getLogger(); 
    
            for (S3EventNotificationRecord record : event.getRecords()) {
                String s3Key = record.getS3().getObject().getKey();
                String s3Bucket = record.getS3().getBucket().getName();
                detectLabels(s3Bucket, s3Key);
            }         
    
        } catch (Exception e) {
            logger.log("Error: " + e.getMessage());
            throw new RuntimeException (e);
        }

    }

    void detectLabels(String bkt, String fname) {
        /*request labels from image file named filename in S3 bucket bkt
          use 90% confidence limit and top 10 labels
         */

        //setup the AWS credentials (from ~/.aws/credentials)
        AWSCredentials credentials = new ProfileCredentialsProvider("racelab").getCredentials();
        //create a client connected to Rekognition service
        AmazonRekognition client = new AmazonRekognitionClient(credentials).withRegion(RegionUtils.getRegion("us-west-2"));

        //pass in the bucket (bkt) and file name (fname) to Rekognition
        //which returns the labels and confidence for lables with 
	//confidence higher than 90%
        DetectLabelsRequest request = new DetectLabelsRequest()
            .withImage(new Image().withS3Object(new S3Object()
            .withBucket(bkt).withName(fname)))
            .withMaxLabels(10).withMinConfidence(90f);
        DetectLabelsResult response = client.detectLabels(request);

        // append the return values to a Java list 
	int labelsCount = 0;
        List<String> labels = new ArrayList<>();
	for (Label label : response.getLabels()) {
            String str = new String(label.getName() + ":" + label.getConfidence());
 	    labels.add(str);
	    labelsCount++;
    	}
        //Print out the label list 
        if (logger != null) {
          logger.log("Total number of labels : " + labelsCount);
          logger.log("labels : " + labels);
        } else {
	    System.err.println("count: "+labelsCount + " labels: "+labels);
        }
    }

    public static void main(String[] args){
	/* Invokes the function from main for local testing */
        LamRekog obj = new LamRekog();
        try{ 
            String s3Key = "cjkltestfolder/badger1.jpg";
            String s3Bucket = "cjkltestbkt";
            obj.detectLabels(s3Bucket, s3Key);
        } catch (Exception e) {
            if (obj != null && obj.logger != null) {
		obj.logger.log("Error: " + e.getMessage());
	    } else {
		System.err.println("Error: " + e.getMessage());
            }
            throw new RuntimeException (e);
        }

    }
}
