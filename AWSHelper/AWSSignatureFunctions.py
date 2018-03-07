import hashlib
import hmac
import base64


def sign(key, msg):
    v = hmac.new(key, msg.encode('utf-8'), hashlib.sha256)
    return v.digest(), v.hexdigest()


def getsignaturekey(key, date_stamp, region_name, service_name):
    sig_date, date_hex = sign(('AWS4' + key).encode('utf-8'), date_stamp)
    sig_region, region_hex = sign(sig_date, region_name)
    sig_service, service_hex = sign(sig_region, service_name)
    sig_digest, sig_hex = sign(sig_service, 'aws4_request')
    return sig_digest, sig_hex


def getencodedpolicy(expiration, bucket, key_name_starts_with, redirect, credential, algorithm, amz_date):
    policy = ''
    if len(key_name_starts_with) == 0:
        keynamestartswith = '/'
    elif not key_name_starts_with[0] == '/':
        keynamestartswith = '/'
    try:
        with open("POSTPolicyTemplate.json", "r") as myfile:
            policy_str = myfile.read()
            policy_str = policy_str.replace('<cse546-expiration>', expiration).replace('<cse546-bucket>', bucket)\
                .replace('<cse546-key-name-conditions>', key_name_starts_with).replace('<cse546-redirect-url>', redirect)\
                .replace('<cse546-amz-credential>', credential).replace('<cse546-amz-algorithm>', algorithm)\
                .replace('<cse546-amz-date>', amz_date)
            policy = base64.b64encode(bytes(policy_str.encode('utf-8')))
    except Exception as e:
        print 'ERROR : ', e.message
        policy = ''
    finally:
        return policy
