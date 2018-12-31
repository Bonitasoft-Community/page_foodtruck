package com.bonitasoft.custompage.foodtruck;

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
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.junit.Test;

import com.bonitasoft.custompage.foodtruck.AppsItem.AppsStatus;
import com.bonitasoft.custompage.foodtruck.AppsItem.TypeArtefacts;
import com.bonitasoft.custompage.foodtruck.FoodTruckAccess.FilterEnum;
import com.bonitasoft.custompage.foodtruck.FoodTruckAccess.FoodTruckParam;
import com.bonitasoft.custompage.foodtruck.FoodTruckStoreFactory.FoodTruckDefStore;
import com.bonitasoft.custompage.foodtruck.Toolbox.FoodTruckResult;
import com.bonitasoft.custompage.foodtruck.github.GithubAccessor;
import com.bonitasoft.custompage.foodtruck.github.GithubAccessor.ResultGithub;

public class JUnitFoodTruck {

    // foodTruckParam.githubUserName = "pierre-yves-monnet";
    // foodTruckParam.githubPassword = "pierreyvesforgithub";




    // @Test

    static Logger logger = Logger.getLogger(JUnitFoodTruck.class.getName());



    @Test
    public void testGithubCommunity() {
        final String jsonSt = "{\"showlocal\":true,\"showBonitaAppsStore\":true,\"github\":{\"login\":\"Pierre-yves-monnet\",\"password\":\"pierreyvesforgithub\"},\"status\":\"\",\"show\":\"ALL\",  \"loglevel\": \"INFO\"}";
        final FoodTruckParam foodTruckParam = FoodTruckParam.getInstanceFromJsonSt(jsonSt);

        final APISession apiSession = login();
        if (apiSession == null)
        {
            System.out.println("Can't connect");
            return;
        }
        try {

            foodTruckParam.listRepository.add(foodTruckParam.getCommunityRepository());

            foodTruckParam.saveLogoFileLocaly = true;
            foodTruckParam.directoryFileLocaly = "c:/temp";

            System.out.println("-------------- Github Community");

            final FoodTruckResult statusOperation = FoodTruckAccess.getListCustomPage(foodTruckParam, apiSession);
            System.out.println(statusOperation.toMap());

            System.out.println("-------------- Result operation");
            for (final BEvent event : statusOperation.getEvents())
            {
                System.out.println("  " + event.toString());
            }
            System.out.println("-------------- End result operation");

            for (final AppsItem customPage : statusOperation.listCustomPage) {
                String profileSt = "";
                final List<Map<String, Object>> listProfiles = customPage.getListProfiles();
                if (listProfiles != null) {
                    for (final Map<String, Object> profile : listProfiles) {
                        profileSt += profile.toString() + ",";
                    }
                }
                logApps("ALL", customPage);
                System.out.println("                 Profile[" + profileSt + "]");
            }

            System.out.println("-------------- end dahsboard");
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    // @ T e s t
    public void testFoodTruckStoreGithub() {
        final FoodTruckStoreGithub foodTruckStoreGithub = new FoodTruckStoreGithub(FoodTruckStoreFactory.CommunityGithubUserName, FoodTruckStoreFactory.CommunityGithubPassword,
                FoodTruckStoreFactory.CommunityGithubUrlRepository);

        final LogBox logBox = new LogBox();
        List<TypeArtefacts> listTypeApps = new ArrayList<TypeArtefacts>();
        listTypeApps.add(TypeArtefacts.CUSTOMPAGE);
        final FoodTruckResult storeResult = foodTruckStoreGithub.getListAvailableApps(listTypeApps, true, logBox);
        System.out.print(storeResult.getEvents());
        for (final AppsItem appsItem : storeResult.listCustomPage)
        {
            logApps("STORE ", appsItem);
        }
        // now upload one element
    }

    // @Test
    public void testDownloadAndInstallQuick() {
        final APISession apiSession = login();
        if (apiSession == null)
        {
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
        foodTruckParam.listRepository.add(FoodTruckDefStore.getGithub(FoodTruckStoreFactory.CommunityGithubUrlRepository, "Pierre-yves-monnet", "pierreyvesforgithub"));
        final FoodTruckResult foodTruckResultApps = FoodTruckAccess.downloadAndInstallCustomPage(foodTruckParam, apiSession);
        System.out.println("Result=" + foodTruckResultApps.toString());

    }

    // @Test
    public void testDownloadAndInstall() {
        final APISession apiSession = login();
        if (apiSession == null)
        {
            System.out.println("Can't connect");
            return;
        }

        // first, get an app
        // final FoodTruckResult storeResult = foodTruckStoreGithub.getListAvailableApps();

        final FoodTruckParam foodTruckParam = new FoodTruckParam();

        // foodTruckParam.listRepository.add(foodTruckParam.getCommunityRepository());

        foodTruckParam.listRepository.add(FoodTruckDefStore.getGithub(FoodTruckStoreFactory.CommunityGithubUrlRepository, "Pierre-yves-monnet", "pierreyvesforgithub"));

        foodTruckParam.filter = FilterEnum.ALL;
        foodTruckParam.saveLogoFileLocaly = true;
        foodTruckParam.directoryFileLocaly = "c:/temp";

        final FoodTruckResult foodTruckResult = FoodTruckAccess.getListCustomPage(foodTruckParam, apiSession);
        int count = 0;
        System.out.println("testDownloadAndInstall : nbList[" + foodTruckResult.listCustomPage.size() + " -  listCustomPageEvent:"
                + foodTruckResult.getEvents());
        System.out.println("    JSON= " + foodTruckResult.toMap());
        for (final AppsItem appsItem : foodTruckResult.listCustomPage)
        {
            if (appsItem.appsStatus == AppsStatus.LOCAL) {
                System.out.println("LOCAL:" + appsItem.appsId + ":" + appsItem.getAppsName() + " (" + appsItem.displayName + ")");
                continue;
            }
            logApps("STORE ", appsItem);

            foodTruckParam.saveDownloadFileLocaly = true;
            foodTruckParam.directoryFileLocaly = "c:/temp/";

            foodTruckParam.appsItem = appsItem;
            if ((appsItem.appsStatus == AppsStatus.NEW || appsItem.appsStatus == AppsStatus.OK || appsItem.appsStatus == AppsStatus.TOUPDATE)
                    && !appsItem.getAppsName().equals("custompage_foodtruck"))
            {

                final FoodTruckResult foodTruckResultApps = FoodTruckAccess.downloadAndInstallCustomPage(foodTruckParam, apiSession);
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

    // @Test
    public void testListCustompage() {
        final FoodTruckParam foodTruckParam = new FoodTruckParam();
        final APISession apiSession = login();
        if (apiSession == null)
        {
            System.out.println("Can't connect");
            return;
        }
        PageAPI pageAPI;
        try {
            pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);

            final ProfileAPI profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);

            final FoodTruckResult statusOperation = FoodTruckAccess.getListLocalResources(foodTruckParam, pageAPI, profileAPI);
            System.out.println(statusOperation.toMap());

            if (statusOperation.listCustomPage != null)
            {
                for (final AppsItem customPage : statusOperation.listCustomPage) {
                    String profileSt="";
                    final List<Map<String, Object>> listProfiles = customPage.getListProfiles();
                    if (listProfiles!=null) {
                        for (final Map<String, Object> profile : listProfiles) {
                            profileSt+=profile.toString()+",";
                        }
                    }

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
        } catch (final Error er)
        {
            er.printStackTrace();
        } catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    // @ T e s t
    public void testDeployCustomPage()
    {
        logger.info("------------------------- TestDeployCustomPage");
        final APISession apiSession = login();
        if (apiSession == null)
        {
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
        if (apiSession == null)
        {
            System.out.println("Can't connect");
            return;
        }
        foodTruckParam.listRepository.add(FoodTruckStoreFactory.CommunityRepository);
        foodTruckParam.appsItem = new AppsItem();

        final FoodTruckResult statusOperation = FoodTruckAccess.getListCustomPage(foodTruckParam, apiSession);

        for (final AppsItem customPage : statusOperation.listCustomPage) {
            foodTruckParam.appsItem = customPage;

            for (final Profile profile : statusOperation.allListProfiles)
            {
                boolean exist = false;
                for (final Map<String, Object> oneProfile : customPage.getListProfiles())
                {
                    final Long profileId = (Long) oneProfile.get("id");

                    if (profile.getId() == profileId) {
                        exist = true;
                    }
                }
                if (!exist)
                {
                    foodTruckParam.profileid = profile.getId();
                    FoodTruckResult foodTruckResult = FoodTruckAccess.addInProfile(foodTruckParam, apiSession);
                    System.out.println("addInprofile result=" + foodTruckResult.statusinfo + " result=" + foodTruckResult.toMap() + " events="
                            + foodTruckResult.getEvents());

                    foodTruckResult = FoodTruckAccess.removeFromProfile(foodTruckParam, apiSession);
                    System.out.println("removedInprofile result=" + foodTruckResult.statusinfo + " events=" + foodTruckResult.getEvents());

                }
            }
        }
    }

    // @Test
    public void checkGithub()
    {
        // final ResultLastContrib resultLastContrib = GithubAccessor.getLastContribReleaseAsset("PierrickVouletBonitasoft", "pwd");
        final GithubAccessor githubAccessor = new GithubAccessor(FoodTruckStoreFactory.CommunityGithubUserName, FoodTruckStoreFactory.CommunityGithubPassword,
                FoodTruckStoreFactory.CommunityGithubUrlRepository);
        final LogBox logBox = new LogBox();
        final ResultGithub resultLastContrib = githubAccessor.executeGetRestOrder("/repos", null, logBox);
        System.out.println(resultLastContrib.jsonResult);
        resultLastContrib.checkResultFormat(true, "Repo should be a list");
        if (!BEventFactory.isError(resultLastContrib.listEvents))
        {
            for (final Object oneCustom : resultLastContrib.getJsonArray())
            {
                System.out.println(oneCustom);

            }
        }
    }
    public APISession login()
    {
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
        } catch (final Exception e)
        {
            logger.severe("during login " + e.toString());
        }
        return null;
    }


    private void logApps(final String label, final AppsItem appsItem)
    {
        System.out.println(">>>>>> " + label + " :" + appsItem.appsId + ":" + appsItem.getAppsName() + " (" + appsItem.displayName + ") Status["
                + appsItem.appsStatus + "]");
        System.out.println("                               What's new  : [" + appsItem.whatsnews + "]");
        System.out.println("                               Description  : [" + appsItem.description + "]");
        System.out.println("                               nbDownload: [" + appsItem.getNumberOfDownload() + "]");
        System.out.println("                               UrlDownload  : [" + appsItem.getLastUrlDownload() + "] realaseDate ["
                + appsItem.getLastReleaseDate() + "]");

    }

    // @Test
    public void testDashBoard() {
        final FoodTruckParam foodTruckParam = new FoodTruckParam();
        final APISession apiSession = login();
        if (apiSession == null)
        {
            System.out.println("Can't connect");
            return;
        }
        try {

            foodTruckParam.listRepository.add(FoodTruckDefStore.getGithub(
                    "https://api.github.com/repos/PierrickVouletBonitasoft/bonita-internal-contrib-releases/releases/latest", "bonitafoodtruck", "bonita2016"));

            foodTruckParam.saveLogoFileLocaly = true;
            foodTruckParam.directoryFileLocaly = "c:/temp";

            System.out.println("-------------- Dahsboard");

            final FoodTruckResult statusOperation = FoodTruckAccess.getListCustomPage(foodTruckParam, apiSession);
            System.out.println(statusOperation.toMap());
            for (final AppsItem customPage : statusOperation.listCustomPage) {
                String profileSt = "";
                final List<Map<String, Object>> listProfiles = customPage.getListProfiles();
                if (listProfiles != null) {
                    for (final Map<String, Object> profile : listProfiles) {
                        profileSt += profile.toString() + ",";
                    }
                }

                System.out.println(customPage.appsStatus + " " + customPage.appsId + ":" + customPage.getAppsName() + " (" + customPage.displayName + ") - "
                        + " provided:" + customPage.isProvided
                        + " " + customPage.getLastReleaseDate() + " - desc[" + customPage.description + "] Profile[" + profileSt + "]");
            }
            System.out.println("-------------- end dahsboard");
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
