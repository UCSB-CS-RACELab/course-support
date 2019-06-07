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

    /* Use a logger for multi-line CloudWatch logging when in AWS Lambda.
     * If there is none, we  will default to System.err.println
     */
    LambdaLogger logger = null;

    /* Choose a Region in which to execute the Rekognition service
     * see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/regions/Regions.html
     */
    final Regions preferred_region = Regions.US_WEST_2;

    //Entry point for AWS Lambda invocation
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
            if (logger != null) {
                logger.log("Error: " + e.getMessage());
                logger.log(e.toString());
	    } else {
                System.err.println("Error: " + e.getMessage());
                System.err.println(e);
            }
        }

    }

    void detectLabels(String bkt, String fname) {
        /* Request labels from image file named filename in S3 bucket bkt
         * use 90% confidence limit and top 10 labels.
         * This method is invoked by main (local/test) 
   	 * and by the handler (AWS Lambda).
         */

        /* Create a client connected to Rekognition service.
	 * Your credentials MUST be set in enviroment variables where
	 * the program runs.  The variable names are AWS_ACCESS_KEY_ID
         * and AWS_SECRET_ACCESS_KEY.  These are set for you in 
	 * AWS Lambda, but if you run this locally, you need to set them.
         * export VARNAME=variable_value or use this on Windows:
         * https://stackoverflow.com/questions/44272416/how-to-add-a-folder-to-path-environment-variable-in-windows-10-with-screensho
         */
        AmazonRekognition client = AmazonRekognitionClientBuilder.standard().withRegion(preferred_region).build();

        //pass in the bucket (bkt) and file name (fname) to Rekognition.
        //Doing so returns the labels and confidence for lables with 
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

    //Entry point for command line invocation (local testing)
    public static void main(String[] args){

	//create an instance of the encapsulating class
        LamRekog obj = new LamRekog();

	//set the name of the file and the bucket in S3 for Rekognition
        String s3Key = "cjkltestfolder/badger1.jpg";
        String s3Bucket = "cjkltestbkt";

        try{ 
            obj.detectLabels(s3Bucket, s3Key);
        } catch (Exception e) {
            if (obj != null && obj.logger != null) {
		obj.logger.log("Error: " + e.getMessage());
	    } else {
		System.err.println("Error: " + e.getMessage());
            }
        }

    }
}
