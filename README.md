# Twitter-Streams-App

This project was created in order to find twitter's most interesting feature of a specific location. Specifically, I will take data from twitter API for a specific location and I will analyze them via apache spark streaming. Then I ' will create an app using the flask and frontend tools(like html-css-chart.js) so that the user can see the most famous twitts. 


## Execute it !
```bat
#Receive tweets
python TwitterHttpClient\twitter_app.py
#Open DashBoard
python HashtagsDashboard\app.py
#Start spark streaming
java -jar twitter-spark.jar
```
