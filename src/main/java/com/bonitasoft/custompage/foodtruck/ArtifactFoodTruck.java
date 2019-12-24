package com.bonitasoft.custompage.foodtruck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreFactory;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.ArtifactCustomPage;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.artifact.SerialiseJsonArtifact;
import org.json.simple.JSONValue;

/**
 * artifact manipulate by the FoodTruck
 * This object use a Storeartifact, then a status, and are able to calculate the JSON value.
 * 
 * @author Firstname Lastname
 */
public class ArtifactFoodTruck {

    /**
     * artifact has a status
     * 
     * @author Firstname Lastname
     */
    public enum AppsStatus {
        NEW, TOUPDATE, OK, LOCAL, INDOWNLOAD, NOTAVAILABLE
    };

    public AppsStatus status;

    public Artifact artifact;

    

    public static ArtifactFoodTruck getInstance(Artifact artifact) {
        ArtifactFoodTruck artifactFoodTruck = new ArtifactFoodTruck();
        artifactFoodTruck.status = AppsStatus.LOCAL;
        artifactFoodTruck.artifact = artifact;
        return artifactFoodTruck;
    }

    public String getName() {
        return artifact == null ? null : artifact.getName();
    }

    public void updateForLastRelease(Artifact storeApp) {
        artifact.setLastReleaseDate(storeApp.getLastReleaseDate());
        artifact.setLastUrlDownload(storeApp.getLastUrlDownload());
        artifact.setNumberOfDownload(storeApp.getNumberOfDownload());

        artifact.documentationFile = storeApp.documentationFile;
        // keep the another parameter as it

    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Profile */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public void clearProfiles() {
        if (artifact instanceof ArtifactCustomPage)
            ((ArtifactCustomPage) artifact).clearProfiles();
    }


    public void addOneProfile(final Profile profile) {
        if (artifact instanceof ArtifactCustomPage)
        {
            ArtifactCustomPage artifactCustomPage = (ArtifactCustomPage) artifact;
            artifactCustomPage.addOneProfile(profile);
        }
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* JSON */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * get the artifact from JSON
     * 
     * @param jsonSt
     * @return
     */
   
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ArtifactFoodTruck getInstanceFromJsonSt(final String jsonSt, BonitaStoreFactory storeFactory) {

        if (jsonSt == null) {
            return null;
        }
        ArtifactFoodTruck artifactFoodTruck = new ArtifactFoodTruck();

        artifactFoodTruck.artifact = SerialiseJsonArtifact.getInstanceFromJsonSt(FactoryArtifact.getInstance(), storeFactory, jsonSt);

        // Toolbox.logger.info("AppsItem: JsonSt[" + jsonSt + "]");
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return null;
        }

        // artifactFoodTruck.listProfiles = (List) Toolbox.getList(jsonHash.get("listprofiles"), new ArrayList<Map<String, String>>());
        // artifactFoodTruck.sourceFoodTruckStoreGithubName = Toolbox.getString(jsonHash.get("storegithubname"), null);

        if (jsonHash.get("status") != null) {
            artifactFoodTruck.status = AppsStatus.valueOf((String) jsonHash.get("status"));
        }
        // appsItem.urlDownload = Toolbox.getString(jsonHash.get("urldownload"),
        // null);

        return artifactFoodTruck;
    }

    public Map<String, Object> getArtifactMap() {
        Map<String, Object> result = SerialiseJsonArtifact.getArtifactMap(artifact);
        boolean isAllowAddProfile = false;

        if (status == AppsStatus.TOUPDATE
                || status == AppsStatus.LOCAL
                || status == AppsStatus.OK)
            isAllowAddProfile = true;
        if (status == AppsStatus.TOUPDATE
                || status == AppsStatus.LOCAL
                || status == AppsStatus.OK)
            isAllowAddProfile = true;

        result.put("isAllowAddProfile", isAllowAddProfile);
        if (artifact.getLastReleaseDate() != null) {
            result.put("installationdate", artifact.getLastReleaseDate().getTime());
        }
        result.put("status", status.toString());

        return result;

    }

}
