/**
 * @author Gavin
 */
package com.example.tidegrab;

import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

//Contains methods and classes required to scrape tide data, and update Graph Views
public class Scraper {
	
	private String url;
	private ProgressDialog mProgressDialog;
	private TideApplication tideApp;
	private GraphView graph;
	private GraphViewSeries series;
	
	//List for scraped data storage
    ArrayList<GraphViewData> tideData;
	
	TideInfo tideInfo;
	
	//Default constructor, useful for JUnit
	public Scraper(){
		
	}
	
	public Scraper(TideApplication tideApplication){
		 tideInfo = new TideInfo();
		 tideData = new ArrayList<GraphViewData>();
		 this.tideApp = tideApplication;
	}
	
	public TideInfo get_tideInfo(){
		return tideInfo;
	}
	
	//Updates the GraphView of the Activity currently running
	public void updateTideGraph(){
		this.graph = tideApp.getGraph();

		tideApp.getActivity().runOnUiThread(new Runnable() {
		    public void run() {
		    	graph.removeAllSeries();
				series = createSeries();
		        graph.addSeries(series);
		        graph.setViewPort(25, 25);
		        graph.setScrollable(true);
		        graph.setScalable(true);
		    }
		});		
	}
	
	//Converts the list of GraphViewData objects into a single GraphViewSeries object
	GraphViewSeries createSeries(){ 
		return new GraphViewSeries("Tide Heights", null, tideData.toArray(new GraphViewData[tideData.size()]));
	}
	
	//Handles the extraction of data from the internet. Invokes methods to update the Graph View.
    public class TideInfo extends AsyncTask<String, Void, GraphViewSeries>{
        String info;
    	Elements rows;

        @Override
        protected void onPreExecute() {
        	Log.d("Gbug", "AsyncTask called");
        	super.onPreExecute();
            mProgressDialog = new ProgressDialog(tideApp.getActivity());
            mProgressDialog.setTitle("Retreiving Tide Information");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
            Log.d("Gbug", "preExecute finished");
        }
        
        @Override
        protected GraphViewSeries doInBackground(String... params) {
            try {
            	Log.d("Gbug", "GraphView doInBackground started");
            	url = "http://www.waterlevels.gc.ca/eng/station?sid=" + params[0];
            	Document doc = Jsoup.connect(url).get();
            	tideData.clear();
            	extractHeight(doc);           	
            	
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
 
        @Override
        protected void onPostExecute(GraphViewSeries result) {
            mProgressDialog.dismiss();
            updateTideGraph();
        }
        
        //Extracts Height data from an HTML document. Stores the data into tideData
        public String extractHeight(Document doc){
        	int iteration = 0;
        	int rownum = 0;
        	
        	Elements tds = null;
        	
        	for (Element table : doc.select("table[title=Predicted Hourly Heights (m)]")) {
                for (Element row : table.select("tr")) {
                	info = "";
                    Log.d("Scraped", "Row #: " + Integer.toString(rownum)); 
                    
                	//Grabbing the heights
                    tds = null;
                    tds = row.select("td");
                    Log.d("Scraped", "Row size: " + Integer.toString(tds.size()));
                    if(tds.size() > 1){
                    	Log.d("Scraped", "tds.size meets threshold");
                    	for( int hour = 0; hour < tds.size(); hour++){	
                    		String text = tds.get(hour).text();
                    		
                    		info += Integer.toString(hour); //Outputting the associated hour as well
                    		info += ": " + text;
                    		info += "  ,  ";
                    		
                    		//Currently Only Graphing Data for the first available date
                    		if(iteration == 0){
                    			tideData.add(new GraphViewData(hour, Float.parseFloat(text)));
                    		}	
                    	}
                    		Log.d("Scraped", "Td Size: " + Integer.toString(tds.size()));
                    		Log.d("Scraped", info);
                    		info += "\n";
                    	iteration++;
                    }
                    rownum++;  
                }	
        	}
        	//Returned for testing purposes
        	return info;
        }
    }
       
}
