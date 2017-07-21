package com.bonitasoft.custompage.foodtruck;

import com.bonitasoft.custompage.foodtruck.AppsItem.TypeApps;
import com.bonitasoft.custompage.foodtruck.Toolbox.FoodTruckResult;

/**
 * BonitaStore : access application
 */
public interface FoodTruckIntBonitaStore {

	/** return the name of the repository. Must be unique */
	public String getName();

	/**
	 * getListAvailableApps
	 * 
	 * @param logBox
	 *            TODO
	 *
	 * @return
	 */
	public FoodTruckResult getListAvailableItems(TypeApps typeApps, LogBox logBox);

	/**
	 * download the application. Result is saved in FoodTruckResult.content
	 *
	 * @param name
	 * @return
	 */
	public FoodTruckResult downloadOneCustomPage(final AppsItem appsItem, LogBox logBox);

}
