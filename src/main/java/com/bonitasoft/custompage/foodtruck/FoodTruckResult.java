package com.bonitasoft.custompage.foodtruck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.ArtifactCustomPage;
import org.bonitasoft.store.artifact.ArtifactRestApi;
import org.bonitasoft.store.toolbox.LoggerStore;



public class FoodTruckResult {


    public List<Artifact> listStoreItem = new ArrayList<Artifact>();
    private final List<BEvent> listEvents = new ArrayList<BEvent>();
    public List<Profile> allListProfiles = new ArrayList<Profile>();

    public Long profileEntry = null;

    public String statusTitle;
    public String statusinfo = "";
    public String statusDetails = "";

    /**
     * on some operation for one custom page, like a Download, this status
     * is updated
     */
    public ArtifactFoodTruck.AppsStatus statuscustompage = null;
    public boolean isAllowAddProfile = false;

    public List<ArtifactFoodTruck> listArtifacts = new ArrayList<ArtifactFoodTruck>();
    
    // public Map<String, Object> mStatusResultJson = new HashMap<String,
    // Object>();

    public byte[] contentByte;

    /** time to do the operation in ms */
    private long beginTime;
    private long timeOperation;

    /** create the result as soon as possible to register the time */
    public FoodTruckResult(final String title) {
      beginOperation();
      statusTitle = title;
    }

    /**
     * if one error message is set, then the status are in error
     *
     * @return
     */
    public boolean isError() {
      return BEventFactory.isError(listEvents);
    }

    public void addDetails(final String details) {
      statusDetails += details + ";";
    }

    public void addEvent(final BEvent event) {
      BEventFactory.addEventUniqueInList(listEvents, event);
      if (event.isError()) {
        LoggerStore.logger.severe("FoodTruck.toolbox: Error " + event.toString());
      }
    }

    public void addEvents(final List<BEvent> events) {
      BEventFactory.addListEventsUniqueInList(listEvents, events);
      for (final BEvent event : events) {
        if (event.isError()) {
          LoggerStore.logger.severe("FoodTruck.toolbox: Error " + event.toString());
        }
      }
    }

    public List<BEvent> getEvents() {
      return listEvents;
    };

    public void setSuccess(final String success) {
      statusinfo = success;

    }

    public void addFoodTruckResult(final FoodTruckResult statusOperation) {
      statusTitle += statusOperation.statusTitle + ";";
      statusinfo += statusOperation.statusinfo.length() > 0 ? statusOperation.statusinfo + ";" : "";
      statusDetails += statusOperation.statusDetails.length() > 0 ? statusOperation.statusDetails + ";" : "";
      listEvents.addAll(statusOperation.listEvents);

    }

    public ArtifactFoodTruck getArtefactByName(final String name) {
      for (final ArtifactFoodTruck apps : listArtifacts) {
        if (apps.artifact.getName().equals(name)) {
          return apps;
        }
      }
      return null;
    }
    
    public void addApps(final ArtifactFoodTruck apps) {
      listArtifacts.add( apps );
    }
    /**
     * timeOperation method
     */
    public void beginOperation() {
      beginTime = System.currentTimeMillis();
    }

    public void endOperation() {
      timeOperation = System.currentTimeMillis() - beginTime;
    }

    public long getTimeOperation() {
      return timeOperation;
    }

    /**
     * @return
     */

    public Map<String, Object> toMap() {
      // map item
      Collections.sort(listArtifacts, new Comparator<ArtifactFoodTruck>() {

        @Override
        public int compare(final ArtifactFoodTruck s1, final ArtifactFoodTruck s2) {
          return s1.artifact.getName().compareTo(s2.artifact.getName());
        }
      });

      final List<Map<String, Object>> listJsonPages = new ArrayList<Map<String, Object>>();
      final List<Map<String, Object>> listJsonRestAPI = new ArrayList<Map<String, Object>>();
      for (final ArtifactFoodTruck artefactFoodTruck : listArtifacts) {
          if (artefactFoodTruck.artifact instanceof ArtifactCustomPage)
              listJsonPages.add( artefactFoodTruck.getArtifactMap() );
          if (artefactFoodTruck.artifact instanceof ArtifactRestApi)
              listJsonRestAPI.add( artefactFoodTruck.getArtifactMap() );
      }

      final Map<String, Object> result = new HashMap<String, Object>();
      result.put("title", statusTitle);
      result.put("listpages", listJsonPages);
      result.put("listrestapi", listJsonRestAPI);
      result.put("listevents", BEventFactory.getHtml(listEvents));
      result.put("statusinfo", statusinfo);
      if (statuscustompage != null) {
        result.put("statuscustompage", statuscustompage.toString());
      }
      result.put("isAllowAddProfile", isAllowAddProfile);

      // save the profiles
      final List<Map<String, Object>> listProfileJson = new ArrayList<Map<String, Object>>();
      for (final Profile profile : allListProfiles) {
        final Map<String, Object> oneProfile = new HashMap<String, Object>();
        oneProfile.put("id", profile.getId());
        oneProfile.put("name", profile.getName());
        oneProfile.put("description", profile.getDescription());

        listProfileJson.add(oneProfile);
      }
      result.put("alllistprofiles", listProfileJson);
      return result;

    }
    
}
