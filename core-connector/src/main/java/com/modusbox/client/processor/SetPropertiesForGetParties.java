package com.modusbox.client.processor;

import com.modusbox.client.model.FulfilNotification;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetPropertiesForGetParties implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        //ArrayList<Object> ary = (ArrayList<Object>) exchange.getIn().getBody();
        // HashMap<String,Object>  bodymap = (HashMap<String, Object>) exchange.getIn().getBody();
        // String lastName = (String) bodymap.get("lastName");
        String body = exchange.getIn().getBody(String.class);
        JSONObject respObject = new JSONObject(body);
        // String str =body.substring(1,body.length()-2);
        // JSONObject respObject = new JSONObject(str);
        String lastName = respObject.getString("lastName");
        String branchName = respObject.getString("branchName");
        Integer entityId = respObject.getInt("entityId");
        // String lastName = (String) ary.get(0);
        exchange.setProperty("lastName", lastName);
        exchange.setProperty("branchName", branchName);
        exchange.setProperty("entityId", entityId);
    }
}
