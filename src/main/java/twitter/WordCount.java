package twitter;

import com.google.gson.Gson;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Serializable;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.impl.client.HttpClientBuilder;
import twitter.HttpWordCount;


public class WordCount implements Serializable {


    public static void main(String[] args) throws Exception {
        Logger.getLogger("org").setLevel(Level.OFF) ;
        Logger.getLogger("akka").setLevel(Level.OFF);

        SparkConf conf = new SparkConf().setMaster("local[*]")
                .setAppName("twitter.WordCount");

        /*
         This functon is called for each key (hashtag). Let's say that the
         hashtag #TikTok has count 5 in the window (i-1)th. Now, in the window i-th
         we receive 100 more #TikTok hashtags. Then, the following function will add
         5 (previous tweets) + 100 (new tweets) and it saves the new count in memory.
         */

        Function2<List<Long>, Optional<Long>, Optional<Long>>
                COMPUTE_RUNNING_SUM = (new_count, current_hashtag) -> {
                Long sum = current_hashtag.or(0L);
                for (Long i : new_count) {
                    sum += i;
                }
                return Optional.of(sum);
        };

        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));
        jssc.checkpoint("/tmp/log-analyzer-streaming");
        JavaReceiverInputDStream<String> lines = jssc.socketTextStream("127.0.0.1", 9009);

        JavaDStream<String> wordsOfTweet = lines.flatMap(tweet -> Arrays.asList(tweet.split(" ")).iterator());

        wordsOfTweet =  wordsOfTweet.filter(tweets -> tweets.contains("#"));

        JavaPairDStream<String, Long> pairs = wordsOfTweet.mapToPair(hashtag -> new Tuple2<String, Long>(hashtag, 1L));

        JavaPairDStream<String, Long> hashTagsCount = pairs.reduceByKey((hashtag1, hashtag2) -> hashtag1 + hashtag2);

        hashTagsCount = hashTagsCount.updateStateByKey(COMPUTE_RUNNING_SUM);

        hashTagsCount.foreachRDD(rdd -> {
            JavaRDD<Row> rows = rdd.map(data -> {
                return RowFactory.create(data._1, data._2);
            });
            StructType schema = new StructType(new StructField[]{
                    new StructField("tag", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("count", DataTypes.LongType, false, Metadata.empty())
            });
            Dataset<Row> documentDF = SQLContext.getOrCreate(rdd.context()).createDataFrame(rows, schema);
            documentDF.createOrReplaceTempView("twitter_tags");
            Dataset<Row> results = SQLContext.getOrCreate(rdd.context()).
                    sql("select tag, count from twitter_tags order by count desc limit 10");
            if (!results.isEmpty()) {
                results.show();
                List<Row> tags = results.select("tag").collectAsList();
                List<Row> counts = results.select("count").collectAsList();
                List<String> tags_list = new ArrayList<String>();
                List<String> counts_list = new ArrayList<String>();

                for (int i = 0; i < tags.size(); i++) {
                    tags_list.add(tags.get(i).getString(0));
                    counts_list.add(String.valueOf(counts.get(i).getLong(0)));
                    System.out.println(tags_list.get(i) + " " + counts_list.get(i));
                }
                HttpWordCount httpWordCount = new HttpWordCount(tags_list, counts_list);
                String json = new Gson().toJson(httpWordCount);
                System.out.println(json);
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();

                try {
                    HttpPost request = new HttpPost("http://localhost:5001/updateData");
                    StringEntity params = new StringEntity(json);
                    request.addHeader("content-type", "application/json");
                    request.setEntity(params);
                    httpClient.execute(request);
                    // handle response here...
                } catch (Exception ex) {
                    System.out.println("Failed **********");
                } finally {
                    httpClient.close();
                }
            }
        });

        hashTagsCount.print();
        jssc.start();
        jssc.awaitTermination();
    }



}