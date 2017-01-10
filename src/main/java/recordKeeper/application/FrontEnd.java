package recordKeeper.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
public class FrontEnd {
	private static HashMap<String,String> listCompanies = new HashMap<String,String>();
	public static MongoClient mongo = new MongoClient("localhost",27017);
	public static final String DB_NAME ="applications";
	public static MongoDatabase db;
	public static final String COLL_NAME = "companies";
	public static final String USAGE = "Read all Queries: /read \n"
			+ "Add a new Record: /add?name=foo&data=foo \n"
			+ "Check a Record by name: /check?name=foo \n"
			+ "Delete a Record by nmae: /delete?name=foo \n";
	public static void main(String args[]){
	initializeConnection();
	Undertow server = Undertow.builder()
            .addHttpListener(8080, "0.0.0.0")
            .setWorkerThreads(4)
            .setIoThreads(5)
            .setHandler(new HttpHandler() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public void handleRequest(final HttpServerExchange exchange) throws Exception { 
               	 exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");     
                 if (exchange.getQueryParameters().containsKey("check")) {
                     //System.out.println("In healthcheck");
                     exchange.getResponseSender().send("OK");
                     return;
                 }
                 else if(exchange.getRequestPath().equalsIgnoreCase("/usage")){
                	 exchange.getResponseSender().send(USAGE);
                 }
                 else if(exchange.getRequestPath().equalsIgnoreCase("/read")){
                	 readHandler(exchange);
                 }
                 else if(exchange.getRequestPath().equalsIgnoreCase("/add")){
                	 writeHandler(exchange);
                 }
                 else if(exchange.getRequestPath().equalsIgnoreCase("/check")){
                	 checkValid(exchange);
                 }
                 else if(exchange.getRequestPath().equalsIgnoreCase("/delete")){
                	 deleteHandler(exchange);
                 }
                 else{
                	 exchange.getResponseSender().send(USAGE);
                 }
               }
     }).build();
 server.start();

}
public static void readHandler(HttpServerExchange exchange){
		StringBuilder result = new StringBuilder();
		for (Entry<String, String> entry : listCompanies.entrySet()) {
			result.append(entry.getKey()+" : "+entry.getValue());
		}
		exchange.getResponseSender().send(readAll());
		return;
}
public static void writeHandler(HttpServerExchange exchange){
	String name = null;
	String data = null;
	try{
	 name = exchange.getQueryParameters().get("name").getFirst();
	 data = exchange.getQueryParameters().get("data").getFirst();
	}catch(Exception ex){
		exchange.getResponseSender().send(USAGE);
		return;
	}
	listCompanies.put(name,data);
	System.out.println("calling writeToDB");
	writeToDB(name, data);
	exchange.getResponseSender().send("added");
}
public static void checkValid(HttpServerExchange exchange){
	String name = null;
	try{
	name = exchange.getQueryParameters().get("name").getFirst();
	}catch(Exception ex){
		exchange.getResponseSender().send(USAGE);
		return;
	}
	exchange.getResponseSender().send(checkIfExists(name));
}
public static void deleteHandler(HttpServerExchange exchange){
	String name = null;
	try{
	name =exchange.getQueryParameters().get("name").getFirst();
	}
	catch(Exception ex){
		exchange.getResponseSender().send(USAGE);
		return;
	}	
	BasicDBObject document = new BasicDBObject();
	document.put("name", name);
	Document filter = new Document();
	filter.append("name",name);
	DeleteResult result = db.getCollection(COLL_NAME).deleteMany(filter);
	if(result.wasAcknowledged())
		exchange.getResponseSender().send("Deleted the records");
	
}
public static void initializeConnection(){
	db = mongo.getDatabase(DB_NAME);
	System.out.println(db);
	
}
public static JSONObject queryJobs(String name){
	BasicDBObject query = new BasicDBObject();
	query.put("name", name.toLowerCase());
	System.out.println(query.toJson());
	List<Document> documents = (List<Document>) db
			.getCollection(COLL_NAME)
			.find(query)
			.into(new ArrayList<Document>());
	JSONObject obj = new JSONObject(documents.get(0));
	return obj;
	
}
public static void writeToDB(String name, String data){
	Document doc = new Document();
	doc.put("name",name);
	doc.put("data", data);
	db.getCollection(COLL_NAME).insertOne(doc);
	System.out.println("written to db");
}

public static String readAll(){
	StringBuilder sb = new StringBuilder();
	FindIterable<Document> documents = db.getCollection(COLL_NAME).find();
	for(Document doc:documents){
		JSONObject obj = new JSONObject(doc.toJson());		
		sb.append("name: "+obj.get("name")+"\n"+"data: "+obj.get("data"));
		sb.append("\n");
	}
	return sb.toString();
}
public static String checkIfExists(String name){
	StringBuilder sb = new StringBuilder();
	BasicDBObject query = new BasicDBObject("name",name);
	FindIterable<Document> documents = db.getCollection(COLL_NAME).find(query);
	for(Document doc:documents){
		JSONObject obj = new JSONObject(doc.toJson());	
		sb.append("name: "+obj.get("name")+"\n"+"data: "+obj.get("data"));
		sb.append("\n");
	}
	if(sb.length()==0) //no document returned
	{
		sb.append("You have not applied to the company "+name);
	}
	return sb.toString();
}
}
