package com.bonitasoft.custompage.foodtruck;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.InvalidPageTokenException;
import org.bonitasoft.engine.exception.InvalidPageZipContentException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.scheduler.impl.BonitaSchedulerFactory;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreAPI;
import org.bonitasoft.store.BonitaStoreResult;
import org.bonitasoft.store.BonitaStore.DetectionParameters;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.toolbox.LoggerStore;


import com.bonitasoft.custompage.foodtruck.ArtifactFoodTruck;
import com.bonitasoft.custompage.foodtruck.FoodTruckAPI;
import com.bonitasoft.custompage.foodtruck.FoodTruckResult;

import com.bonitasoft.custompage.foodtruck.ArtifactFoodTruck.AppsStatus;
import com.bonitasoft.custompage.foodtruck.FoodTruckAPI.FilterStatusEnum;
import com.bonitasoft.custompage.foodtruck.FoodTruckAPI.FoodTruckParam;



class JunitFoodTruck {


    // foodTruckParam.githubUserName = "pierre-yves-monnet";
    // foodTruckParam.githubPassword = "pierreyvesforgithub";

    // @Test

    static Logger logger = Logger.getLogger(JunitFoodTruck.class.getName());

    @Test
    public void testGithubCommunity() {
      final String jsonSt = "{\"showlocal\":true,\"showBonitaAppsStore\":true,\"github\":{\"login\":\"Pierre-yves-monnet\",\"password\":\"pierreyvesforgithub\"},\"status\":\"\",\"show\":\"ALL\",  \"loglevel\": \"INFO\"}";
      final FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt(jsonSt);

      final APISession apiSession = login();
      if (apiSession == null) {
        System.out.println("Can't connect");
        return;
      }
      try {
        BonitaStoreAPI bonitaStoreAPI = new BonitaStoreAPI();
        foodTruckParam.addInListRepository(bonitaStoreAPI.getBonitaCommunityStore( true ));

        foodTruckParam.saveLogoFileLocaly = true;
        foodTruckParam.directoryFileLocaly = "c:/temp";

        System.out.println("-------------- Github Community");

        final FoodTruckResult foodTruckResult = FoodTruckAPI.getListArtifacts(foodTruckParam, apiSession);
        System.out.println(foodTruckResult.toMap());

        System.out.println("-------------- Result operation");
        for (final BEvent event : foodTruckResult.getEvents()) {
          System.out.println("  " + event.toString());
        }
        System.out.println("-------------- End result operation");

        for (final ArtifactFoodTruck customPage : foodTruckResult.listArtifacts) {
          String profileSt = "";
         
          // logApps("ALL", customPage);
          System.out.println("                 Profile[" + profileSt + "]");
        }

        System.out.println("-------------- end dahsboard");
      } catch (final Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    @Test
    public void testFoodTruckStoreGithub() {
      BonitaStoreAPI bonitaStoreAPI = new BonitaStoreAPI();
      BonitaStore bonitaStore = bonitaStoreAPI.getBonitaCommunityStore( true );

      final LoggerStore loggerBox = new LoggerStore();
      DetectionParameters detectionParameters = new DetectionParameters();
      detectionParameters.listTypeArtifact.add(TypeArtifact.CUSTOMPAGE);

      // final FoodTruckResult storeResult = bonitaStore.getListAvailableArtefacts(getListTypeApps(), false, foodTruckParam.logBox);
      BonitaStoreResult storeResult = bonitaStore.getListArtefacts(detectionParameters, loggerBox);

      System.out.print(storeResult.getEvents());
      for (final Artifact artefact : storeResult.listArtifacts) {
          ArtifactFoodTruck appsItem = new ArtifactFoodTruck();
                  appsItem.artifact = artefact;
        logApps("STORE ", appsItem);
      }
      // now upload one element
    }

    // @Test
    public void testDownloadAndInstallQuick() {
      final APISession apiSession = login();
      if (apiSession == null) {
        System.out.println("Can't connect");
        return;
      }

      // first, get an app
      // final FoodTruckResult storeResult = foodTruckStoreGithub.getListAvailableApps();

      String json = "{\"appsname\":\"custompage_americain\", ";
      json += "\"appisid\":-1,\"status\":\"INDOWNLOAD\",";
      json += "\"urldownload\":\"https://github.com/Bonitasoft-Community/page_americain/releases/download/1.0/page_americain.20160331.zip\",";
      json += "\"storegithubname\":\"https://api.github.com/orgs/Bonitasoft-Community\",";
      json += "\"github\":{\"login\":\"Pierre-yves-monnet\",\"password\":\"pierreuvesforgithuib\"}}";

      final FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt(json);

      foodTruckParam.directoryFileLocaly = "c:/temp";
      foodTruckParam.saveDownloadFileLocaly = true;

      final FoodTruckResult foodTruckResultApps = FoodTruckAPI.downloadAndInstallCustomPage(foodTruckParam, apiSession);
      System.out.println("Result=" + foodTruckResultApps.toString());

    }

    // @Test
    public void testDownloadAndInstall() {
      final APISession apiSession = login();
      if (apiSession == null) {
        System.out.println("Can't connect");
        return;
      }

      // first, get an app
      // final FoodTruckResult storeResult = foodTruckStoreGithub.getListAvailableApps();

      final FoodTruckParam foodTruckParam = new FoodTruckParam();

      // foodTruckParam.listRepository.add(foodTruckParam.getCommunityRepository());
      BonitaStoreAPI bonitaStoreAPI = new BonitaStoreAPI();
      BonitaStore bonitaStore = bonitaStoreAPI.getBonitaCommunityStore(true)  ;

      foodTruckParam.addInListRepository(bonitaStore);

      foodTruckParam.filterStatus = FilterStatusEnum.ALL;
      foodTruckParam.saveLogoFileLocaly = true;
      foodTruckParam.directoryFileLocaly = "c:/temp";

      final FoodTruckResult foodTruckResult = FoodTruckAPI.getListArtifacts(foodTruckParam, apiSession);
      int count = 0;
      System.out.println("testDownloadAndInstall : nbList[" + foodTruckResult.listArtifacts.size() + " -  listCustomPageEvent:"
          + foodTruckResult.getEvents());
      System.out.println("    JSON= " + foodTruckResult.toMap());
      for (final ArtifactFoodTruck appsItem : foodTruckResult.listArtifacts) {
        if (appsItem.status == AppsStatus.LOCAL) {
          System.out.println("LOCAL:" + appsItem.getName() + " (" + appsItem.artifact.getDisplayName() + ")");
          continue;
        }
        logApps("STORE ", appsItem);

        foodTruckParam.saveDownloadFileLocaly = true;
        foodTruckParam.directoryFileLocaly = "c:/temp/";

        foodTruckParam.artifactFoodTruck = appsItem;
        if ((appsItem.status == AppsStatus.NEW || appsItem.status == AppsStatus.OK || appsItem.status == AppsStatus.TOUPDATE)
            && !appsItem.getName().equals("custompage_foodtruck")) {

          final FoodTruckResult foodTruckResultApps = FoodTruckAPI.downloadAndInstallCustomPage(foodTruckParam, apiSession);
          logApps("NEW IN STORE ", appsItem);

          count++;
          if (count > 1) {
            break;
          }
        } else {
          System.out.println("    ..ignore");
        }
      }
      // now upload one element
    }

    @Test
    public void testListCustompage() {
      final FoodTruckParam foodTruckParam = new FoodTruckParam();
      final APISession apiSession = login();
      if (apiSession == null) {
        System.out.println("Can't connect");
        return;
      }
      PageAPI pageAPI;
      try {
        pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);

        final ProfileAPI profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);

        final FoodTruckResult statusOperation = FoodTruckAPI.getListLocalResources(foodTruckParam, pageAPI, profileAPI);
        System.out.println(statusOperation.toMap());

        if (statusOperation.listArtifacts != null) {
          for (final ArtifactFoodTruck customPage : statusOperation.listArtifacts) {
            String profileSt = "";

            logApps("STORE", customPage);
            System.out.println("      Profile[" + profileSt + "]");
          }

        }

      } catch (final BonitaHomeNotSetException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (final ServerAPIException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (final UnknownAPITypeException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (final Error er) {
        er.printStackTrace();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    // @ T e s t
    public void testDeployCustomPage() {
      logger.info("------------------------- TestDeployCustomPage");
      final APISession apiSession = login();
      if (apiSession == null) {
        System.out.println("Can't connect");
        return;
      }
      try {

        final PageAPI pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);

        final FileInputStream in = new FileInputStream("C:/git/bonita-internal-contrib/Custom_Pages/FoodTruck/test/custompage_towtruck Github.zip");
        final byte[] buff = new byte[8000];

        int bytesRead = 0;

        final ByteArrayOutputStream bao = new ByteArrayOutputStream();

        while ((bytesRead = in.read(buff)) != -1) {
          bao.write(buff, 0, bytesRead);
        }

        final byte[] contentByte = bao.toByteArray();

        final Page page = pageAPI.createPage("testfoodtruck", contentByte);
        System.out.println("PageId =" + page.getId());

      } catch (final AlreadyExistsException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      } catch (final InvalidPageTokenException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      } catch (final InvalidPageZipContentException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      } catch (final CreationException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      } catch (final BonitaHomeNotSetException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      } catch (final ServerAPIException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      } catch (final UnknownAPITypeException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      } catch (final IOException e) {
        logger.severe("FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
      }
    }

    // @Test
    public void testRegisterpageInProfile() {
      System.out.println("-------------- testRegisterpageInProfile");
      final FoodTruckParam foodTruckParam = new FoodTruckParam();
      final APISession apiSession = login();
      if (apiSession == null) {
        System.out.println("Can't connect");
        return;
      }
      BonitaStoreAPI bonitaStoreAPI = new BonitaStoreAPI();
      foodTruckParam.addInListRepository(bonitaStoreAPI.getBonitaCommunityStore( true ));

      foodTruckParam.artifactFoodTruck = new ArtifactFoodTruck();

      final FoodTruckResult statusOperation = FoodTruckAPI.getListArtifacts(foodTruckParam, apiSession);

      for (final ArtifactFoodTruck customPage : statusOperation.listArtifacts) {
        foodTruckParam.artifactFoodTruck = customPage;

        for (final Profile profile : statusOperation.allListProfiles) {
          boolean exist = false;
         
        }
      }
    }

    // @Test
    public void checkGithub() {
      // final ResultLastContrib resultLastContrib = GithubAccessor.getLastContribReleaseAsset("PierrickVouletBonitasoft", "pwd");
      BonitaStoreAPI bonitaStoreAPI = new BonitaStoreAPI();
      BonitaStore bonitaStore = bonitaStoreAPI.getBonitaCommunityStore( true );
      final LoggerStore loggerBox = new LoggerStore();

      BonitaStoreResult storeResult = bonitaStore.ping(loggerBox);
      System.out.println(storeResult.statusDetails);

      assert (!BEventFactory.isError(storeResult.getEvents()));

    }

    public APISession login() {
      final Map<String, String> map = new HashMap<String, String>();
      map.put("server.url", "http://localhost:8080");
      map.put("application.name", "bonita");
      APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

      // Set the username and password
      // final String username = "helen.kelly";
      final String username = "walter.bates";
      final String password = "bpm";

      // get the LoginAPI using the TenantAPIAccessor
      LoginAPI loginAPI;
      try {
        loginAPI = TenantAPIAccessor.getLoginAPI();
        // log in to the tenant to create a session
        final APISession session = loginAPI.login(username, password);
        return session;
      } catch (final Exception e) {
        logger.severe("during login " + e.toString());
      }
      return null;
    }

    private void logApps(final String label, final ArtifactFoodTruck appsItem) {
      System.out.println(">>>>>> " + label + " : " + appsItem.getName() + " (" + appsItem.artifact.getDisplayName() + ") Status["
          + appsItem.status + "]");
      System.out.println("                               What's new  : [" + appsItem.artifact.getWhatsnews() + "]");
      System.out.println("                               Description  : [" + appsItem.artifact.getDescription() + "]");
      System.out.println("                               nbDownload: [" + appsItem.artifact.getNumberOfDownload() + "]");
      System.out.println("                               UrlDownload  : [" + appsItem.artifact.getLastUrlDownload() + "] realaseDate ["
          + appsItem.artifact.getLastReleaseDate() + "]");

    }

    // @Test
    public void testDashBoard() {
      final FoodTruckParam foodTruckParam = new FoodTruckParam();
      final APISession apiSession = login();
      if (apiSession == null) {
        System.out.println("Can't connect");
        return;
      }
      try {
        BonitaStoreAPI bonitaStoreAPI = new BonitaStoreAPI();
        BonitaStore bonitaStore = bonitaStoreAPI.getBonitaCommunityStore( true );

        foodTruckParam.addInListRepository(bonitaStore);

        foodTruckParam.saveLogoFileLocaly = true;
        foodTruckParam.directoryFileLocaly = "c:/temp";

        System.out.println("-------------- Dahsboard");

        final FoodTruckResult statusOperation = FoodTruckAPI.getListArtifacts(foodTruckParam, apiSession);
        System.out.println(statusOperation.toMap());
        for (final ArtifactFoodTruck customPage : statusOperation.listArtifacts) {
          String profileSt = "";
          

          System.out.println(customPage.status +  ":" + customPage.getName() + " (" + customPage.artifact.getDisplayName() + ") - "
              + " provided:" + customPage.artifact.isProvided()
              + " " + customPage.artifact.getLastReleaseDate() + " - desc[" + customPage.artifact.getDescription() + "] Profile[" + profileSt + "]");
        }
        System.out.println("-------------- end dahsboard");
      } catch (final Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

