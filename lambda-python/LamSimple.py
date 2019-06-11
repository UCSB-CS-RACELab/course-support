#import boto3, json, argparse, time, requests, uuid, sys
import json, argparse, sys, os

import logging #more info: https://docs.aws.amazon.com/lambda/latest/dg/python-logging.html
logger = logging.getLogger()
logger.setLevel(logging.INFO) 

############## handler - Python Lambda entry point #############
def handler(event, context): #can be called directly or triggered by AWS

    #print out details on event, context, and the execution environment
    logger.info("LamSimple:handler: \n\tevent: {}\n\tcontext: {}\nEnvironment Variables: {}".format(event,context,os.environ))

    #setup argument checks (HTTP params) for when API Gateway is used
    params = {'params': 'none'}
    if 'queryStringParameters' in event:
        params = event['queryStringParameters']
        logger.info("LamSimple:handler: has params:\n\t{}".format(params))

    #get list of event keys as a string for return 
    keys = list(event.keys())
    
    retn = {} #python dictionary, must have statusCode and body keys
    retn['statusCode']=200 
    #body must be valid json, converted via json.dumps
    retn['body']=json.dumps({'name': 'LamSimple:handler', 'keys': keys, 'params': params})
    print('LamSimple:handler returning: {}'.format(retn)) #print example
    return retn

############## main - so we can call/test the handler directly ###############
def main():
    parser = argparse.ArgumentParser(description='LamSimple: a simple python script that can be invoked via AWS Lamba')
    parser.add_argument('--input',action='store',default='''{"no_args": "no_args"}''',help='''json key/value input values, e.g. '{"name": "Chandra", "year": 2019}', the default is '{"no_args":"no_args"}' ''')
    args = parser.parse_args()

    print('Entry LamSimple:Main')
    print(args.input)
    event = json.loads(args.input)
    context = None
    
    retn = handler(event,context)
    print('LamSimple:main returning: {}'.format(retn))

######## route python entry point to main() function ########
if __name__ == "__main__":
    main()

