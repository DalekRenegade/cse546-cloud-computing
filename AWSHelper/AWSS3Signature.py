import datetime
import hashlib
import hmac
import os
import sys
import urllib
import AWSSignatureFunctions as AWSSignature

access_key = os.environ.get('AWS_ACCESS_KEY_ID')
secret_key = os.environ.get('AWS_SECRET_ACCESS_KEY')
if access_key is None or secret_key is None:
    print 'No access key is available.'
    sys.exit()

utc = datetime.datetime.utcnow()
today = datetime.datetime(utc.year, utc.month, utc.day)
expiration = datetime.datetime(2018, 5, 5)
expiration_str = expiration.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
amz_date_time = today.strftime('%Y%m%dT%H%M%SZ')
date_stamp = today.strftime('%Y%m%d')
amz_expires = int(round((expiration - today).total_seconds()))

method = 'GET'
service = 's3'
bucket = 'dalek-test-bucket'
region = 'us-west-1'
algorithm = 'AWS4-HMAC-SHA256'
signed_headers = 'host'
redirect = 'https://s3-us-west-1.amazonaws.com/dalek-test-bucket/music_chords_in_the_key_of_a_b_c_d_e_f_g_flat_sharp_major.png'

canonical_uri = '/index.html'  # the file name to be shared/hosted

credential_scope = date_stamp + '/' + region + '/' + service + '/' + 'aws4_request'
credentials = access_key + '/' + credential_scope
credential_scope_encoded = urllib.quote(credentials.encode('utf8'), safe='')

canonical_querystring = 'X-Amz-Algorithm=' + algorithm \
                        + 'X-Amz-Credential=' + credential_scope_encoded \
                        + '&X-Amz-Date=' + amz_date_time \
                        + '&X-Amz-Expires=' + str(amz_expires) \
                        + '&X-Amz-SignedHeaders=' + signed_headers

canonical_headers = 'host:' + bucket + '.s3.amazonaws.com' + '\n'

canonical_request = method + '\n' + canonical_uri + '\n' \
                    + canonical_querystring + '\n' + canonical_headers + '\n' \
                    + signed_headers + '\n' + 'UNSIGNED-PAYLOAD'

string_to_sign = algorithm + '\n' + amz_date_time + '\n' \
                 + credential_scope + '\n' + hashlib.sha256(canonical_request).hexdigest()
# print '\n-------------------STRING 2 SIGN-------------------'
# print string_to_sign
# print '---------------------------------------------------'

signing_key, signing_key_hex = AWSSignature.getsignaturekey(secret_key, date_stamp, region, service)
# print '\n------------------SIGNING KEY HEX------------------'
# print signing_key_hex
# print '---------------------------------------------------'

# signature = hmac.new(signing_key, string_to_sign.encode('utf-8'), hashlib.sha256).hexdigest()
# print '\n---------------------SIGNATURE---------------------'
# print signature
# print '---------------------------------------------------'

base64policy = AWSSignature.getencodedpolicy(expiration_str, bucket, 'uploads/', redirect, credentials, algorithm, amz_date_time)
print '\n---------------BASE 64 POLICY STRING---------------'
print base64policy
print '---------------------------------------------------'

a = 'eyAiZXhwaXJhdGlvbiI6ICIyMDE4LTA1LTA1VDAwOjAwOjAwLjAwMFoiLA0KICAiY29uZGl0aW9ucyI6IFsNCiAgICB7ImJ1Y2tldCI6ICJkYWxlay10ZXN0LWJ1Y2tldCJ9LA0KICAgIFsic3RhcnRzLXdpdGgiLCAiJGtleSIsICJ1cGxvYWRzLyJdLA0KICAgIHsiYWNsIjogInB1YmxpYy1yZWFkIn0sDQogICAgeyJzdWNjZXNzX2FjdGlvbl9yZWRpcmVjdCI6ICJodHRwczovL3MzLXVzLXdlc3QtMS5hbWF6b25hd3MuY29tL2RhbGVrLXRlc3QtYnVja2V0L211c2ljX2Nob3Jkc19pbl90aGVfa2V5X29mX2FfYl9jX2RfZV9mX2dfZmxhdF9zaGFycF9tYWpvci5wbmcifSwNCiAgICBbInN0YXJ0cy13aXRoIiwgIiRDb250ZW50LVR5cGUiLCAiaW1hZ2UvIl0sDQogICAgeyJ4LWFtei1tZXRhLXV1aWQiOiAiMTQzNjUxMjM2NTEyNzQifSwNCiAgICB7IngtYW16LXNlcnZlci1zaWRlLWVuY3J5cHRpb24iOiAiQUVTMjU2In0sDQogICAgWyJzdGFydHMtd2l0aCIsICIkeC1hbXotbWV0YS10YWciLCAiIl0sDQoNCiAgICB7IngtYW16LWNyZWRlbnRpYWwiOiAiQUtJQUo3SlBEWkhLQkVGWjZBRUEvMjAxODAzMDgvdXMtd2VzdC0xL3MzL2F3czRfcmVxdWVzdCJ9LA0KICAgIHsieC1hbXotYWxnb3JpdGhtIjogIkFXUzQtSE1BQy1TSEEyNTYifSwNCiAgICB7IngtYW16LWRhdGUiOiAiMjAxODAzMDhUMDAwMDAwWiIgfQ0KICBdDQp9'

signature3 = hmac.new(signing_key, a.encode('utf-8'), hashlib.sha256).hexdigest()
print '\n-------------------S3 SIGNATURE--------------------'
print signature3
print '---------------------------------------------------'
