package com.bonitasoft.custompage.foodtruck;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

import com.bonitasoft.custompage.foodtruck.AppsItem.AppsStatus;

public class Toolbox {

	/**
	 * this is the logger to use in FoodTruck Attention to reduce the usage, and
	 * to use foodTruckParam.log, then the log information can be manage at the
	 * Input level, as a parameters
	 */

	public static SimpleDateFormat sdfJavasscript = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	static Long getLong(final Object parameter, final Long defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return Long.valueOf(parameter.toString());
		} catch (final Exception e) {
			LogBox.logger.severe("Can't decode integer [" + parameter + "]");
			return defaultValue;
		}
	}

	static Integer getInteger(final Object parameter, final Integer defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return Integer.valueOf(parameter.toString());
		} catch (final Exception e) {
			LogBox.logger.severe("Can't decode integer [" + parameter + "]");
			return defaultValue;
		}
	}

	static Boolean getBoolean(final Object parameter, final Boolean defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return Boolean.valueOf(parameter.toString());
		} catch (final Exception e) {
			LogBox.logger.severe("Can't decode boolean [" + parameter + "]");
			return defaultValue;
		}
	}

	static String getString(final Object parameter, final String defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return parameter.toString();
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	static Date getDate(final Object parameter, final Date defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {

			return sdfJavasscript.parse(parameter.toString());
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * @param parameter
	 * @param defaultValue
	 * @return
	 */
	static List<Map<String, String>> getList(final Object parameter, final List<Map<String, String>> defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return (List<Map<String, String>>) parameter;
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * @param parameter
	 * @param defaultValue
	 * @return
	 */
	static Map<String, Object> getMap(final Object parameter, final Map<String, Object> defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return (Map<String, Object>) parameter;
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * calculate the file name
	 *
	 * @param directory
	 * @param domain
	 * @param configFileName
	 * @return
	 */
	public static String getConfigFileName(final String ldapSynchronizerPath, final String domain, final String configFileName) {
		final String fileName = ldapSynchronizerPath + File.separator + domain + File.separator + configFileName;
		// logger.info("CraneTruck.Toolbox: configuration [" + configFileName +
		// "] file is [" + fileName + "]");
		return fileName;
	}

	/**
	 * This class : - get the apps available on BonitaTStore - get the local
	 * Apps install - give a status apps per apps if
	 * foodTruckParam.listcustompage is true, then the custom page is checked if
	 * foodTruckParam.callAppStoreCustomPages is true, the AppsStore is call to
	 * search new apps, new update
	 *
	 * @param foodTruckParam
	 * @param pageAPI
	 * @param profileAPI
	 * @return
	 */

	/**
	 * this class is available to be used in different element
	 *
	 */
	public static class FoodTruckResult {

		public List<AppsItem> listStoreItem = new ArrayList<AppsItem>();
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
		public AppsStatus statuscustompage = null;
		public boolean isAllowAddProfile = false;

		public List<AppsItem> listCustomPage = new ArrayList<AppsItem>();
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
				LogBox.logger.severe("FoodTruck.toolbox: Error " + event.toString());
			}
		}

		public void addEvents(final List<BEvent> events) {
			BEventFactory.addListEventsUniqueInList(listEvents, events);
			for (final BEvent event : events) {
				if (event.isError()) {
					LogBox.logger.severe("FoodTruck.toolbox: Error " + event.toString());
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

		public AppsItem getAppsByName(final String name) {
			for (final AppsItem apps : listCustomPage) {
				if (apps.getAppsName().equals(name)) {
					return apps;
				}
			}
			return null;

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
			Collections.sort(listCustomPage, new Comparator<AppsItem>() {

				@Override
				public int compare(final AppsItem s1, final AppsItem s2) {
					return s1.getAppsName().compareTo(s2.getAppsName());
				}
			});

			final List<Map<String, Object>> listJson = new ArrayList<Map<String, Object>>();
			for (final AppsItem appsItem : listCustomPage) {
				listJson.add(appsItem.toMap());
			}

			final Map<String, Object> result = new HashMap<String, Object>();
			result.put("title", statusTitle);
			result.put("list", listJson);
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

}
