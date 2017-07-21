package com.bonitasoft.custompage.foodtruck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodTruckStoreFactory {

	/**
	 * describe a repository
	 */
	public static class FoodTruckDefStore {

		public boolean isGithub;
		public String githubUserName;
		public String githubPassword;
		public String githubUrlRepository;

		private FoodTruckDefStore() {

		}

		public static FoodTruckDefStore getGithub(final String url, final String name, final String password) {
			final FoodTruckDefStore defStore = new FoodTruckDefStore();
			defStore.githubUrlRepository = url;
			defStore.githubUserName = name;
			defStore.githubPassword = password;
			defStore.isGithub = true;
			return defStore;
		}

		public String getName() {
			return githubUrlRepository;
		}
	}

	/**
	 * declare the Community Github Repository
	 */
	public static String CommunityGithubUserName = "bonitafoodtruck";
	public static String CommunityGithubPassword = "bonita2016";
	public static String CommunityGithubUrlRepository = "https://api.github.com/orgs/Bonitasoft-Community";

	public static FoodTruckDefStore CommunityRepository = FoodTruckDefStore.getGithub(CommunityGithubUrlRepository, CommunityGithubUserName, CommunityGithubPassword);

	/**
	 * factory intanciation
	 */
	private static FoodTruckStoreFactory foodTruckStoreFactory = new FoodTruckStoreFactory();

	private FoodTruckStoreFactory() {
	}

	public static FoodTruckStoreFactory getInstance() {
		return foodTruckStoreFactory;
	}

	/**
	 * store definition registration
	 */
	private final Map<String, FoodTruckDefStore> mapDefStore = new HashMap<String, FoodTruckDefStore>();

	public void registerStore(final List<FoodTruckDefStore> newListDefStore) {
		for (final FoodTruckDefStore defStore : newListDefStore) {
			mapDefStore.put(defStore.getName(), defStore);
		}
	}

	/**
	 * return the store from the definition
	 *
	 * @param foodTruckRepository
	 * @return
	 */
	public FoodTruckIntBonitaStore getFoodTruckStore(final FoodTruckDefStore defStore) {
		// at this moment, only the githhub is available
		return new FoodTruckStoreGithub(defStore.githubUserName, defStore.githubPassword, defStore.githubUrlRepository);
	}

	/**
	 * return the store by the name
	 *
	 * @param storeName
	 * @return
	 */
	public FoodTruckIntBonitaStore searchStoreByName(final String storeName) {
		final FoodTruckDefStore defStore = mapDefStore.get(storeName);
		if (defStore == null) {
			return null;
		}
		return getFoodTruckStore(defStore);
	}

}
