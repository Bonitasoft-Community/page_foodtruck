import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;
import java.util.Properties;


import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import org.apache.commons.lang3.StringEscapeUtils

import org.bonitasoft.engine.identity.User;
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProfileAPI;

import com.bonitasoft.engine.api.PlatformMonitoringAPI;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;

import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
	

import com.bonitasoft.custompage.foodtruck.FoodTruckAccess.FoodTruckParam;
import com.bonitasoft.custompage.foodtruck.Toolbox.FoodTruckResult;
import com.bonitasoft.custompage.foodtruck.FoodTruckAccess;

public class Index implements PageController {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft");
		
		
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter()

			String action=request.getParameter("action");
            String json=request.getParameter("json");
			logger.info("###################################### action is["+action+"] json["+json+"]");
			if (action==null || action.length()==0 )
			{
				// logger.severe("###################################### RUN Default !");
				
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			
			APISession apiSession = pageContext.getApiSession()
			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
			PlatformMonitoringAPI platformMonitoringAPI = TenantAPIAccessor.getPlatformMonitoringAPI(apiSession);
			IdentityAPI identityApi = TenantAPIAccessor.getIdentityAPI(apiSession);
			
			
			HashMap<String,Object> answer = null;
			if ("listcustompage".equals(action))
			{
				FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( json );
                foodTruckParam.listRepository.add(  foodTruckParam.getCommunityRepository() );
                
                File customPageFile = pageResourceProvider.getPageDirectory();
                foodTruckParam.directoryFileLocaly = customPageFile.getAbsolutePath();
                
                answer = FoodTruckAccess.getListCustomPage(foodTruckParam, apiSession).toMap();
               
			} else if ("downloadcustompage".equals(action))
            {
                File customPageFile = pageResourceProvider.getPageDirectory();

                FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( json );
                foodTruckParam.listRepository.add(  foodTruckParam.getCommunityRepository() );
                
                foodTruckParam.directoryFileLocaly = customPageFile.getAbsolutePath(); 
                answer = FoodTruckAccess.downloadAndInstallCustomPage(foodTruckParam, apiSession).toMap();

            } else if ("addcustompageinprofile".equals(action))
            {
                  FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( json );
                  answer = FoodTruckAccess.addInProfile(foodTruckParam, apiSession).toMap();
                
            } else if ("removecustompagefromprofile".equals(action))
            {
                  FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( json );
                  answer = FoodTruckAccess.removeFromProfile(foodTruckParam, apiSession).toMap();
                
            } else if ("saveparameters".equals(action))
            {
                answer = new HashMap();
                File fileParam=null;
                try
                {
                    fileParam = pageResourceProvider.getResourceAsFile( "param.properties" );
                    logger.info("Save ["+json+"] in file ["+fileParam.getAbsolutePath()+"]");
                    FileOutputStream filePropertiesSave = new FileOutputStream( fileParam );
                    
                    Properties props = new Properties();
                    props.setProperty( "params", json );
                    /*
                    final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse( json);
                    for (String key : jsonHash.keySet())
                        props.setProperty(key, jsonHash.get( key ).toString());
                        */
                    props.store(filePropertiesSave,"saveparam");
                    answer.put("status", "saved");
                }
                catch(Exception e)
                {
                    logger.severe("Can't saveParameters ["+json+"] in file ["+ (fileParam==null ? "null": fileParam.getAbsolutePath())+"]");
                    
                    answer.put("status", "error "+e.toString());
                }
            } else if ("loadparameters".equals(action))
            {
                answer = new HashMap();
                File fileParam=null;
                try
                {
                    fileParam = pageResourceProvider.getResourceAsFile( "param.properties" );
                    FileInputStream filePropertiesLoad = new FileInputStream( fileParam );
                    
                    Properties props = new Properties();
                    props.load(filePropertiesLoad);
                    String jsonProperties = props.getProperty("params");
                    
                    logger.info("loadparameters jsonProperties=["+jsonProperties+"]  from file ["+ (fileParam==null ? "null": fileParam.getAbsolutePath())+"] ");
                    String trace="";
                    for (String key : props.keySet())
                        trace+=key+":"+ props.get( key )+";";
                    logger.info("loadparameters getMap ="+trace+";");
                    if (jsonProperties!=null)
                    {
                        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse( jsonProperties );
                        answer.putAll( jsonHash );
                    }
                }
                catch(Exception e)
                {
                    logger.severe("Can't loadparameters from file ["+ (fileParam==null ? "null": fileParam.getAbsolutePath())+"] error :"+e.toString());
                
                    answer.put("status", "error "+e.toString());
                }
            }

			
			if (answer != null)
			{			
				String jsonDetailsSt = JSONValue.toJSONString( answer );
				logger.info("Index.groovy action ["+action+"] return Json="+jsonDetailsSt);
				
				out.write( jsonDetailsSt );
				out.flush();
				out.close();				
				return;				
			}
			
			out.write( "Unknow command" );
			out.flush();
			out.close();
			return;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
            out.write( "Exception ["+e.toString()+"] at "+exceptionDetails);
            out.flush();
            out.close();
		}
	}

	
	/** -------------------------------------------------------------------------
	 *
	 *runTheBonitaIndexDoGet
	 * 
	 */
	private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
				try {
						def String indexContent;
						pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
								indexContent = s.getText()
						}
						
						def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
						
						indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
						indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
						
						response.setCharacterEncoding("UTF-8");
						PrintWriter out = response.getWriter();
						out.print(indexContent);
						out.flush();
						out.close();
				} catch (Exception e) {
						e.printStackTrace();
				}
		}
		
		/**
		to create a simple chart
		*/
		public static class ActivityTimeLine
		{
				public String activityName;
				public Date dateBegin;
				public Date dateEnd;
				
				public static ActivityTimeLine getActivityTimeLine(String activityName, int timeBegin, int timeEnd)
				{
					Calendar calBegin = Calendar.getInstance();
					calBegin.set(Calendar.HOUR_OF_DAY , timeBegin);
					Calendar calEnd = Calendar.getInstance();
					calEnd.set(Calendar.HOUR_OF_DAY , timeEnd);
					
						ActivityTimeLine oneSample = new ActivityTimeLine();
						oneSample.activityName = activityName;
						oneSample.dateBegin		= calBegin.getTime();
						oneSample.dateEnd 		= calEnd.getTime();
						
						return oneSample;
				}
				public long getDateLong()
				{ return dateBegin == null ? 0 : dateBegin.getTime(); }
		}
		
		
		/** create a simple chart 
		*/
		public static String getChartTimeLine(String title, List<ActivityTimeLine> listSamples){
				Logger logger = Logger.getLogger("org.bonitasoft");
				
				/** structure 
				 * "rows": [
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },

				 */
				String resultValue="";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");
				
				for (int i=0;i<listSamples.size();i++)
				{
					logger.info("sample [i] : "+listSamples.get( i ).activityName+"] dateBegin["+simpleDateFormat.format( listSamples.get( i ).dateBegin)+"] dateEnd["+simpleDateFormat.format( listSamples.get( i ).dateEnd) +"]");
						if (listSamples.get( i ).dateBegin!=null &&  listSamples.get( i ).dateEnd != null)
								resultValue+= "{ \"c\": [ { \"v\": \""+listSamples.get( i ).activityName+"\" }," ;
								resultValue+= " { \"v\": \""+listSamples.get( i ).activityName +"\" }, " ;
								resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateBegin) +")\" }, " ;
								resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateEnd) +")\" } " ;
								resultValue+= "] },";
				}
				if (resultValue.length()>0)
						resultValue = resultValue.substring(0,resultValue.length()-1);
				
				String resultLabel = "{ \"type\": \"string\", \"id\": \"Role\" },{ \"type\": \"string\", \"id\": \"Name\"},{ \"type\": \"datetime\", \"id\": \"Start\"},{ \"type\": \"datetime\", \"id\": \"End\"}";
				
				String valueChart = "	{"
					   valueChart += "\"type\": \"Timeline\", ";
					  valueChart += "\"displayed\": true, ";
					  valueChart += "\"data\": {";
					  valueChart +=   "\"cols\": ["+resultLabel+"], ";
					  valueChart +=   "\"rows\": ["+resultValue+"] ";
					  /*
					  +   "\"options\": { "
					  +         "\"bars\": \"horizontal\","
					  +         "\"title\": \""+title+"\", \"fill\": 20, \"displayExactValues\": true,"
					  +         "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
					  */
					  valueChart +=  "}";
					  valueChart +="}";
// 				+"\"isStacked\": \"true\","
 	          
//		    +"\"displayExactValues\": true,"
//		    
//		    +"\"hAxis\": { \"title\": \"Date\" }"
//		    +"},"
				logger.info("Value1 >"+valueChart+"<");

				
				return valueChart;		
		}	
}
