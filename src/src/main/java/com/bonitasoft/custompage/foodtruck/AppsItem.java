package com.bonitasoft.custompage.foodtruck;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.log.event.BEvent;
import org.json.simple.JSONValue;

/**
 * an AppsItem maybe a Custom Page, a CustomWidget
 */
public class AppsItem
{

    public class AppsRelease
    {

        public Long id;
        public String version;
        public Date dateRelease;
        public String urlDownload;
        public Long numberOfDownload;
        public String releaseNote;
    }
    /**
     * NEW : the apps is NEW on the store
     * UPDATE : the apps is present localy and on the store, and a new version is available
     * OK : Apps exist on store and localy, same versio
     * LOCAL : apps exist only locally
     * INDOWNLOAD : currently in progress to download
     */
    public enum AppsStatus {
        NEW, TOUPDATE, OK, LOCAL, INDOWNLOAD, NOTAVAILABLE
    };

    public enum TypeApps {
        CUSTOMPAGE, CUSTOMWIDGET
    };


    /**
     * name of the application. THis name is unique on the Store and locally
     */
    private String appsName;

    /**
     * when the apps is installed, it got an unique ID.
     */
    public Long appsId;

    // give the status of the apps
    public AppsStatus appsStatus;

    public TypeApps typeApps;

    public String displayName;
    public String contribFile;
    // public String urlDownload;
    public String documentationFile;
    public String description;
    public byte[] logo;

    /**
     * multiple github source can be explode. Retains from which github this apps come from
     */
    public FoodTruckIntBonitaStore sourceFoodTruckStoreGithub;

    /**
     * name can be send to the browser, and then we can retrieve the store from the name
     */
    public String sourceFoodTruckStoreGithubName = null;
    /**
     * this properties are private : if some release are know, then the information come the release list.
     * Use the get() method
     */
    private String lastUrlDownload;
    private Date lastReleaseDate;
    private long numberOfDownload = 0;

    // in case of a new release exist on the store, this is the new release date
    // public Date storeReleaseDate;

    // isProvided : this page is provided by defaut on the BonitaEngine (like the GroovyExample page)
    public boolean isProvided = false;

    /**
     * calculate the whatsnews between the current version and the store one
     */
    public String whatsnews;
    // add description... profile...


    public List<BEvent> listEvents = new ArrayList<BEvent>();

    private List<Map<String, Object>> listProfiles = new ArrayList<Map<String, Object>>();

    public final List<AppsRelease> listReleases = new ArrayList<AppsRelease>();
    public AppsItem() {
    };

    public AppsItem(final Page page)
    {
        appsStatus = AppsStatus.LOCAL;
        typeApps = TypeApps.CUSTOMPAGE;
        displayName = page.getDisplayName();
        appsId = page.getId();
        appsName = page.getName().toLowerCase();
        description = page.getDescription();
        isProvided = page.isProvided();
        lastReleaseDate = page.getInstallationDate();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static AppsItem getInstanceFromJsonSt(final String jsonSt) {
        if (jsonSt == null) {
            return null;
        }

        // Toolbox.logger.info("AppsItem: JsonSt[" + jsonSt + "]");
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return null;
        }
        final AppsItem appsItem = new AppsItem();
        appsItem.appsName = Toolbox.getString(jsonHash.get("appsname"), null);
        if (appsItem.appsName == null)
        {
            return null; // does not contains a apps in fact
        }
        appsItem.appsName = appsItem.appsName.toLowerCase();
        appsItem.listProfiles = (List) Toolbox.getList(jsonHash.get("listprofiles"), new ArrayList<Map<String, String>>());
        appsItem.displayName = Toolbox.getString(jsonHash.get("displayname"), null);
        appsItem.appsId = Toolbox.getLong(jsonHash.get("appsid"), null);
        appsItem.description = Toolbox.getString(jsonHash.get("description"), null);
        appsItem.isProvided = Toolbox.getBoolean(jsonHash.get("provided"), false);
        appsItem.lastReleaseDate = Toolbox.getDate(jsonHash.get("installationdate"), null);
        appsItem.whatsnews = Toolbox.getString(jsonHash.get("whatsnews"), null);
        appsItem.lastUrlDownload = Toolbox.getString(jsonHash.get("urldownload"), null);
        if (jsonHash.get("status") != null) {
            appsItem.appsStatus = AppsStatus.valueOf((String) jsonHash.get("status"));
        }
        // appsItem.urlDownload = Toolbox.getString(jsonHash.get("urldownload"), null);
        appsItem.documentationFile = Toolbox.getString(jsonHash.get("documentationfile"), null);
        appsItem.sourceFoodTruckStoreGithubName = Toolbox.getString(jsonHash.get("storegithubname"), null);

        return appsItem;
    }

    public void clearProfiles()
    {
        if (listProfiles == null) {
            listProfiles = new ArrayList<Map<String, Object>>();
        } else {
            listProfiles.clear();
        }
    }

    public List<Map<String, Object>> getListProfiles()
    {
        return listProfiles;
    }
    public void addOneProfile(final Profile profile)
    {
        if (listProfiles == null) {
            listProfiles = new ArrayList<Map<String, Object>>();
        }

        for (final Map<String, Object> profileExist : listProfiles)
        {
            if (profileExist.get("id").equals(profile.getId()))
            {
                return; // already register
            }
        }
        final Map<String, Object> profileMap = new HashMap<String, Object>();
        listProfiles.add(profileMap);
        profileMap.put("name", profile.getName());
        profileMap.put("description", profile.getDescription());
        profileMap.put("id", profile.getId());
    }


    public void updateFromNewRelease(final AppsItem storeApp)
    {
        lastReleaseDate = storeApp.getLastReleaseDate();
        lastUrlDownload = storeApp.getLastUrlDownload();
        numberOfDownload = storeApp.getNumberOfDownload();

        documentationFile = storeApp.documentationFile;
        // keep the another parameter as it

    }

    public void setAppsName(final TypeApps typeApps, final String appsName)
    {

        this.appsName = appsName == null ? "" : appsName.toLowerCase();
        if (typeApps != null)
        {
            // normalise the name
            if (this.appsName.indexOf("_") != -1) {
                this.appsName = typeApps.toString().toLowerCase() + this.appsName.substring(this.appsName.indexOf("_"));
            }
        }
    }

    public String getAppsName() {
        return appsName;
    }



    /**
     * @return
     */
    public Map<String, Object> toMap()
    {
        final Map<String, Object> appsDetails = new HashMap<String, Object>();
        appsDetails.put("listprofiles", listProfiles);
        appsDetails.put("displayname", displayName);
        appsDetails.put("appsid", appsId);
        appsDetails.put("appsname", appsName);
        appsDetails.put("description", description);
        appsDetails.put("provided", isProvided);
        appsDetails.put("urldownload", getLastUrlDownload());
        appsDetails.put("documentationfile", documentationFile);
        appsDetails.put("nbdownloads", getNumberOfDownload());
        if (getLastReleaseDate() != null) {
            appsDetails.put("installationdate", getLastReleaseDate().getTime());
        }
        if (whatsnews != null) {
            appsDetails.put("whatsnews", whatsnews);
        }
        if (sourceFoodTruckStoreGithub != null) {
            appsDetails.put("storegithubname", sourceFoodTruckStoreGithub.getName());
        }
        appsDetails.put("status", appsStatus.toString());
        // logo generated by GenerateListingItem.java
        if (logo != null) {
            appsDetails.put("urllogo", "pageResource?page=custompage_foodtruck&location=storeapp/" + appsName + ".jpg");
        }

        return appsDetails;
    }

    @Override
    public String toString() {
        return appsName + "(" + appsId + ") " + appsStatus + " IsProvided:" + isProvided + " " + description + " URL[" + getLastUrlDownload() + "] nbRelease["
                + listReleases.size() + "]";
    }

    // release
    public AppsRelease newInstanceRelease()
    {
        final AppsRelease appsRelease = new AppsRelease();
        return appsRelease;
    }

    /**
     * we parse the release. For all realease AFTER the date, we complete the release note.
     *
     * @param dateFrom
     * @return
     */
    public String getReleaseInformation(final Date dateFrom)
    {
        return "";
    }

    public Date getLastReleaseDate()
    {
        if (listReleases.size() > 0) {
            return listReleases.get(0).dateRelease;
        }
        return lastReleaseDate;
    }

    /**
     * getNbDownload, by summury all the download in release, or by using the local information
     */
    public long getNumberOfDownload()
    {
        if (listReleases.size() > 0) {
            long total = 0;
            for (final AppsRelease appsRelease : listReleases) {
                total += appsRelease.numberOfDownload;
            }
            return total;
        }
        return numberOfDownload;
    }

    public String getLastUrlDownload()
    {
        if (listReleases.size() > 0) {
            return listReleases.get(0).urlDownload;
        }
        return lastUrlDownload;
    }

    public void addEvent(final BEvent eventMsg)
    {
        listEvents.add(eventMsg);
    }
}