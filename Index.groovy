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

import org.bonitasoft.engine.api.TenantAPIAccessor;


import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProfileAPI;

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

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;


import org.bonitasoft.ext.properties.BonitaProperties;

public class Index implements PageController {

	private static final BEvent eventSaveParameter 		= new BEvent("org.bonitasoft.foodtruck", 1, Level.INFO, "Parameters saved with success", "Parameters are saved with success for your next visit");
	private static final BEvent eventErrorSaveParameter = new BEvent("org.bonitasoft.foodtruck", 2, Level.APPLICATIONERROR, "Error during parameters save", "Parameters can't be saved with success", "Your parameters will be stay as before", "Check exception");
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft");
		
		PrintWriter out = response.getWriter()
		
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");

			String action=request.getParameter("action");
            
        	String jsonParamEncode = request.getParameter("json");
            String jsonParamSt = (jsonParamEncode==null ? null : java.net.URLDecoder.decode(jsonParamEncode, "UTF-8"));
            
        
            
            
            
            List<BEvent> listEvents = new ArrayList<BEvent>();
            
			logger.info("###################################### action is["+action+"] json["+jsonParamSt+"]");
			if (action==null || action.length()==0 )
			{
				// logger.severe("###################################### RUN Default !");
				
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			
			APISession apiSession = pageContext.getApiSession()
			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);			
			IdentityAPI identityApi = TenantAPIAccessor.getIdentityAPI(apiSession);
			
			
			HashMap<String,Object> answer = null;
			if ("listcustompage".equals(action))
			{
				FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( jsonParamSt );
                foodTruckParam.listRepository.add(  foodTruckParam.getCommunityRepository() );
                
                File customPageFile = pageResourceProvider.getPageDirectory();
                foodTruckParam.directoryFileLocaly = customPageFile.getAbsolutePath();
                
                answer = FoodTruckAccess.getListCustomPage(foodTruckParam, apiSession).toMap();
               
			} else if ("downloadcustompage".equals(action))
            {
                File customPageFile = pageResourceProvider.getPageDirectory();

                FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( jsonParamSt );
                foodTruckParam.listRepository.add(  foodTruckParam.getCommunityRepository() );
                
                foodTruckParam.directoryFileLocaly = customPageFile.getAbsolutePath(); 
                answer = FoodTruckAccess.downloadAndInstallCustomPage(foodTruckParam, apiSession).toMap();

            } else if ("addcustompageinprofile".equals(action))
            {
                  FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( jsonParamSt );
                  answer = FoodTruckAccess.addInProfile(foodTruckParam, apiSession).toMap();
                
            } else if ("removecustompagefromprofile".equals(action))
            {
                  FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt( jsonParamSt );
                  answer = FoodTruckAccess.removeFromProfile(foodTruckParam, apiSession).toMap();
                
            } else if ("saveparameters".equals(action))
            {
                answer = new HashMap();
                try
                {

	                BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );
	                listEvents.addAll( bonitaProperties.load() );
	                bonitaProperties.put("param", jsonParamSt);
	                listEvents.addAll( bonitaProperties.store() );
	                if (! BEventFactory.isError(listEvents))
	                {
	                	listEvents.add( eventSaveParameter);
	                }
                }
                catch(Exception e)
                {
                    logger.severe("Can't saveParameters ["+jsonParamSt+"]");
                	listEvents.add( eventErrorSaveParameter, e.toString());
                }
                answer.put("listevents",  BEventFactory.getHtml( listEvents));

            } else if ("loadparameters".equals(action))
            {
                answer = new HashMap();
                String trace="";
                BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );
                listEvents.addAll( bonitaProperties.load() );
                logger.info("BonitaProperties.saveConfig: loadproperties done, events = "+listEvents.size() );
                String paramSt = bonitaProperties.get("param");
                if (paramSt!=null)
                {
                	logger.info("loadparameters read ="+paramSt+";");
                	HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse( paramSt );
                    answer.put("param",  jsonHash);
                }
                else
                	answer.put("param",  new HashMap<String,Object>());
                
                answer.put("listevents",  BEventFactory.getHtml( listEvents));

                
              
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
			
			// def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
			// indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
			// indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
			
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter();
			out.print(indexContent);
			out.flush();
			out.close();
	} catch (Exception e) {
			e.printStackTrace();
	}
		}
		
}
