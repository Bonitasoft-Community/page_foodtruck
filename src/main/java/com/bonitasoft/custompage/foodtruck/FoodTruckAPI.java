package com.bonitasoft.custompage.foodtruck;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.InvalidPageTokenException;
import org.bonitasoft.engine.exception.InvalidPageZipContentException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.exception.UpdatingWithInvalidPageTokenException;
import org.bonitasoft.engine.exception.UpdatingWithInvalidPageZipContentException;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileEntry;
import org.bonitasoft.engine.profile.ProfileEntrySearchDescriptor;
import org.bonitasoft.engine.profile.ProfileNotFoundException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreAPI;
import org.bonitasoft.store.BonitaStoreCommunity;
import org.bonitasoft.store.BonitaStoreFactory;
import org.bonitasoft.store.BonitaStoreGit;
import org.bonitasoft.store.BonitaStoreResult;
import org.bonitasoft.store.BonitaStore.DetectionParameters;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.artifact.ArtifactCustomPage;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.foodtruck.ArtifactFoodTruck.AppsStatus;
import com.bonitasoft.engine.profile.ProfileEntryCreator;

public class FoodTruckAPI {

  private static final BEvent eventSearchException = new BEvent(FoodTruckAPI.class.getName(), 1, Level.ERROR, "Search exception", "The search in engine failed", "No result", "Check the log (maybe not the same BonitaVersion ?)");

  private static final BEvent eventProfileNotFound = new BEvent(FoodTruckAPI.class.getName(), 2, Level.ERROR, "Profile not found", "A profile is register in a page, but can't be found by it's ID", "Check the log (maybe not the same BonitaVersion ?)");

  private static final BEvent serverAPIError = new BEvent(FoodTruckAPI.class.getName(), 3, Level.ERROR, "ServerAPI failed", "A request is asked to the server to get the different API, and the call failed", "No answer will be visible", "You should be disconnected : reconnect");

  private static final BEvent pageAlreadyExist = new BEvent(FoodTruckAPI.class.getName(), 4, Level.APPLICATIONERROR, "Page already exist", "The page can't be load in the portal, because it's already exist with the same name", "Check the name of the page");

  private static final BEvent invalidePageToken = new BEvent(FoodTruckAPI.class.getName(), 5, Level.ERROR, "Invalid Page token", "The page can't be created due to an Invalid Page Token", "Contact the Support to get explanation on this exception");

  private static final BEvent invalidZipContent = new BEvent(FoodTruckAPI.class.getName(), 6, Level.APPLICATIONERROR, "Invalid Zip Content", "The page downloaded is not a correct ZIP file, and does not have the expected format", "Contact the Support to fix the content of this page");

  private static final BEvent creationException = new BEvent(FoodTruckAPI.class.getName(), 7, Level.ERROR, "Creation exception", "An error arrived during the creation of the page", "Check the message");

  private static final BEvent saveLocalFile = new BEvent(FoodTruckAPI.class.getName(), 8, Level.APPLICATIONERROR, "File save error", "An error arrive when the file is saved", "Check the message");

  private static final BEvent searchProfileError = new BEvent(FoodTruckAPI.class.getName(), 9, Level.ERROR, "Search profile", "A search profile failed", "Check the message");

  private static final BEvent comProfileAPIRequested = new BEvent(FoodTruckAPI.class.getName(), 10, Level.APPLICATIONERROR, "Com Profile API required", "To reference the profile in a profile, the Subscription version is required", "Use a subscription version");

  private static final BEvent failRegisterPageInProfile = new BEvent(FoodTruckAPI.class.getName(), 11, Level.APPLICATIONERROR, "Registration failed", "The registration of the page in the profile failed", "Check the message");

  private static final BEvent notAllowToUpdatePage = new BEvent(FoodTruckAPI.class.getName(), 12, Level.APPLICATIONERROR, "Not allow to update the page", "The update failed", "Check the message");

  private static final BEvent updateError = new BEvent(FoodTruckAPI.class.getName(), 12, Level.APPLICATIONERROR, "Update failed", "The new page can't be install, because it's not allow to update an existing page", "Allow the user to update existing page");

  private static final BEvent noContentAvailable = new BEvent(FoodTruckAPI.class.getName(), 13, Level.APPLICATIONERROR, "No content available", "The application can't be download", "The application can't be uploaded, so can't be installed",
      "Ask the repository administrator, or wait the avaibility of the application");

  public enum FilterStatusEnum {
    ALL, LOCALONLY, NEWONLY, UPDATABLEONLY
  };

  // access the BonitaCommunity

  /**
   * FoodTruckParam
   */
  public static class FoodTruckParam {

    private List<BonitaStore> listRepositories = new ArrayList<BonitaStore>();

    public ArtifactFoodTruck artifactFoodTruck;
    public Long profileid;
    public boolean keepIsProvided = true;
    /**
     * perimeter of type artefact. if NULL, that means "all artefacts" 
     */
    public TypeArtifact typeArtifact=null;
    public FilterStatusEnum filterStatus = FilterStatusEnum.ALL;
    public boolean reload =true;
    int searchMaxResources = 1000;

    // if true, then all download file are saved localy, using the
    // directoryFileLocaly repository
    public boolean saveDownloadFileLocaly = false;
    public boolean saveLogoFileLocaly = true;
    public String directoryFileLocaly;
    public boolean allowUpdatePage = true;

    /**
     * github to access the community - better to have a login password to
     * not reach the API Limit
     */
    public boolean defaultLogin=true;
    public String githubLogin = null;
    public String githubPassword = null;

    LoggerStore logBox = new LoggerStore();

    /**
     * @param jsonSt
     * @return
     */
    public static FoodTruckParam getInstanceFromJsonSt(final String jsonSt) {
      if (jsonSt == null) {
        return new FoodTruckParam();
      }
      final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
      if (jsonHash == null) {
        return new FoodTruckParam();
      }
      final FoodTruckParam foodTruckParam = new FoodTruckParam();
      try {
        if (jsonHash.get("loglevel") != null) {
          foodTruckParam.logBox.logLevel = LoggerStore.LOGLEVEL.valueOf((String) jsonHash.get("loglevel"));
        }
      } catch (final Exception e) {
        foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Decode [loglevel] parameter in Json :" + e.toString());
      } ;
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.DEBUG, "FoodTruckParam: _JsonSt[" + jsonSt + "] ");

      // calculate the list of repository now
      BonitaStoreAPI bonitaStoreAPI = new BonitaStoreAPI();
      final BonitaStoreFactory bonitaStoreFactory = bonitaStoreAPI.getBonitaStoreFactory();
      
      foodTruckParam.resolveListRepository( bonitaStoreFactory );
      
  
      // now, we have all in hand to solve the artefact
      
      foodTruckParam.artifactFoodTruck = ArtifactFoodTruck.getInstanceFromJsonSt( jsonSt, bonitaStoreFactory );

      foodTruckParam.profileid = Toolbox.getLong(jsonHash.get("profileid"), null);

      String typeArtifact = Toolbox.getString( jsonHash.get("typeArtifact"), null);
      if (typeArtifact==null || "ALL".equals(typeArtifact))
          foodTruckParam.typeArtifact = null;
      else {
          try
          {
              foodTruckParam.typeArtifact = TypeArtifact.valueOf( typeArtifact );
          } catch(Exception e) {}
      }
      
      
      final String show = Toolbox.getString( jsonHash.get("show"),"ALL"); 
      if ("MYPLATFORM".equals(show)) {
        foodTruckParam.filterStatus = FilterStatusEnum.LOCALONLY;
      } else if ("ALL".equals(show) ) {
        foodTruckParam.filterStatus = FilterStatusEnum.ALL;
      } else if ("WHATSNEWS".equals(show)) {
        foodTruckParam.filterStatus = FilterStatusEnum.NEWONLY;
      } else if ("WHATSUPDATE".equals(show)) {
        foodTruckParam.filterStatus = FilterStatusEnum.UPDATABLEONLY;
      } else if ("SEARCH".equals(show)) {
        // not yet implemented
        foodTruckParam.filterStatus = FilterStatusEnum.ALL;
      } else
          foodTruckParam.filterStatus = FilterStatusEnum.ALL;

      foodTruckParam.reload = Toolbox.getBoolean(jsonHash.get("reload"), Boolean.FALSE);

      // login - password
      final Map<String, Object> github = Toolbox.getMap(jsonHash.get("github"), null);
      if (github != null) {
        foodTruckParam.defaultLogin = Toolbox.getBoolean(github.get("defaultLogin"), Boolean.TRUE);
        foodTruckParam.githubLogin = Toolbox.getString(github.get("gitlogin"), null);
        foodTruckParam.githubPassword = Toolbox.getString(github.get("gitpassword"), null);
        
      }

      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.INFO, "FoodTruckParam:  github[" + foodTruckParam.githubLogin + "] pass[" + foodTruckParam.githubPassword + "]");

      return foodTruckParam;
    }

    @Override
    public String toString() {
      return "filter[" + filterStatus + "]";
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Repository     */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

 
    /**
     * resolve all store according the parameters
     * @param bonitaStoreFactory
     */
    public void resolveListRepository( BonitaStoreFactory bonitaStoreFactory )
    {
      BonitaStore bonitaStore= null;
      if (defaultLogin) {
        bonitaStore = bonitaStoreFactory.getBonitaCommunityStore(true );
      } else if (githubLogin !=null) {
        bonitaStore = bonitaStoreFactory.getGitStore(githubLogin, githubPassword, BonitaStoreAPI.CommunityGithubUrlRepository, true);
      }
      
      if (bonitaStore !=null)
      {
        addInListRepository(bonitaStore);;
        
      }
    }
    
    public void addInListRepository(BonitaStore bonitaStore ) {
        listRepositories.add( bonitaStore );
    }
    public List<BonitaStore> getListRepositories() {
        return listRepositories;
    }
    
  }


  public static BonitaStore getCommunityRepository() {
      BonitaStore bonitaStore = BonitaStoreAPI.getInstance().getBonitaCommunityStore( true );

      return bonitaStore;
  }
  
  /* ******************************************************************************** */
  /*                                                                                  */
  /* GetAPI     */
  /*                                                                                  */
  /*                                                                                  */
  /* ******************************************************************************** */


  
  /**
   * @param foodTruckParam
   * @param pageAPI
   * @param profileAPI
   * @return
   */
  public static FoodTruckResult getListArtifacts(final FoodTruckParam foodTruckParam, final APISession apiSession) {
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logBeginOperation("getListCustomPage", "Filter=" + foodTruckParam.filterStatus, foodTruckParam);
    }

    
    final ProfileAPI profileAPI;
    try {
      
      profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);
    } catch (ServerAPIException | UnknownAPITypeException | BonitaHomeNotSetException e) {
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Error creating api");
      final FoodTruckResult dashboardResult = new FoodTruckResult("getListCustomPage");
      dashboardResult.addEvent(new BEvent(serverAPIError, e, ""));
      if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
        logEndOperation("getListCustomPage", "", dashboardResult, foodTruckParam);
      }

      return dashboardResult;
    }
    LoggerStore loggerStore = new LoggerStore();
    DetectionParameters detectionParameters = new DetectionParameters();
    detectionParameters.listTypeArtifact = (getListTypeApps( foodTruckParam));

    BonitaStore bonitaLocalStore = BonitaStoreAPI.getInstance().getLocalStore(apiSession);
    BonitaStoreResult storeResult = bonitaLocalStore.getListArtefacts(detectionParameters, loggerStore);
    final FoodTruckResult foodTruckResult = new FoodTruckResult("ListArtefacts");
    for (Artifact artefact : storeResult.listArtifacts)
    {
        foodTruckResult.listArtifacts.add( ArtifactFoodTruck.getInstance( artefact ));

    }
    
    foodTruckResult.addEvents( storeResult.getEvents());
    foodTruckResult.allListProfiles = storeResult.allListProfiles;
    // final FoodTruckResult foodTruckResult = getListLocalResources(foodTruckParam, pageAPI, profileAPI);

    // add all store ?
    if (foodTruckParam.filterStatus == FilterStatusEnum.ALL || foodTruckParam.filterStatus == FilterStatusEnum.NEWONLY || foodTruckParam.filterStatus == FilterStatusEnum.UPDATABLEONLY) {
     

      //--------------------------  Collect all pages form all repository
      final List<Artifact> listArtefactsFromStores = new ArrayList<Artifact>();
      for (final BonitaStore bonitaStore : foodTruckParam.getListRepositories()) {
        if (foodTruckParam.logBox.isLog(LOGLEVEL.DEBUG)) {
          foodTruckParam.logBox.log(LOGLEVEL.DEBUG, "--- FoodTruckAPI.getListCustomPage: call Repository [" + bonitaStore.getName() + "]");
        }


        
        storeResult = bonitaStore.getListArtefacts(detectionParameters, loggerStore);

        foodTruckResult.addEvents(storeResult.getEvents());
        for (Artifact artefact : storeResult.listArtifacts)
          listArtefactsFromStores.add(artefact);

        if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
          foodTruckParam.logBox.log(LOGLEVEL.INFO, "--- FoodTruckAPI.getListCustomPage: Repository [" + bonitaStore.toString() + "] Collect [" + storeResult.listArtifacts + "] apps");
        }

      }

      // maybe already deployed ?
      for (final Artifact storeApp : listArtefactsFromStores) {

        // save the logo file ?
        if (foodTruckParam.saveLogoFileLocaly) {
          if (storeApp.logo != null) {
            // logo generated by GenerateListingItem.java
            FileOutputStream fileOutput;
            final String fileName = foodTruckParam.directoryFileLocaly + "/resources/storeapp/" + storeApp.getName() + ".jpg";
            if (foodTruckParam.logBox.isLog(LOGLEVEL.DEBUG)) {
              foodTruckParam.logBox.log(LOGLEVEL.DEBUG, "FoodTruckAPI.getListCustomPage Save LogoFile[" + storeApp.getName() + "] on [" + fileName + "]");
            }
            try {

              fileOutput = new FileOutputStream(new File(fileName));
              fileOutput.write(storeApp.logo);
              fileOutput.close();
            } catch (final FileNotFoundException e) {
              foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckResult.getListCustomPage: Can't write on [" + fileName + "] " + e.toString());
            } catch (final IOException e) {
              foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckResult.getListCustomPage: error during writing [" + fileName + "] " + e.toString());
            }
          }
        }
        // let's fullfill the foodTruckResult.apps list.
        final ArtifactFoodTruck artefactFoodtruck = foodTruckResult.getArtefactByName(storeApp.getName());
        if (artefactFoodtruck != null) {
          // apps already registers in the list, so we UPDATE the apps list
            artefactFoodtruck.status = AppsStatus.OK;
          // logo are on the store, not locally (not yet)
          if (artefactFoodtruck.artifact.logo == null) {
              artefactFoodtruck.artifact.logo = storeApp.logo;
          }
          if (artefactFoodtruck.artifact.getLastReleaseDate() != null && storeApp.getLastReleaseDate() != null && storeApp.getLastReleaseDate().after(artefactFoodtruck.artifact.getLastReleaseDate())) {
              artefactFoodtruck.status = AppsStatus.TOUPDATE;
              artefactFoodtruck.artifact.setWhatsnews( storeApp.getReleaseInformation(artefactFoodtruck.artifact.getLastReleaseDate()) );
          }
          // update every time from the store
          artefactFoodtruck.updateForLastRelease( storeApp );
          
          if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
            foodTruckParam.logBox.log(LOGLEVEL.INFO, "Apps[" + artefactFoodtruck.artifact.getName() + "] status[" + artefactFoodtruck.status.toString() + "] LocalDate [" + artefactFoodtruck.artifact.getLastReleaseDate() + "] StoreApps[" + storeApp.getLastReleaseDate() + "]");
          }
        } else // new apps on store : add it in the apps list
        {
            ArtifactFoodTruck artefactFoodTruck = new ArtifactFoodTruck();
            artefactFoodTruck.artifact = storeApp;
            artefactFoodTruck.status = AppsStatus.NEW;
          foodTruckResult.addApps(artefactFoodTruck);
          if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
            foodTruckParam.logBox.log(LOGLEVEL.INFO, "Apps[" + storeApp.getName() + "] status[NEW] StoreApps[" + storeApp.getLastReleaseDate() + "]");
          }

        }
        
      }
    }
    // ---------------------- ok, lets apply the filter now
    final List<ArtifactFoodTruck> listFilterCustomPage = new ArrayList<ArtifactFoodTruck>();
    for (final ArtifactFoodTruck appsItem : foodTruckResult.listArtifacts) {
      if (appsItem.artifact.isProvided() && !foodTruckParam.keepIsProvided) {
        continue;
      }

      if (foodTruckParam.filterStatus == FilterStatusEnum.ALL) {
        listFilterCustomPage.add(appsItem);
      } else if (foodTruckParam.filterStatus == FilterStatusEnum.LOCALONLY) {
        if (appsItem.status == AppsStatus.LOCAL) {
          listFilterCustomPage.add(appsItem);
        }
      } else if (foodTruckParam.filterStatus == FilterStatusEnum.NEWONLY) {
        if (appsItem.status == AppsStatus.NEW) {
          listFilterCustomPage.add(appsItem);
        }
      } else if (foodTruckParam.filterStatus == FilterStatusEnum.UPDATABLEONLY) {
        if (appsItem.status == AppsStatus.TOUPDATE) {
          listFilterCustomPage.add(appsItem);
        }
      }
    }
    foodTruckResult.listArtifacts = listFilterCustomPage;

    //------------------  sort by name now
    Collections.sort(foodTruckResult.listArtifacts, new Comparator<ArtifactFoodTruck>() {

      @Override
      public int compare(final ArtifactFoodTruck s1, final ArtifactFoodTruck s2) {
        return s1.getName().compareTo(s2.getName());
      }
    });

    //---------------------  Search the profiles
    try {
      final SearchOptionsBuilder searchOptionProfile = new SearchOptionsBuilder(0, 1000);
      // Not working !!
      // searchOptionProfile.filter(ProfileSearchDescriptor.IS_DEFAULT,
      // Boolean.FALSE);
      final SearchResult<Profile> searchResult = profileAPI.searchProfiles(searchOptionProfile.done());
      for (final Profile profile : searchResult.getResult()) {
        if (profile.isDefault()) {
          continue;
        }
        foodTruckResult.allListProfiles.add(profile);
      }
      foodTruckResult.isAllowAddProfile = true;
    } catch (final SearchException e) {
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Error during seachProfile [" + e.toString() + "]");
      foodTruckResult.addEvent(new BEvent(searchProfileError, e, ""));
    }
    foodTruckResult.endOperation();
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logEndOperation("getListCustomPage", "", foodTruckResult, foodTruckParam);
    }

    return foodTruckResult;
  }

  /**
   * from an custompage page name, saved in foodTruckParam.appsItem, reload
   * all information
   *
   * @param foodTruckParam
   * @param apiSession
   * @return
   */
  public static FoodTruckResult completeCustomPage(final FoodTruckParam foodTruckParam, final APISession apiSession) {
    final FoodTruckResult foodTruckResult = new FoodTruckResult("completeCustomPage");
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logBeginOperation("completeCustomPage", "appsItem=" + foodTruckParam.artifactFoodTruck.getName(), foodTruckParam);
    }

    /*
     * final FoodTruckResult foodTruckResultSearch =
     * searchAppsByNameInRepository(foodTruckParam.appsItem.getAppsName(), foodTruckParam);
     * foodTruckResult.addEvents(foodTruckResultSearch.getEvents());
     * foodTruckResult.listStoreItem = foodTruckResultSearch.listStoreItem;
     * if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
     * logEndOperation("completeCustomPage", "", foodTruckResult, foodTruckParam);
     * }
     */
    return foodTruckResult;
  }

  /**
   * @param foodTruckParam
   * @param apiSession
   * @return
   */
  public static FoodTruckResult downloadAndInstallCustomPage(final FoodTruckParam foodTruckParam, final APISession apiSession) {
      final FoodTruckResult foodTruckResult = new FoodTruckResult("getListCustomPage");
      try {
    ArtifactFoodTruck artefactFoodTruck = foodTruckParam.artifactFoodTruck;
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logBeginOperation("downloadAndInstallCustomPage", " urlDownload[" + artefactFoodTruck.artifact.getLastUrlDownload() + "]", foodTruckParam);
    }

    
    if (foodTruckParam.artifactFoodTruck == null) {
      // give a appsItem !
      foodTruckResult.addEvent(new BEvent(serverAPIError, "No Artefact given !"));
      if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
        logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
      }
      return foodTruckResult;

    }

    if (artefactFoodTruck.artifact.getLastUrlDownload() == null) {
      final FoodTruckResult complete = completeCustomPage(foodTruckParam, apiSession);
      foodTruckResult.addEvents(complete.getEvents());
      artefactFoodTruck = complete.listStoreItem.size() == 0 ? artefactFoodTruck : ArtifactFoodTruck.getInstance( complete.listStoreItem.get(0) );
      if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
        foodTruckParam.logBox.log(LOGLEVEL.INFO, "foodTruck.downloadAndInstallCustomPage: After complete [" + artefactFoodTruck.getName() + "] urlDownload[" + artefactFoodTruck.artifact.getLastUrlDownload() + "]");
      }

    }
    PageAPI pageAPI;
    

    try {
      pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);
    
    } catch (ServerAPIException | UnknownAPITypeException | BonitaHomeNotSetException e) {
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Error creating api");
      foodTruckResult.addEvent(new BEvent(serverAPIError, e, ""));
      if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
        logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
      }

      return foodTruckResult;
    }

    /*
     * if (foodTruckParam.appsItem.sourceFoodTruckStoreGithub == null) {
     * final FoodTruckResult foodTruckResultSearch =
     * searchAppsByNameInRepository(appsItem.getAppsName(), foodTruckParam);
     * foodTruckResult.addEvents(foodTruckResultSearch.getEvents());
     * if (foodTruckResultSearch.listStoreItem.size() == 0) {
     * // we can't found this apps anymore !
     * } else {
     * foodTruckParam.appsItem.sourceFoodTruckStoreGithub =
     * foodTruckResultSearch.listStoreItem.get(0).sourceFoodTruckStoreGithub;
     * }
     * }
     */
    if (foodTruckParam.artifactFoodTruck.artifact != null && foodTruckParam.artifactFoodTruck.artifact.getStore() != null) {
      foodTruckResult.statuscustompage = AppsStatus.INDOWNLOAD;

      // The apps is part of the foodTruckParam
      LoggerStore LoggerStore = new LoggerStore();

      BonitaStoreResult storeResultDownload = foodTruckParam.artifactFoodTruck.artifact.getStore().downloadArtefact(foodTruckParam.artifactFoodTruck.artifact, UrlToDownload.LASTRELEASE, LoggerStore);
      // merge
      foodTruckResult.addEvents(storeResultDownload.getEvents());
      foodTruckResult.contentByte = storeResultDownload.contentByte;

      if (BEventFactory.isError(foodTruckResult.getEvents())) {
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
          logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
        }
        foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;

        return foodTruckResult;
      }

    }
    /**
     * too early
     * if (foodTruckResult.contentByte != null) {
     * foodTruckResult.statuscustompage = AppsStatus.OK;
     * }
     */

    // test
    if (foodTruckParam.saveDownloadFileLocaly) {
      FileOutputStream file;
      try {
        if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
          foodTruckParam.logBox.log(LOGLEVEL.INFO, "FoodTruckAPI.downloadAndInstallCustomPage: save file [" + foodTruckParam.directoryFileLocaly + "/" + foodTruckParam.artifactFoodTruck.getName() + ".zip" + "]");
        }
        if (foodTruckResult.contentByte != null) {
          file = new FileOutputStream(foodTruckParam.directoryFileLocaly + "/" + foodTruckParam.artifactFoodTruck.getName() + ".zip");
          file.write(foodTruckResult.contentByte);
          file.close();

        } else {
          foodTruckResult.addEvent(new BEvent(noContentAvailable, "Apps " + artefactFoodTruck.getName() + "]"));
        }

      } catch (final IOException e) {
        foodTruckResult.addEvent(new BEvent(saveLocalFile, e, "File [" + foodTruckParam.directoryFileLocaly + foodTruckParam.artifactFoodTruck.getName() + "]"));
        foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;

      }
    }

    // first delete an exiting page


      Page currentPage = null;
      try {
        currentPage = pageAPI.getPageByName(foodTruckParam.artifactFoodTruck.getName());
        // getPageByName does not work : search manually
        /*
         * final SearchResult<Page> searchResult =
         * pageAPI.searchPages(new SearchOptionsBuilder(0,
         * 1000).done()); for (final Page page :
         * searchResult.getResult()) { if
         * (page.getName().equalsIgnoreCase(foodTruckParam.appsItem.
         * getAppsName())) { pageAPI.deletePage(page.getId()); } }
         */

      } catch (final Exception e) {
      }

      /**
       * EXIT
       */
      
      if (currentPage != null) {
        if (foodTruckParam.allowUpdatePage) {
          pageAPI.updatePageContent(currentPage.getId(), foodTruckResult.contentByte);
          final Artifact appsItemFromPage = new ArtifactCustomPage(currentPage, null);
          foodTruckResult.listArtifacts.add( ArtifactFoodTruck.getInstance( appsItemFromPage) );
        } else {
          // update not allow
          foodTruckResult.addEvent(new BEvent(notAllowToUpdatePage, "Apps [" + currentPage.getName() + "]"));
        }
      } else // new
      {
        final Page page = pageAPI.createPage(foodTruckParam.artifactFoodTruck.getName(), foodTruckResult.contentByte);
        final Artifact appsItemFromPage = new ArtifactCustomPage(page, null);
        foodTruckResult.listArtifacts.add( ArtifactFoodTruck.getInstance( appsItemFromPage) );
      }
      // if we arrive here, that's mean the application is now installed
      foodTruckResult.statuscustompage = AppsStatus.OK;
      foodTruckResult.isAllowAddProfile = true;

    } catch (final AlreadyExistsException e) {
      foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
      foodTruckResult.addEvent(new BEvent(pageAlreadyExist, "Name:" + e.getName() + ", Message:" + e.getMessage()));
      foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;

    } catch (final InvalidPageTokenException e) {
      foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
      foodTruckResult.addEvent(new BEvent(invalidePageToken, "Message:" + e.getMessage()));
      foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
    } catch (final InvalidPageZipContentException e) {
      foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
      foodTruckResult.addEvent(new BEvent(invalidZipContent, "Message:" + e.getMessage()));
      foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
    } catch (final CreationException e) {
      foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
      foodTruckResult.addEvent(new BEvent(creationException, "Message:" + e.getMessage()));
      foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
    } catch (final UpdatingWithInvalidPageTokenException e) {
      foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
      foodTruckResult.addEvent(new BEvent(updateError, "Message:" + e.getMessage()));
      foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
    } catch (final UpdatingWithInvalidPageZipContentException e) {
      foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
      foodTruckResult.addEvent(new BEvent(invalidZipContent, "Message:" + e.getMessage()));
      foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
    } catch (final UpdateException e) {
      foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
      foodTruckResult.addEvent(new BEvent(updateError, "Message:" + e.getMessage()));
      foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
    } catch(Exception e) {
        foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAPI.downloadAndInstallCustomPage: exception " + e.toString());
        foodTruckResult.addEvent(new BEvent(updateError, e, "" ));
        foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
        
    }

    // final
    // foodTruckStoreGithub.downloadOneCustomPagefoodTruckParam.appsName
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
    }

    return foodTruckResult;

  }

  /**
   * Return the list of all local resource.
   * Should be done via the BonitaStore too (local BonitaStore)
   * 
   * @param ldapSynchronizerPath
   * @param mDomain
   * @return
   */
  public static FoodTruckResult getListLocalResources(final FoodTruckParam foodTruckParam, final PageAPI pageAPI, final ProfileAPI profileAPI) {
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logBeginOperation("getListLocalResources", " in profile [" + foodTruckParam.profileid + "]", foodTruckParam);
    }

    final FoodTruckResult foodTruckResult = new FoodTruckResult("ListResource");

    // get list of pages
    if (foodTruckParam.typeArtifact==null || TypeArtifact.CUSTOMPAGE.equals( foodTruckParam.typeArtifact)) {
      Long profileId = null;

      try {

        SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, foodTruckParam.searchMaxResources);
        final SearchResult<Page> searchResultPage = pageAPI.searchPages(searchOptionsBuilder.done());
        for (final Page page : searchResultPage.getResult()) {
          if ("page".equals(page.getContentType()) && (foodTruckParam.typeArtifact==null || TypeArtifact.CUSTOMPAGE.equals( foodTruckParam.typeArtifact)))  {
              final Artifact apps = new ArtifactCustomPage(page, null);
              foodTruckResult.listArtifacts.add( ArtifactFoodTruck.getInstance( apps ));
          }
        }

        // search all profile, and populate the page
        searchOptionsBuilder = new SearchOptionsBuilder(0, foodTruckParam.searchMaxResources);
        SearchResult<ProfileEntry> searchResultProfile;

        searchResultProfile = profileAPI.searchProfileEntries(searchOptionsBuilder.done());

        for (final ProfileEntry profileEntry : searchResultProfile.getResult()) {
          final String name = profileEntry.getPage();
          profileId = profileEntry.getProfileId();
          final ArtifactFoodTruck appsItem = foodTruckResult.getArtefactByName(name);
          if (appsItem != null) {
            final Profile profile = profileAPI.getProfile(profileId);
            appsItem.addOneProfile(profile);
          }
        }
      } catch (final SearchException e) {
        foodTruckParam.logBox.logException("FoodTruckAPI. Error during read", e);
        foodTruckResult.addEvent(new BEvent(eventSearchException, e, "Max Result:" + foodTruckParam.searchMaxResources));
      } catch (final ProfileNotFoundException e) {
        foodTruckParam.logBox.logException("FoodTruckAPI. Error during read", e);
        foodTruckResult.addEvent(new BEvent(eventProfileNotFound, e, "ProfileId:" + profileId));
      }
    }
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.DEBUG)) {
      logEndOperation("getListLocalResources", "", foodTruckResult, foodTruckParam);
    }

    return foodTruckResult;
  }

  /**
   * add a page in the profile
   *
   * @param foodTruckParam
   * @param apiSession
   * @return
   */
  public static FoodTruckResult addInProfile(final FoodTruckParam foodTruckParam, final APISession apiSession) {

    final FoodTruckResult foodTruckResult = new FoodTruckResult("addInprofile");
    try {
      final com.bonitasoft.engine.api.ProfileAPI comProfileAI = com.bonitasoft.engine.api.TenantAPIAccessor.getProfileAPI(apiSession);
      final Profile profile = comProfileAI.getProfile(foodTruckParam.profileid);
      logBeginOperation("addInprofile", " in profile name[" + profile.getName() + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);

      final ArtifactFoodTruck artifactFoodtruck = foodTruckParam.artifactFoodTruck;

      /*
       * final ProfileEntry profileEntry =
       * comProfileAI.createProfileEntry(appsItem.displayName,
       * appsItem.description, foodTruckParam.profileid, "link",
       * appsItem.getAppsName());
       */
      final ProfileEntryCreator profileEntryCreator = new ProfileEntryCreator(foodTruckParam.profileid);
      profileEntryCreator.setPage(artifactFoodtruck.getName());
      profileEntryCreator.setParentId(0L);
      profileEntryCreator.setType("link");
      profileEntryCreator.setCustom(true);
      profileEntryCreator.setName(artifactFoodtruck.artifact.getDisplayName() == null ? artifactFoodtruck.getName() : artifactFoodtruck.artifact.getDisplayName());
      final ProfileEntry profileEntry = comProfileAI.createProfileEntry(profileEntryCreator);

      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.INFO, "createProfileEntry: profileId[" + foodTruckParam.profileid + "] appsName[" + artifactFoodtruck.getName() + "] ParentId[OL] Type[link] Custom[true] displayname[" + artifactFoodtruck.artifact.getDisplayName() + "] ==> PromeEntry[" + profileEntry.getId() + "]");

      artifactFoodtruck.addOneProfile(profile);

      final ProfileAPI profileAI = TenantAPIAccessor.getProfileAPI(apiSession);
      refreshListProfiles(foodTruckParam.artifactFoodTruck, profileAI, foodTruckParam.logBox);

      foodTruckResult.listArtifacts.add(artifactFoodtruck);
      foodTruckResult.statusinfo = "Profile " + profile.getName() + " added";

      foodTruckResult.profileEntry = profileEntry.getId();

      foodTruckParam.logBox.log(LOGLEVEL.INFO, "AddInprofile apps[" + artifactFoodtruck.getName() + "]");
    } catch (BonitaHomeNotSetException | ServerAPIException | UnknownAPITypeException e1) {
      foodTruckResult.addEvent(new BEvent(serverAPIError, e1, ""));
    } catch (final CreationException e) {
      foodTruckResult.addEvent(new BEvent(failRegisterPageInProfile, e, "profileId[" + foodTruckParam.profileid + "] Page[" + foodTruckParam.artifactFoodTruck.getName() + "]"));
    } catch (final ProfileNotFoundException e) {
      logBeginOperation("addInprofile", " in profile Id[" + foodTruckParam.profileid + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);
      foodTruckResult.addEvent(new BEvent(eventProfileNotFound, "profileId [" + foodTruckParam.profileid + "]"));
    } catch (final ClassCastException e) {
      final StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Class " + sw.toString());

      foodTruckResult.addEvent(comProfileAPIRequested);
    } catch (final Exception e) {
      final StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Exception  " + sw.toString());

      foodTruckResult.addEvent(comProfileAPIRequested);
    }
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logEndOperation("addInprofile", "", foodTruckResult, foodTruckParam);
    }

    return foodTruckResult;

  }

  /**
   * remove one apps in a profile
   *
   * @param foodTruckParam
   * @param apiSession
   * @return
   */
  public static FoodTruckResult removeFromProfile(final FoodTruckParam foodTruckParam, final APISession apiSession) {

    final FoodTruckResult foodTruckResult = new FoodTruckResult("removeInprofile");
    try {
      final com.bonitasoft.engine.api.ProfileAPI comProfileAI = com.bonitasoft.engine.api.TenantAPIAccessor.getProfileAPI(apiSession);
      
      final ArtifactFoodTruck artifactFoodtruck = foodTruckParam.artifactFoodTruck;

      final Profile profile = comProfileAI.getProfile(foodTruckParam.profileid);
      logBeginOperation("removeInprofile", " in profile name[" + profile.getName() + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);

      final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 100);
      searchOptionsBuilder.filter(ProfileEntrySearchDescriptor.PROFILE_ID, foodTruckParam.profileid);
      searchOptionsBuilder.filter(ProfileEntrySearchDescriptor.PAGE, foodTruckParam.artifactFoodTruck.getName());

      final SearchResult<ProfileEntry> searchResult = comProfileAI.searchProfileEntries(searchOptionsBuilder.done());
      for (final ProfileEntry profileEntry : searchResult.getResult()) {
        comProfileAI.deleteProfileEntry(profileEntry.getId());
      }

      foodTruckResult.statusinfo = "Profile " + profile.getName() + " removed";

      // recalcul this apps
      final ProfileAPI profileAI = TenantAPIAccessor.getProfileAPI(apiSession);
      refreshListProfiles(artifactFoodtruck, profileAI, foodTruckParam.logBox);
      foodTruckResult.listArtifacts.add(artifactFoodtruck);
      
    } catch (BonitaHomeNotSetException | ServerAPIException | UnknownAPITypeException e1) {
      foodTruckResult.addEvent(new BEvent(serverAPIError, e1, ""));
    } catch (final ProfileNotFoundException e) {
      logBeginOperation("removeFromProfile", " in profile Id[" + foodTruckParam.profileid + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);
      foodTruckResult.addEvent(new BEvent(eventProfileNotFound, "profileId [" + foodTruckParam.profileid + "]"));
    } catch (final ClassCastException e) {
      final StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Class " + sw.toString());

      foodTruckResult.addEvent(comProfileAPIRequested);
    } catch (final Exception e) {
      final StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.ERROR, "Class " + sw.toString());

      foodTruckResult.addEvent(comProfileAPIRequested);
    }
    if (foodTruckParam.logBox.isLog(LoggerStore.LOGLEVEL.MAIN)) {
      logEndOperation("removeInprofile", "", foodTruckResult, foodTruckParam);
    }

    return foodTruckResult;

  }

  /* ******************************************************************************** */
  /*                                                                                  */
  /* private     */
  /*                                                                                  */
  /*                                                                                  */
  /* ******************************************************************************** */


  /**
   * search an apps in directory from it's name
   *
   * @param appsName
   * @param foodTruckParam
   * @return
   */
  /*
   * private static FoodTruckResult searchAppsByNameInRepository(final String appsName, final
   * FoodTruckParam foodTruckParam) {
   * final FoodTruckResult foodTruckResult = new FoodTruckResult("searchAppsByNameInRepository");
   * final FoodTruckStoreFactory foodTruckStoreFactory = FoodTruckStoreFactory.getInstance();
   * for (final FoodTruckDefStore bonitaStore : foodTruckParam.listRepository) {
   * final FoodTruckIntBonitaStore foodTruckStoreGithub =
   * foodTruckStoreFactory.getFoodTruckStore(bonitaStore);
   * final FoodTruckResult storeResult =
   * foodTruckStoreGithub.getListAvailableArtefacts(getListTypeApps(), false,
   * foodTruckParam.logBox);
   * foodTruckResult.addEvents(storeResult.getEvents());
   * // Exist in this repository ?
   * for (final Artefact storeApp : storeResult.listCustomPage) {
   * if (storeApp.getAppsName().equals(appsName)) {
   * foodTruckResult.listStoreItem.add(storeApp);
   * return foodTruckResult;
   * }
   * }
   * }
   * return foodTruckResult;
   * }
   */
  /*
   * *************************************************************************
   * *******
   */
  /*                                                                                                                                                                  */
  /* Log method */
  /*                                                                                                                                                                  */
  /*
   * *************************************************************************
   * *******
   */

  /**
   * normalize beginLog
   *
   * @param method
   * @param message
   * @param foodTruckParam
   */
  private static void logBeginOperation(final String method, final String message, final FoodTruckParam foodTruckParam) {
    foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.MAIN, "~~~~~~~~ START FoodTruckAPI." + method + ": " + message + (foodTruckParam.artifactFoodTruck != null ? " Apps[" + foodTruckParam.artifactFoodTruck.getName() + "]" : ""));
  }

  /**
   * logEndOperation
   *
   * @param method
   * @param message
   * @param foodTruckResult
   */
  private static void logEndOperation(final String method, final String message, final FoodTruckResult foodTruckResult, final FoodTruckParam foodTruckParam) {
    foodTruckResult.endOperation();
    foodTruckParam.logBox.log(LoggerStore.LOGLEVEL.MAIN, "~~~~~~~~ END FoodTruckAPI." + method + ": " + foodTruckResult.getTimeOperation() + " ms status [" + foodTruckResult.statusinfo + "] " + message + " events[" + foodTruckResult.getEvents() + "] content[" + foodTruckResult.toMap() + "]");
  }

  private static void refreshListProfiles(final ArtifactFoodTruck appsItem, final ProfileAPI profileAPI, final LoggerStore logBox) {
    final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 100);
    searchOptionsBuilder.filter(ProfileEntrySearchDescriptor.PAGE, appsItem.getName());
    SearchResult<ProfileEntry> searchResultProfile;
    try {
      searchResultProfile = profileAPI.searchProfileEntries(searchOptionsBuilder.done());
      appsItem.clearProfiles();

      for (final ProfileEntry profileEntry : searchResultProfile.getResult()) {
        final Long profileId = profileEntry.getProfileId();
        final Profile profile = profileAPI.getProfile(profileId);
        appsItem.addOneProfile(profile);
      }
    } catch (SearchException | ProfileNotFoundException e) {
      logBox.log(LoggerStore.LOGLEVEL.ERROR, "Error during recalcul profile for Apps [" + appsItem.getName() + "]");
    }
  }

  /**
   * type supported by the foodtruck
   * 
   * @return
   */
  private static List<TypeArtifact> getListTypeApps(FoodTruckParam foodTruckParam) {
    List<TypeArtifact> listTypeApps = new ArrayList<TypeArtifact>();
    if (foodTruckParam.typeArtifact ==null) {
        listTypeApps.add(TypeArtifact.CUSTOMPAGE);
        listTypeApps.add(TypeArtifact.RESTAPI);
        listTypeApps.add(TypeArtifact.CUSTOMWIDGET);
    }
    else
        listTypeApps.add(foodTruckParam.typeArtifact);

    return listTypeApps;
  }

}
