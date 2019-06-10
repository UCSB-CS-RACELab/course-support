package edu.ucsb.cs.course_support;

import java.util.*; //for Map
import com.amazonaws.regions.*;
import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class LamSimple {

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
         * a (source event).  The logger comes from the incoming context
         * object that the AWS Lambda platform passes in.
         *
         * This function can also be invoked (for testing) by main.
         * In this case, the context object is null (check it before using!).
         */
        if (context != null) {
            logger = context.getLogger(); 
            //print out event, context, and environment to the log 
            String envstr = "empty";
            if (event != null){
                envstr = event.toString();
            }
            //get the environment variables
            String envvars = "Environment Variables\n";
            Map<String, String> env = System.getenv();
            for (Map.Entry<String, String> entry : env.entrySet()) {
                envvars += (entry.getKey()+" : "+entry.getValue()+"\n");
            }

	    logger.log("LamSimple:handler: \n\tevent: "+envstr
	        + "\n\tcontext: "+context.toString()
	        + "\n"+envvars);
        } else {
            System.err.println("LamSimple (invoked internally) event: " + event.toString());
        }

        //convert event.keySet to JSONArray of Strings
        JSONArray jary = new JSONArray();
        for (Object k : event.keySet()) {
            String strele = (String)k;
            jary.add(strele);
        }
            
        //prepare an object to return
        JSONObject retn = new JSONObject();
        retn.put("statusCode", 200);
        retn.put("fname", "LamSimple:handler");
        retn.put("body", jary);
        System.err.println("LamSimple:handler returning: "+retn);
        return retn;

    }
    //Entry point for command line invocation (local testing)
    public static void main(String[] args){

        System.err.println("Entry LamSimple:Main");
	//create an instance of the encapsulating class
        LamSimple obj = new LamSimple();

	//create an argument for the handler
        String val = "{\"no_args\":\"no_args\"}";
        if (args.length >= 1){
            val = args[0];
        }
        JSONParser parser = new JSONParser();
        JSONObject handler_args = new JSONObject();
        try {
            handler_args = (JSONObject) parser.parse(val);
        } catch (Exception e) { 
            System.err.println(e);
        } //the initialization (empty) object will pass through

        //invoke the handler directly
        JSONObject retn = obj.handler(handler_args,null);
        System.err.println("LamSimple:main returning: "+retn);

    }
}
