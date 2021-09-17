package twitter;

import java.util.ArrayList;
import java.util.List;

public class HttpWordCount {

    List<String> label;  //hashtag
    List<String> data;  // counts of hashtags

    public HttpWordCount(List<String> tags_list, List<String> counts_list){
        label=tags_list;
        data= counts_list ;
    }
}
