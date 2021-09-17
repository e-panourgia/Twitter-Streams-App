import socket
import sys
import requests
import requests_oauthlib
import json

# Replace the values below with yours
ACCESS_TOKEN = ''
ACCESS_SECRET = ''
CONSUMER_KEY = ''
CONSUMER_SECRET = ''
my_auth = requests_oauthlib.OAuth1(CONSUMER_KEY, CONSUMER_SECRET,ACCESS_TOKEN, ACCESS_SECRET)


def send_tweets_to_spark(http_resp, tcp_connection):
    for line in http_resp.iter_lines():
        try:
            #Twitter response format is json
            full_tweet = json.loads(line)
            #Isolate only the text of each tweet
            tweet_text = str(full_tweet['text'])
            tcp_connection.send((tweet_text + '\n').encode("utf-8"))
        except:
            print("Error sending tweet to apache spark")


def get_tweets():
    url = 'https://stream.twitter.com/1.1/statuses/filter.json'
    #https://stream.twitter.com/1.1/statuses/filter.json?&language=en&locations=-130,-20,100,50
    query_data = [('language', 'en'), ('locations', '-130,-20,100,50'),('track','#')]
    #query_data = [('track', '#')] #this location value is San Francisco & NYC
    query_url = url + '?' + '&'.join([str(t[0]) + '=' + str(t[1]) for t in query_data])
    # Create HTTP GET request to twitter API in order to get stream of tweets
    response = requests.get(query_url, auth=my_auth, stream=True)
    print(query_url, response)
    return response

# Init Socket to communicate with apache spark
TCP_IP = "localhost"
TCP_PORT = 9009
conn = None
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind((TCP_IP, TCP_PORT))
s.listen(1)
print("Waiting for TCP connection...")
conn, addr = s.accept()
print("Connected... Starting getting tweets.")
resp = get_tweets()
send_tweets_to_spark(resp,conn)



