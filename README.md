# Twitter-Streams-App

This project was created in order to find twitter's most interesting feature of a specific location. Specifically, I will take data from twitter API for a specific location and I will analyze them via apache spark streaming. Then I ' will create an app using the flask and frontend tools(like html-css-chart.js) so that the user can see the most famous twitts. 

Here is a digram shows exactly what I will implement. 
![diagramAPP](https://user-images.githubusercontent.com/89423777/133858926-04f02769-47e7-4ade-92a3-d2d287edb711.png)

## Execute it !
```bat
#Receive tweets
python TwitterHttpClient\twitter_app.py
#Open DashBoard
python HashtagsDashboard\app.py
#Start spark streaming
java -jar twitter-spark.jar
```
