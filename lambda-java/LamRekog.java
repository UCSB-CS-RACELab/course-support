package edu.ucsb.cs.course_support;

import java.util.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.lambda.*;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import com.amazonaws.services.s3.event.*;
import com.amazonaws.services.s3.event.S3EventNotification.*;
import com.amazonaws.services.rekognition.*;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.*; //for AmazonServiceException
import org.json.simple.*;


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
    public JSONObject handler(JSONObject event, Context context) {
	/* AWS Lambda invokes this function, e.g. in response to 
           (triggered by 
         * an S3 update (source event) or a cli invocation:
         * aws lambda invoke ...
         */
        JSONObject retn = new JSONObject();

        //double check that the input objects are not null!
        if (context == null || event == null) {
            String err = "LamRekog:handler: context ("+context+
                ") or event ("+event+") are null!";
            retn.put("ERROR", err);
            return retn;
        }

        try{ 
            logger = context.getLogger(); 
            //print out the event to the log to see its structure
            logger.log("Input event: " + event.toString());
    
            /* Invoked via aws lambda invoke ... with payload containing a 
             * key "eventSource", e.g. 
             * aws lambda invoke --invocation-type RequestResponse --function-name lambda-rekog --region us-west-2 --profile myprofile --payload '{"eventSource":"ext:invokeCLI","name":"bktname","key":"foldername/fname.jpg"}' outputfile
		--invocation-type can be RequestResponse (sync) or Event (async)
             */
            if (event.containsKey("eventSource")) { //passed in via invoke CLI
                String s3Bucket = (String) event.get("name");
                String s3Key = (String) event.get("key");
                retn = detectLabels(s3Bucket, s3Key);

            } else {
            /* Invoked by AWS lambda via an S3Event (all other event
	     * types will cause an exception).
             */
                S3EventNotification notif = S3EventNotification.parseJson(event.toString()); 
                for (S3EventNotificationRecord record : notif.getRecords()) {
                    String s3Key = record.getS3().getObject().getKey();
                    String s3Bucket = record.getS3().getBucket().getName();
                    logger.log("Rekognition args: " + s3Bucket + ", "+s3Key);
                    retn = detectLabels(s3Bucket, s3Key);
                }         
     	    } 

            retn.put("LamRekog:handler",invokeLambda());

        } catch (Exception e) {
            retn.put("ERROR", e.toString());
            if (logger != null) {
                logger.log("LamRekog:handler: " + e.toString());
	    } else {
                System.err.println("LamRekog:handler: " + e);
            }
        }
        return retn;

    }

    JSONObject detectLabels(String bkt, String fname) {
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

        //prepare a default return value
        JSONObject retn = new JSONObject();

        //pass in the bucket (bkt) and file name (fname) to Rekognition.
        //Doing so returns the labels and confidence for lables with 
	//confidence higher than 90%
        DetectLabelsResult response = null;
        boolean ERROR = false;
        String err = "unknown error";
        try { //always catch exceptions from remote API calls!
            DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(new Image().withS3Object(new S3Object()
                .withBucket(bkt).withName(fname)))
                .withMaxLabels(10).withMinConfidence(90f);
            response = client.detectLabels(request);
        } catch (InvalidS3ObjectException e) {
            err = "Rekognition Cannot access S3 Object";
            ERROR = true;
        } catch (AmazonServiceException e) {
            err = "Unauthorized.  Are your credentials set in the environment variables?";
            ERROR = true;
        } catch (Exception e) {
            err = e.getMessage();
            ERROR = true;
        }
        if (ERROR || response == null) { //handle all error cases
            retn.put("ERROR", err);
            System.err.println("LamRekog:handler: "+ err);
            return retn;
        }

        // append the return values to a Java Map 
        Map<String, Float> lmap = new HashMap<>();
	for (Label label : response.getLabels()) {
            lmap.put(label.getName(),label.getConfidence());
    	}
	int labelsCount = lmap.size();

        //Convert Java Map to a JSONObject
        retn = new JSONObject(lmap);

        //Print out the label list 
        if (logger != null) {
          logger.log("Total number of labels : " + labelsCount);
          logger.log("labels : " + retn);
        } else {
	    System.err.println("Rekognition output: count: "+labelsCount + " labels: "+retn);
        }
        return retn;
    }
	    
    //This method invokes another AWS Lambda function
    JSONObject invokeLambda() {

        //setup the return object
        JSONObject retn = new JSONObject();

	/* Add your code here: 
	 * 1) setup client with environment credentials 
	 * 2) setup InvokeRequest with payload appropriate for function handler
	 * 3) invoke lambda-simple (or other named) Lambda function synchronously
	 * 4) get response and add it to retn using key LamRekog:invokeLambda
	 * 5) return retn to caller (done below)
         */

         return retn;
    }

    //Entry point for command line invocation (local testing)
    public static void main(String[] args){

	//create an instance of the encapsulating class
        LamRekog obj = new LamRekog();

	//set the name of the file and the bucket in S3 for Rekognition
        String s3Key = "cjkltestfolder/badger1.jpg";
        String s3Bucket = "cjkltestbkt";
        JSONObject retn = new JSONObject();
        try{ 
	    /* Call rekognition directly passing in an S3 bucket and filename
	     * filename must be at the top level or have a folder followed
	     * by a slash as in the s3Key variable above.
	     */
            retn = obj.detectLabels(s3Bucket, s3Key);
        } catch (Exception e) {
	    System.err.println("Error in main: " + e.getMessage());
        }
        System.err.println("\nReturning from main: "+retn);

    }
}
