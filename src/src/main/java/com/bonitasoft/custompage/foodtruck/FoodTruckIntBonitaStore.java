package com.bonitasoft.custompage.foodtruck;

import java.util.List;

import com.bonitasoft.custompage.foodtruck.AppsItem.TypeArtefacts;
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
	
	public FoodTruckResult getListAvailableApps( final List<TypeArtefacts> listTypeApps,final boolean withNotAvailable, final LogBox logBox);

	/**
	 * download the application. Result is saved in FoodTruckResult.content
	 *
	 * @param name
	 * @return
	 */
	public FoodTruckResult downloadOneApps(final AppsItem appsItem, LogBox logBox);

}
