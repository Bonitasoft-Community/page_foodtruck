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
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.foodtruck.AppsItem.AppsStatus;
import com.bonitasoft.custompage.foodtruck.AppsItem.TypeApps;
import com.bonitasoft.custompage.foodtruck.FoodTruckStoreFactory.FoodTruckDefStore;
import com.bonitasoft.custompage.foodtruck.LogBox.LOGLEVEL;
import com.bonitasoft.custompage.foodtruck.Toolbox.FoodTruckResult;
import com.bonitasoft.engine.profile.ProfileEntryCreator;

public class FoodTruckAccess {



    private static final BEvent eventSearchException = new BEvent(FoodTruckAccess.class.getName(), 1, Level.ERROR, "Search exception",
            "The search in engine failed",
            "Check the log (maybe not the same BonitaVersion ?)");

    private static final BEvent eventProfileNotFound = new BEvent(FoodTruckAccess.class.getName(), 2, Level.ERROR, "Profile not found",
            "A profile is register in a page, but can't be found by it's ID",
            "Check the log (maybe not the same BonitaVersion ?)");

    private static final BEvent serverAPIError = new BEvent(FoodTruckAccess.class.getName(), 3, Level.ERROR, "ServerAPI failed",
            "A request is asked to the server to get the different API, and the call failed",
            "You should be disconnected : reconnect");

    private static final BEvent pageAlreadyExist = new BEvent(FoodTruckAccess.class.getName(), 4, Level.APPLICATIONERROR, "Page already exist",
            "The page can't be load in the portal, because it's already exist with the same name",
            "Check the name of the page");

    private static final BEvent invalidePageToken = new BEvent(FoodTruckAccess.class.getName(), 5, Level.ERROR, "Invalid Page token",
            "The page can't be created due to an Invalid Page Token",
            "Contact the Support to get explanation on this exception");

    private static final BEvent invalidZipContent = new BEvent(FoodTruckAccess.class.getName(), 6, Level.APPLICATIONERROR, "Invalid Zip Content",
            "The page downloaded is not a correct ZIP file, and does not have the expected format",
            "Contact the Support to fix the content of this page");

    private static final BEvent creationException = new BEvent(FoodTruckAccess.class.getName(), 7, Level.ERROR, "Creation exception",
            "An error arrived during the creation of the page",
            "Check the message");

    private static final BEvent saveLocalFile = new BEvent(FoodTruckAccess.class.getName(), 8, Level.APPLICATIONERROR, "File save error",
            "An error arrive when the file is saved",
            "Check the message");

    private static final BEvent searchProfileError = new BEvent(FoodTruckAccess.class.getName(), 9, Level.ERROR, "Search profile",
            "A search profile failed",
            "Check the message");

    private static final BEvent comProfileAPIRequested = new BEvent(FoodTruckAccess.class.getName(), 10, Level.APPLICATIONERROR, "Com Profile API required",
            "To reference the profile in a profile, the Subscription version is required",
            "Use a subscription version");

    private static final BEvent failRegisterPageInProfile = new BEvent(FoodTruckAccess.class.getName(), 11, Level.APPLICATIONERROR, "Registration failed",
            "The registration of the page in the profile failed",
            "Check the message");

    private static final BEvent notAllowToUpdatePage = new BEvent(FoodTruckAccess.class.getName(), 12, Level.APPLICATIONERROR, "Not allow to update the page",
            "The update failed",
            "Check the message");

    private static final BEvent updateError = new BEvent(FoodTruckAccess.class.getName(), 12, Level.APPLICATIONERROR, "Update failed",
            "The new page can't be install, because it's not allow to update an existing page",
            "Allow the user to update existing page");

    private static final BEvent noContentAvailable = new BEvent(FoodTruckAccess.class.getName(), 13, Level.APPLICATIONERROR, "No content available",
            "The application can't be download",
            "The application can't be uploaded, so can't be installed",
            "Ask the repository administrator, or wait the avaibility of the application");

    public enum FilterEnum {
        ALL, LOCALONLY, NEWONLY, UPDATABLEONLY
    };

    // access the BonitaCommunity




    /**
     * FoodTruckParam
     */
    public static class FoodTruckParam {

        public List<FoodTruckDefStore> listRepository = new ArrayList<FoodTruckDefStore>();

        public AppsItem appsItem;
        public Long profileid;
        public boolean keepIsProvided = false;
        public boolean listCustomPages = false;
        public FilterEnum filter = FilterEnum.ALL;
        int searchMaxResources = 1000;

        // if true, then all download file are saved localy, using the directoryFileLocaly repository
        public boolean saveDownloadFileLocaly = false;
        public boolean saveLogoFileLocaly = true;
        public String directoryFileLocaly;
        public boolean allowUpdatePage = true;

        /**
         * github to access the community - better to have a login password to not reach the API Limit
         */
        public String githubLogin = null;
        public String githubPassword = null;



        LogBox logBox = new LogBox();
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
            try
            {
                if (jsonHash.get("loglevel") != null) {
                    foodTruckParam.logBox.logLevel = LOGLEVEL.valueOf((String) jsonHash.get("loglevel"));
                }
            } catch (final Exception e)
            {
                foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Decode [loglevel] parameter in Json :" + e.toString());
            };
            foodTruckParam.logBox.log(LOGLEVEL.DEBUG, "FoodTruckParam: _JsonSt[" + jsonSt + "] ");

            foodTruckParam.appsItem = AppsItem.getInstanceFromJsonSt(jsonSt);

            foodTruckParam.profileid = Toolbox.getLong(jsonHash.get("profileid"), null);

            final String show = (String) jsonHash.get("show");
            if ("MYPLATFORM".equals(show)) {
                foodTruckParam.filter = FilterEnum.LOCALONLY;
            } else if ("ALL".equals(show)) {
                foodTruckParam.filter = FilterEnum.ALL;
            } else if ("WHATSNEWS".equals(show)) {
                foodTruckParam.filter = FilterEnum.NEWONLY;
            } else if ("WHATSUPDATE".equals(show)) {
                foodTruckParam.filter = FilterEnum.UPDATABLEONLY;
            } else if ("SEARCH".equals(show)) {
                // not yet implemented
                foodTruckParam.filter = FilterEnum.ALL;
            }

            // login - password
            final Map<String, Object> github = Toolbox.getMap(jsonHash.get("github"), null);
            if (github != null)
            {
                foodTruckParam.githubLogin = Toolbox.getString(github.get("login"), null);
                foodTruckParam.githubPassword = Toolbox.getString(github.get("password"), null);

            }
            foodTruckParam.logBox.log(LOGLEVEL.INFO, "FoodTruckParam:  github[" + foodTruckParam.githubLogin + "] pass[" + foodTruckParam.githubPassword + "]");

            return foodTruckParam;
        }

        @Override
        public String toString()
        {
            return "filter[" + filter + "]";
        }



        /**
         * return the communityRepository : it the user give a login/password, use it
         *
         * @return
         */
        public FoodTruckDefStore getCommunityRepository()
        {

            if (githubLogin == null) {
                return FoodTruckStoreFactory.CommunityRepository;
            } else {
                return FoodTruckDefStore.getGithub(FoodTruckStoreFactory.GithubUrlRepository, githubLogin, githubPassword);
            }
        }

    }

    /**
     * @param foodTruckParam
     * @param pageAPI
     * @param profileAPI
     * @return
     */
    public static FoodTruckResult getListCustomPage(final FoodTruckParam foodTruckParam, final APISession apiSession)
    {
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logBeginOperation("getListCustomPage", "Filter=" + foodTruckParam.filter, foodTruckParam);
        }

        PageAPI pageAPI;
        final ProfileAPI profileAPI;
        try {
            pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);
            profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);
        } catch (ServerAPIException | UnknownAPITypeException | BonitaHomeNotSetException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Error creating api");
            final FoodTruckResult dashboardResult = new FoodTruckResult("getListCustomPage");
            dashboardResult.addEvent(new BEvent(serverAPIError, e, ""));
            if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
                logEndOperation("getListCustomPage", "", dashboardResult, foodTruckParam);
            }

            return dashboardResult;
        }

        foodTruckParam.listCustomPages = true;
        final FoodTruckResult dashboardResult = getListLocalResources(foodTruckParam, pageAPI, profileAPI);

        // add all store ?
        if (foodTruckParam.filter == FilterEnum.ALL || foodTruckParam.filter == FilterEnum.NEWONLY || foodTruckParam.filter == FilterEnum.UPDATABLEONLY)
        {
            final FoodTruckStoreFactory foodTruckStoreFactory = FoodTruckStoreFactory.getInstance();
            foodTruckStoreFactory.registerStore(foodTruckParam.listRepository);

            // Collect all page form all repository
            final List<AppsItem> listAllAppsItem = new ArrayList<AppsItem>();
            for (final FoodTruckDefStore foodTruckRepository : foodTruckParam.listRepository)
            {
                if (foodTruckParam.logBox.isLog(LOGLEVEL.DEBUG)) {
                    foodTruckParam.logBox.log(LOGLEVEL.DEBUG, "--- FoodTruckAccess.getListCustomPage: call Repository ["
                            + foodTruckRepository.githubUrlRepository + "] user["
                        + foodTruckRepository.githubUserName + "]");
                }

                final FoodTruckIntBonitaStore foodTruckStoreGithub = foodTruckStoreFactory.getFoodTruckStore(foodTruckRepository);

                final FoodTruckResult storeResult = foodTruckStoreGithub.getListAvailableItems(TypeApps.CUSTOMPAGE, foodTruckParam.logBox);
            dashboardResult.addEvents(storeResult.getEvents());
                if (storeResult.listCustomPage != null) {
                    listAllAppsItem.addAll(storeResult.listCustomPage);
                }
                if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
                    foodTruckParam.logBox.log(LOGLEVEL.INFO, "--- FoodTruckAccess.getListCustomPage: Repository [" + foodTruckRepository.githubUrlRepository
                            + "] Collect ["
                        + storeResult.listCustomPage + "] apps");
                }

            }

            // maybe already deployed ?
            for (final AppsItem storeApp : listAllAppsItem)
            {

                // save the logo file ?
                if (foodTruckParam.saveLogoFileLocaly)
                {
                    if (storeApp.logo != null)
                    {
                        // logo generated by GenerateListingItem.java
                        FileOutputStream fileOutput;
                        final String fileName = foodTruckParam.directoryFileLocaly + "/resources/storeapp/" + storeApp.getAppsName() + ".jpg";
                        if (foodTruckParam.logBox.isLog(LOGLEVEL.DEBUG)) {
                            foodTruckParam.logBox.log(LOGLEVEL.DEBUG, "FoodTruckAccess.getListCustomPage Save LogoFile[" + storeApp.getAppsName() + "] on ["
                                    + fileName + "]");
                        }
                        try {

                            fileOutput = new FileOutputStream(new File(fileName));
                            fileOutput.write(storeApp.logo);
                            fileOutput.close();
                        } catch (final FileNotFoundException e) {
                            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckResult.getListCustomPage: Can't write on [" + fileName + "] " + e.toString());
                        } catch (final IOException e) {
                            foodTruckParam.logBox.log(LOGLEVEL.ERROR,
                                    "FoodTruckResult.getListCustomPage: error during writing [" + fileName + "] " + e.toString());
                        }
                    }
                }
                final AppsItem appsItem = dashboardResult.getAppsByName(storeApp.getAppsName());
                if (appsItem != null)
                {
                    appsItem.appsStatus = AppsStatus.OK;
                    // logo are on the store, not locally (not yet)
                    if (appsItem.logo == null) {
                        appsItem.logo = storeApp.logo;
                    }
                    if (appsItem.getLastReleaseDate() != null && storeApp.getLastReleaseDate() != null
                            && storeApp.getLastReleaseDate().after(appsItem.getLastReleaseDate())) {
                        appsItem.appsStatus = AppsStatus.TOUPDATE;
                        appsItem.whatsnews = storeApp.getReleaseInformation(appsItem.getLastReleaseDate());
                    }
                    // update every time from the store
                    appsItem.updateFromNewRelease(storeApp);
                }
                else // new apps on store
                {
                    storeApp.appsStatus = AppsStatus.NEW;
                    dashboardResult.listCustomPage.add(storeApp);
                }
                if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
                    foodTruckParam.logBox.log(LOGLEVEL.INFO, "Apps[" + storeApp.getAppsName() + "] status[" + storeApp.appsStatus.toString() + "] LocalDate ["
                            + (appsItem != null ? appsItem.getLastReleaseDate() : "<nolocal>") + "] StoreApps[" + storeApp.getLastReleaseDate() + "]");
                }
            }
        }
        // ok, lets apply the filter now
        final List<AppsItem> listFilterCustomPage = new ArrayList<AppsItem>();
        for (final AppsItem appsItem : dashboardResult.listCustomPage)
        {
            if (appsItem.isProvided && !foodTruckParam.keepIsProvided) {
                continue;
            }

            if (foodTruckParam.filter == FilterEnum.ALL) {
                listFilterCustomPage.add(appsItem);
            }
            else if (foodTruckParam.filter == FilterEnum.LOCALONLY) {
                if (appsItem.appsStatus == AppsStatus.LOCAL) {
                    listFilterCustomPage.add(appsItem);
                }
            }
            else if (foodTruckParam.filter == FilterEnum.NEWONLY) {
                if (appsItem.appsStatus == AppsStatus.NEW) {
                    listFilterCustomPage.add(appsItem);
                }
            }
            else if (foodTruckParam.filter == FilterEnum.UPDATABLEONLY) {
                if (appsItem.appsStatus == AppsStatus.TOUPDATE) {
                    listFilterCustomPage.add(appsItem);
                }
            }
        }
        dashboardResult.listCustomPage = listFilterCustomPage;

        // sort by name now

        Collections.sort(dashboardResult.listCustomPage, new Comparator<AppsItem>()
        {

            @Override
            public int compare(final AppsItem s1,
                    final AppsItem s2)
            {
                return s1.getAppsName().compareTo(s2.getAppsName());
            }
        });

        // Search the profiles
        try
        {
            final SearchOptionsBuilder searchOptionProfile = new SearchOptionsBuilder(0, 1000);
            // Not working !! searchOptionProfile.filter(ProfileSearchDescriptor.IS_DEFAULT, Boolean.FALSE);
            final SearchResult<Profile> searchResult = profileAPI.searchProfiles(searchOptionProfile.done());
            for (final Profile profile : searchResult.getResult())
            {
                if (profile.isDefault()) {
                    continue;
                }
                dashboardResult.allListProfiles.add(profile);
            }
        } catch (final SearchException e)
        {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Error during seachProfile [" + e.toString() + "]");
            dashboardResult.addEvent(new BEvent(searchProfileError, e, ""));
        }
        dashboardResult.endOperation();
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logEndOperation("getListCustomPage", "", dashboardResult, foodTruckParam);
        }

        return dashboardResult;
    }

    /**
     * from an custompage page name, saved in foodTruckParam.appsItem, reload all information
     *
     * @param foodTruckParam
     * @param apiSession
     * @return
     */
    public static FoodTruckResult completeCustomPage(final FoodTruckParam foodTruckParam, final APISession apiSession)
    {
        final FoodTruckResult foodTruckResult = new FoodTruckResult("completeCustomPage");
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logBeginOperation("completeCustomPage", "appsItem=" + foodTruckParam.appsItem.getAppsName(), foodTruckParam);
        }

        final FoodTruckResult foodTruckResultSearch = searchAppsByNameInRepository(foodTruckParam.appsItem.getAppsName(), foodTruckParam);
        foodTruckResult.addEvents(foodTruckResultSearch.getEvents());
        foodTruckResult.listStoreItem = foodTruckResultSearch.listStoreItem;

        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logEndOperation("completeCustomPage", "", foodTruckResult, foodTruckParam);
        }

        return foodTruckResult;
    }


    /**
     * @param foodTruckParam
     * @param apiSession
     * @return
     */
    public static FoodTruckResult downloadAndInstallCustomPage(final FoodTruckParam foodTruckParam, final APISession apiSession)
    {
        AppsItem appsItem = foodTruckParam.appsItem;
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logBeginOperation("downloadAndInstallCustomPage", " urlDownload[" + appsItem.getLastUrlDownload() + "]", foodTruckParam);
        }

        final FoodTruckResult foodTruckResult = new FoodTruckResult("getListCustomPage");
        if (foodTruckParam.appsItem == null)
        {
            // give a appsItem !
            foodTruckResult.addEvent(new BEvent(serverAPIError, "No AppsItem given !"));
            if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
                logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
            }
            return foodTruckResult;

        }


        if (appsItem.getLastUrlDownload() == null)
        {
            final FoodTruckResult complete = completeCustomPage(foodTruckParam, apiSession);
            foodTruckResult.addEvents(complete.getEvents());
            appsItem = complete.listStoreItem.size() == 0 ? appsItem : complete.listStoreItem.get(0);
            if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
                foodTruckParam.logBox.log(LOGLEVEL.INFO, "foodTruck.downloadAndInstallCustomPage: After complete [" + appsItem.getAppsName() + "] urlDownload["
                    + appsItem.getLastUrlDownload() + "]");
            }

        }
        PageAPI pageAPI;
        final ProfileAPI profileAPI;

        try {
            pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);
            profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);
        } catch (ServerAPIException | UnknownAPITypeException | BonitaHomeNotSetException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Error creating api");
            foodTruckResult.addEvent(new BEvent(serverAPIError, e, ""));
            if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
                logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
            }

            return foodTruckResult;
        }

        // if we now the github, it's help !
        if (foodTruckParam.appsItem.sourceFoodTruckStoreGithub == null && foodTruckParam.appsItem.sourceFoodTruckStoreGithubName != null)
        {
            // try to find the store by the name

            foodTruckParam.appsItem.sourceFoodTruckStoreGithub = FoodTruckStoreFactory.getInstance().searchStoreByName(
                    foodTruckParam.appsItem.sourceFoodTruckStoreGithubName);

        }

        if (foodTruckParam.appsItem.sourceFoodTruckStoreGithub == null)
        {
            final FoodTruckResult foodTruckResultSearch = searchAppsByNameInRepository(appsItem.getAppsName(), foodTruckParam);
            foodTruckResult.addEvents(foodTruckResultSearch.getEvents());
            if (foodTruckResultSearch.listStoreItem.size() == 0)
            {
                // we can't found this apps anymore !

            } else {
                foodTruckParam.appsItem.sourceFoodTruckStoreGithub = foodTruckResultSearch.listStoreItem.get(0).sourceFoodTruckStoreGithub;
            }
        }

        if (foodTruckParam.appsItem.sourceFoodTruckStoreGithub != null)
        {
            foodTruckResult.statuscustompage = AppsStatus.INDOWNLOAD;

            // The apps is part of the foodTruckParam
            final FoodTruckResult foodTruckResultDownload = foodTruckParam.appsItem.sourceFoodTruckStoreGithub.downloadOneCustomPage(appsItem,
                    foodTruckParam.logBox);
            // merge
            foodTruckResult.addEvents(foodTruckResultDownload.getEvents());
            foodTruckResult.contentByte = foodTruckResultDownload.contentByte;

            if (BEventFactory.isError(foodTruckResult.getEvents())) {
                if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
                    logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
                }
                foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;

                return foodTruckResult;
            }

        }

        if (foodTruckResult.contentByte != null) {
            foodTruckResult.statuscustompage = AppsStatus.OK;
        }

        // test
        if (foodTruckParam.saveDownloadFileLocaly)
        {
            FileOutputStream file;
            try {
                if (foodTruckParam.logBox.isLog(LOGLEVEL.INFO)) {
                    foodTruckParam.logBox.log(LOGLEVEL.INFO, "FoodTruckAccess.downloadAndInstallCustomPage: save file [" + foodTruckParam.directoryFileLocaly + "/"
                        + foodTruckParam.appsItem.getAppsName() + ".zip" + "]");
                }
                if (foodTruckResult.contentByte != null)
                {
                    file = new FileOutputStream(foodTruckParam.directoryFileLocaly + "/" + foodTruckParam.appsItem.getAppsName() + ".zip");
                    file.write(foodTruckResult.contentByte);
                    file.close();

                } else {
                    foodTruckResult.addEvent(new BEvent(noContentAvailable, "Apps " + appsItem.getAppsName() + "]"));
                }

            } catch (final IOException e) {
                foodTruckResult.addEvent(new BEvent(saveLocalFile, e, "File [" + foodTruckParam.directoryFileLocaly
                        + foodTruckParam.appsItem.getAppsName()
                        + "]"));
                foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;

            }
        }

        // first delete an exiting page
        try
        {

            Page currentPage = null;
            try
            {
                currentPage = pageAPI.getPageByName(foodTruckParam.appsItem.getAppsName());
                // getPageByName does not work : search manually
                /*
                 * final SearchResult<Page> searchResult = pageAPI.searchPages(new SearchOptionsBuilder(0, 1000).done());
                 * for (final Page page : searchResult.getResult())
                 * {
                 * if (page.getName().equalsIgnoreCase(foodTruckParam.appsItem.getAppsName()))
                 * {
                 * pageAPI.deletePage(page.getId());
                 * }
                 * }
                 */

            } catch (final Exception e)
            {
            }

            /**
             * EXIT
             */
            if (currentPage != null)
            {
                if (foodTruckParam.allowUpdatePage)
                {
                    pageAPI.updatePageContent(currentPage.getId(), foodTruckResult.contentByte);
                    final AppsItem appsItemFromPage = new AppsItem(currentPage);
                    foodTruckResult.listCustomPage.add(appsItemFromPage);
                }
                else
                {
                    // update not allow
                    foodTruckResult.addEvent(new BEvent(notAllowToUpdatePage, "Apps [" + currentPage.getName() + "]"));
                }
            }
            else // new
            {
                final Page page = pageAPI.createPage(foodTruckParam.appsItem.getAppsName(), foodTruckResult.contentByte);
                final AppsItem appsItemFromPage = new AppsItem(page);
                foodTruckResult.listCustomPage.add(appsItemFromPage);
            }

        } catch (final AlreadyExistsException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
            foodTruckResult.addEvent(new BEvent(pageAlreadyExist, "Name:" + e.getName() + ", Message:" + e.getMessage()));
            foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;

        } catch (final InvalidPageTokenException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
            foodTruckResult.addEvent(new BEvent(invalidePageToken, "Message:" + e.getMessage()));
            foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
        } catch (final InvalidPageZipContentException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
            foodTruckResult.addEvent(new BEvent(invalidZipContent, "Message:" + e.getMessage()));
            foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
        } catch (final CreationException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
            foodTruckResult.addEvent(new BEvent(creationException, "Message:" + e.getMessage()));
            foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
        } catch (final UpdatingWithInvalidPageTokenException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
            foodTruckResult.addEvent(new BEvent(updateError, "Message:" + e.getMessage()));
            foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
        } catch (final UpdatingWithInvalidPageZipContentException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
            foodTruckResult.addEvent(new BEvent(invalidZipContent, "Message:" + e.getMessage()));
            foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
        } catch (final UpdateException e) {
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "FoodTruckAccess.downloadAndInstallCustomPage: exception " + e.toString());
            foodTruckResult.addEvent(new BEvent(updateError, "Message:" + e.getMessage()));
            foodTruckResult.statuscustompage = AppsStatus.NOTAVAILABLE;
        }

        // final foodTruckStoreGithub.downloadOneCustomPagefoodTruckParam.appsName
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logEndOperation("downloadAndInstallCustomPage", "", foodTruckResult, foodTruckParam);
        }

        return foodTruckResult;

    }

    /**
     * @param ldapSynchronizerPath
     * @param mDomain
     * @return
     */
    public static FoodTruckResult getListLocalResources(final FoodTruckParam foodTruckParam, final PageAPI pageAPI, final ProfileAPI profileAPI)
    {
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logBeginOperation("getListLocalResources", " in profile [" + foodTruckParam.profileid + "]", foodTruckParam);
        }

        final FoodTruckResult foodTruckResult = new FoodTruckResult("ListResource");

        // get list of pages
        if (foodTruckParam.listCustomPages)
        {
            Long profileId = null;

            try {

                SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, foodTruckParam.searchMaxResources);
                final SearchResult<Page> searchResultPage = pageAPI.searchPages(searchOptionsBuilder.done());
                for (final Page page : searchResultPage.getResult())
                {
                    if (!"page".equals(page.getContentType())) {
                        continue;
                    }

                    final AppsItem apps = new AppsItem(page);
                    foodTruckResult.listCustomPage.add(apps);
                }

                // search all profile, and populate the page
                searchOptionsBuilder = new SearchOptionsBuilder(0, foodTruckParam.searchMaxResources);
                SearchResult<ProfileEntry> searchResultProfile;

                searchResultProfile = profileAPI.searchProfileEntries(searchOptionsBuilder.done());

                for (final ProfileEntry profileEntry : searchResultProfile.getResult())
                {
                    final String name = profileEntry.getPage();
                    profileId = profileEntry.getProfileId();
                    final AppsItem appsItem = foodTruckResult.getAppsByName(name);
                    if (appsItem != null)
                    {
                        final Profile profile = profileAPI.getProfile(profileId);
                        appsItem.addOneProfile(profile);
                    }
                }
            } catch (final SearchException e) {
                foodTruckParam.logBox.logException("FoodTruckAccess. Error during read", e);
                foodTruckResult.addEvent(new BEvent(eventSearchException, e, "Max Result:" + foodTruckParam.searchMaxResources));
            } catch (final ProfileNotFoundException e) {
                foodTruckParam.logBox.logException("FoodTruckAccess. Error during read", e);
                foodTruckResult.addEvent(new BEvent(eventProfileNotFound, e, "ProfileId:" + profileId));
            }
        }
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
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
    public static FoodTruckResult addInProfile(final FoodTruckParam foodTruckParam, final APISession apiSession)
    {

        final FoodTruckResult foodTruckResult = new FoodTruckResult("addInprofile");
        try
        {
            final com.bonitasoft.engine.api.ProfileAPI comProfileAI = com.bonitasoft.engine.api.TenantAPIAccessor.getProfileAPI(apiSession);
            final Profile profile = comProfileAI.getProfile(foodTruckParam.profileid);
            logBeginOperation("addInprofile", " in profile name[" + profile.getName() + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);

            final AppsItem appsItem = foodTruckParam.appsItem;

            /*
             * final ProfileEntry profileEntry = comProfileAI.createProfileEntry(appsItem.displayName,
             * appsItem.description,
             * foodTruckParam.profileid,
             * "link",
             * appsItem.getAppsName());
             */
            final ProfileEntryCreator profileEntryCreator = new ProfileEntryCreator(foodTruckParam.profileid);
            profileEntryCreator.setPage(appsItem.getAppsName());
            profileEntryCreator.setParentId(0L);
            profileEntryCreator.setType("link");
            profileEntryCreator.setCustom(true);
            profileEntryCreator.setName(appsItem.displayName == null ? appsItem.getAppsName() : appsItem.displayName);
            final ProfileEntry profileEntry = comProfileAI.createProfileEntry(profileEntryCreator);

            foodTruckParam.logBox.log(LOGLEVEL.INFO, "createProfileEntry: profileId[" + foodTruckParam.profileid + "] appsName[" + appsItem.getAppsName()
                    + "] ParentId[OL] Type[link] Custom[true] name[" + appsItem.displayName + "] ==> PromeEntry[" + profileEntry.getId() + "]");

            appsItem.addOneProfile(profile);

            final ProfileAPI profileAI = TenantAPIAccessor.getProfileAPI(apiSession);
            refreshListProfiles(foodTruckParam.appsItem, profileAI, foodTruckParam.logBox);

            foodTruckResult.listCustomPage.add(appsItem);
            foodTruckResult.statusinfo = "Profile " + profile.getName() + " added";

            foodTruckResult.profileEntry = profileEntry.getId();

            foodTruckParam.logBox.log(LOGLEVEL.INFO, "AddInprofile apps[" + appsItem.toMap() + "]");
        } catch (BonitaHomeNotSetException | ServerAPIException | UnknownAPITypeException e1)
        {
            foodTruckResult.addEvent(new BEvent(serverAPIError, e1, ""));
        } catch (final CreationException e)
        {
            foodTruckResult.addEvent(new BEvent(failRegisterPageInProfile, e, "profileId[" + foodTruckParam.profileid + "] Page["
                    + foodTruckParam.appsItem.getAppsName() + "]"));
        } catch (final ProfileNotFoundException e)
        {
            logBeginOperation("addInprofile", " in profile Id[" + foodTruckParam.profileid + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);
            foodTruckResult.addEvent(new BEvent(eventProfileNotFound, "profileId [" + foodTruckParam.profileid + "]"));
        } catch (final ClassCastException e)
        {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Class " + sw.toString());

            foodTruckResult.addEvent(comProfileAPIRequested);
        } catch (final Exception e)
        {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Exception  " + sw.toString());

            foodTruckResult.addEvent(comProfileAPIRequested);
        }
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
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
    public static FoodTruckResult removeFromProfile(final FoodTruckParam foodTruckParam, final APISession apiSession)
    {

        final FoodTruckResult foodTruckResult = new FoodTruckResult("removeInprofile");
        try
        {
            final com.bonitasoft.engine.api.ProfileAPI comProfileAI = com.bonitasoft.engine.api.TenantAPIAccessor.getProfileAPI(apiSession);
            final Profile profile = comProfileAI.getProfile(foodTruckParam.profileid);
            logBeginOperation("removeInprofile", " in profile name[" + profile.getName() + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);

            final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 100);
            searchOptionsBuilder.filter(ProfileEntrySearchDescriptor.PROFILE_ID, foodTruckParam.profileid);
            searchOptionsBuilder.filter(ProfileEntrySearchDescriptor.PAGE, foodTruckParam.appsItem.getAppsName());

            final SearchResult<ProfileEntry> searchResult = comProfileAI.searchProfileEntries(searchOptionsBuilder.done());
            for (final ProfileEntry profileEntry : searchResult.getResult()) {
                comProfileAI.deleteProfileEntry(profileEntry.getId());
            }

            foodTruckResult.statusinfo = "Profile " + profile.getName() + " removed";

            // recalcul this apps
            final ProfileAPI profileAI = TenantAPIAccessor.getProfileAPI(apiSession);
            refreshListProfiles(foodTruckParam.appsItem, profileAI, foodTruckParam.logBox);
            foodTruckResult.listCustomPage.add(foodTruckParam.appsItem);

        } catch (BonitaHomeNotSetException | ServerAPIException | UnknownAPITypeException e1)
        {
            foodTruckResult.addEvent(new BEvent(serverAPIError, e1, ""));
        } catch (final ProfileNotFoundException e)
        {
            logBeginOperation("removeFromProfile", " in profile Id[" + foodTruckParam.profileid + "] id[" + foodTruckParam.profileid + "]", foodTruckParam);
            foodTruckResult.addEvent(new BEvent(eventProfileNotFound, "profileId [" + foodTruckParam.profileid + "]"));
        } catch (final ClassCastException e)
        {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Class " + sw.toString());

            foodTruckResult.addEvent(comProfileAPIRequested);
        } catch (final Exception e)
        {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            foodTruckParam.logBox.log(LOGLEVEL.ERROR, "Class " + sw.toString());

            foodTruckResult.addEvent(comProfileAPIRequested);
        }
        if (foodTruckParam.logBox.isLog(LOGLEVEL.MAIN)) {
            logEndOperation("removeInprofile", "", foodTruckResult, foodTruckParam);
        }

        return foodTruckResult;

    }

    /* ******************************************************************************** */
    /*                                                                                                                                                                  */
    /* private Method */
    /*                                                                                                                                                                  */
    /* ******************************************************************************** */

    /**
     * search an apps in directory from it's name
     *
     * @param appsName
     * @param foodTruckParam
     * @return
     */
    private static FoodTruckResult searchAppsByNameInRepository(final String appsName, final FoodTruckParam foodTruckParam)
    {
        final FoodTruckResult foodTruckResult = new FoodTruckResult("searchAppsByNameInRepository");
        final FoodTruckStoreFactory foodTruckStoreFactory = FoodTruckStoreFactory.getInstance();

        for (final FoodTruckDefStore foodTruckRepository : foodTruckParam.listRepository)
        {
            final FoodTruckIntBonitaStore foodTruckStoreGithub = foodTruckStoreFactory.getFoodTruckStore(foodTruckRepository);

            final FoodTruckResult storeResult = foodTruckStoreGithub.getListAvailableItems(TypeApps.CUSTOMPAGE, foodTruckParam.logBox);
            foodTruckResult.addEvents(storeResult.getEvents());

            // Exist in this repository ?
            for (final AppsItem storeApp : storeResult.listCustomPage)
            {
                if (storeApp.getAppsName().equals(appsName)) {
                    foodTruckResult.listStoreItem.add(storeApp);
                    return foodTruckResult;
                }
            }
        }
        return foodTruckResult;
    }

    /* ******************************************************************************** */
    /*                                                                                                                                                                  */
    /* Log method */
    /*                                                                                                                                                                  */
    /* ******************************************************************************** */

    /**
     * normalize beginLog
     *
     * @param method
     * @param message
     * @param foodTruckParam
     */
    private static void logBeginOperation(final String method, final String message, final FoodTruckParam foodTruckParam)
    {
        foodTruckParam.logBox.log(LOGLEVEL.MAIN, "~~~~~~~~ START FoodTruckAccess." + method + ": " + message
                + (foodTruckParam.appsItem != null ? " Apps[" + foodTruckParam.appsItem.getAppsName() + "]" : ""));
    }

    /**
     * logEndOperation
     *
     * @param method
     * @param message
     * @param foodTruckResult
     */
    private static void logEndOperation(final String method, final String message, final FoodTruckResult foodTruckResult, final FoodTruckParam foodTruckParam)
    {
        foodTruckResult.endOperation();
        foodTruckParam.logBox.log(LOGLEVEL.MAIN, "~~~~~~~~ END FoodTruckAccess." + method + ": " + foodTruckResult.getTimeOperation() + " ms status ["
                + foodTruckResult.statusinfo + "] "
                + message + " events["
                + foodTruckResult.getEvents() + "] content[" + foodTruckResult.toMap() + "]");
    }

    private static void refreshListProfiles(final AppsItem appsItem, final ProfileAPI profileAPI, final LogBox logBox)
    {
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 100);
        searchOptionsBuilder.filter(ProfileEntrySearchDescriptor.PAGE, appsItem.getAppsName());
        SearchResult<ProfileEntry> searchResultProfile;
        try {
            searchResultProfile = profileAPI.searchProfileEntries(searchOptionsBuilder.done());
            appsItem.clearProfiles();

            for (final ProfileEntry profileEntry : searchResultProfile.getResult())
            {
                final Long profileId = profileEntry.getProfileId();
                final Profile profile = profileAPI.getProfile(profileId);
                appsItem.addOneProfile(profile);
            }
        } catch (SearchException | ProfileNotFoundException e) {
            logBox.log(LOGLEVEL.ERROR, "Error during recalcul profile for Apps [" + appsItem.getAppsName() + "]");
        }
    }

}
