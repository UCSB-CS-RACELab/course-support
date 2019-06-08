package edu.ucsb.cs.course_support;

import com.amazonaws.regions.*;
import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import org.json.simple.*;

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
            //print out the event to the log to see its structure
            logger.log("LamSimple event: " + event.toString());
        } else {
            System.err.println("LamSimple (invoked internally) event: " + event.toString());
        }

        //prepare an object to return
        JSONObject retn = new JSONObject();
        retn.put("LamSimple:handler", event.keySet());
        return retn;

    }
    //Entry point for command line invocation (local testing)
    public static void main(String[] args){

        System.err.println("Entry LamSimple:Main");
	//create an instance of the encapsulating class
        LamSimple obj = new LamSimple();

	//create an argument for the handler
        JSONObject handler_args = new JSONObject();
        String val = "no_args";
        if (args.length > 1){
            val = args[0];
        }
        handler_args.put("LamSimple:Main", val);

        //invoke the handler directly
        System.err.println("LamSimple:Main calling handler");
        JSONObject retn = obj.handler(handler_args,null);
        System.err.println("LamSimple:Main handler returned: "+retn);

    }
}
