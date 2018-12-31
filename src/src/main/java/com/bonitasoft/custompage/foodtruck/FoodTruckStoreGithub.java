package com.bonitasoft.custompage.foodtruck;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.json.simple.JSONObject;

import com.bonitasoft.custompage.foodtruck.AppsItem.AppsStatus;
import com.bonitasoft.custompage.foodtruck.AppsItem.TypeArtefacts;
import com.bonitasoft.custompage.foodtruck.LogBox.LOGLEVEL;
import com.bonitasoft.custompage.foodtruck.Toolbox.FoodTruckResult;
import com.bonitasoft.custompage.foodtruck.github.GithubAccessor;
import com.bonitasoft.custompage.foodtruck.github.GithubAccessor.ResultGithub;

public class FoodTruckStoreGithub implements FoodTruckIntBonitaStore {

	private final GithubAccessor mGithubAccessor;

	private final static BEvent NoListingFound = new BEvent(FoodTruckStoreGithub.class.getName(), 2, Level.APPLICATIONERROR, "No Listing Found", "The Githbub repository is supposed to have a file 'listing.xml' which describe all objects available. THis file is not found.",
			"Check the github repository, you may be not access the correct one ? ");

	private final static BEvent BadListingFormat = new BEvent(FoodTruckStoreGithub.class.getName(), 3, Level.APPLICATIONERROR, "Bad Listing.xml format", "The format of the file Listing.xml is incorrect. This file describe all applications available in the repository",
			"The list of apps is not visible", "Check the exception, call the Github Administrator");
	private final static BEvent NoGithubInformation = new BEvent(FoodTruckStoreGithub.class.getName(), 4, Level.APPLICATIONERROR, "NoGithub information", "To connect to Gitghub, a login/password is mandatory", "The repository can't be accessed, the list of apps present itself is not visible",
			"Give a login / password");

	private final static BEvent noContribFile = new BEvent(FoodTruckStoreGithub.class.getName(), 5, Level.APPLICATIONERROR, "No contrib file", "The apps does not have any contrib file, and nothing can be download", "The apps can not be upload", "Contact the owner of the page to fix it");

	private final static BEvent noBinaryFile = new BEvent(FoodTruckStoreGithub.class.getName(), 6, Level.APPLICATIONERROR, "No binary file", "The binary file can't be uploaded from the store", "Apps can not be upload", "Contact the owner of the page to fix it");

	private final static BEvent errorDecodeLogo = new BEvent(FoodTruckStoreGithub.class.getName(), 7, Level.APPLICATIONERROR, "Error decode Logo", "The Logo can't be decode, or at not present", "No logo image", "Contact the owner of the page to fix it");

	private final static BEvent errorDecodePageProperties = new BEvent(FoodTruckStoreGithub.class.getName(), 8, Level.APPLICATIONERROR, "Error decode Pageproperties", "The page.properties file can't be decode",
			"No description on the item in the repository, and then a local application and the repository one can be considered different when in fact this is the same", "Contact the owner of the page to fix it");

	public FoodTruckStoreGithub(final String userName, final String password, final String urlRepository) {
		mGithubAccessor = new GithubAccessor(userName, password, urlRepository);
	}

	/**
	 * return the name
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return mGithubAccessor.getUrlRepository();
	}

	/**
	 *
	 */
	@Override
	public FoodTruckResult getListAvailableApps(final List<TypeArtefacts> listTypeApps, boolean withNotAvailable, final LogBox logBox) {
		final SimpleDateFormat sdfParseRelease = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

		final FoodTruckResult storeResult = new FoodTruckResult("getListAvailableItems");

		if (logBox.isLog(LOGLEVEL.MAIN)) {
			logBox.log(LOGLEVEL.MAIN, "Github  getListAvailableItems : " + mGithubAccessor.toLog());
		}

		if (!mGithubAccessor.isCorrect()) {
			storeResult.addEvent(NoGithubInformation);
			storeResult.endOperation();
			return storeResult;
		}
		// call the github to get the contents
		final ResultGithub resultListRepository = mGithubAccessor.executeGetRestOrder("/repos?page=1&per_page=10000", null, logBox);
		storeResult.addEvents(resultListRepository.listEvents);

		resultListRepository.checkResultFormat(true, "The Github should return a list or repository");
		// Sarch all project started by the ItemTypetype
		if (resultListRepository.isError()) {
			storeResult.endOperation();
			return storeResult;
		}
		for (final Object oneRepositoryOb : resultListRepository.getJsonArray()) {
			if (!(oneRepositoryOb instanceof JSONObject)) {
				continue;
			}
			final JSONObject oneRepository = (JSONObject) oneRepositoryOb;

			final String repositoryName = (String) oneRepository.get("name");
			TypeArtefacts typeApps = match(listTypeApps, repositoryName);
			if (typeApps == null)
				continue;
			

			// this is what we search
		
			final AppsItem appsItem = new AppsItem();
			appsItem.typeApps = typeApps;
			appsItem.appsStatus = AppsStatus.NEW; // for the moment
			appsItem.sourceFoodTruckStoreGithub = this;

			appsItem.appsId = -1L;

			// set the name. If a page.properties exist, then we will get the
			// information inside
			appsItem.setAppsName(typeApps, (String) oneRepository.get("name"));
			appsItem.displayName = (String) oneRepository.get("name");
			appsItem.description = (String) oneRepository.get("description");
			String traceOneApps = "name[" + appsItem.getAppsName() + "]";
			String shortName = appsItem.getAppsName();
			if (shortName.toUpperCase().startsWith(typeApps.toString().toUpperCase() + "_")) {
				// remove the typeApps
				shortName = shortName.substring(typeApps.toString().length() + 1);
			}

			// --------------------- Content
			// Logo and documentation
			final String contentUrl = (String) oneRepository.get("url");

			final ResultGithub resultContent = mGithubAccessor.executeGetRestOrder(null, contentUrl + "/contents", logBox);
			resultContent.checkResultFormat(true, "Contents of a repository must be a list");
			storeResult.addEvents(resultContent.listEvents);

			if (!resultContent.isError()) {
				for (final Map<String, Object> oneContent : (List<Map<String, Object>>) resultContent.getJsonArray()) {
					final String assetName = (String) oneContent.get("name");
					if (assetName == null) {
						continue;
					}
					if (assetName.equalsIgnoreCase("page.properties")) {
						final String urlPageProperties = (String) oneContent.get("download_url");
						final ResultGithub resultContentpage = mGithubAccessor.getContent(urlPageProperties, "GET", "", "UTF-8");
						storeResult.addEvents(resultContentpage.listEvents);
						if (!BEventFactory.isError(resultContentpage.listEvents)) {
							final String pageSt = resultContentpage.content;
							final StringReader stringPage = new StringReader(pageSt);
							try {
								final Properties properties = new Properties();

								properties.load(stringPage);
								final String pageName = properties.getProperty("name");
								if (pageName != null && !pageName.isEmpty()) {
									appsItem.setAppsName(typeApps, pageName);
								}

								final String pageDescription = properties.getProperty("description");
								if (pageDescription != null && !pageDescription.isEmpty()) {
									appsItem.description = pageDescription;
								}

								final String pageDisplayName = properties.getProperty("displayName");
								if (pageDisplayName != null && !pageDisplayName.isEmpty()) {
									appsItem.displayName = pageDisplayName;
								}
							} catch (final Exception e) {
								storeResult.addEvent(new BEvent(errorDecodePageProperties, "Error " + e.toString()));
							}
						}

					}

					if (assetName.equalsIgnoreCase("logo.jpg") || assetName.equalsIgnoreCase(shortName + ".jpg")) {

						// we get it !
						try {
							final ResultGithub resultContentLogo = mGithubAccessor.executeGetRestOrder(null, (String) oneContent.get("url"), logBox);
							final String logoSt = (String) resultContentLogo.getJsonObject().get("content");
							final Base64 base64 = new Base64();
							appsItem.logo = base64.decode(logoSt);
							traceOneApps += "logo detected;";
						} catch (final Exception e) {
							appsItem.addEvent(new BEvent(errorDecodeLogo, "Get logo from  [" + oneContent.get("url") + "] : " + e.toString()));
						}
					}
					if (assetName.endsWith(".pdf")) {
						// we get the documentation
						appsItem.documentationFile = (String) oneContent.get("url");
						traceOneApps += "doc detected;";
					}
				} // end loop on content
			} // end get Contents

			// --------------------- release
			String releaseUrl = (String) oneRepository.get("releases_url");
			// release is : :
			// "https://api.github.com/repos/Bonitasoft-Community/page_awacs/releases{/id}",
			if (releaseUrl != null && releaseUrl.endsWith("{/id}")) {
				releaseUrl = releaseUrl.substring(0, releaseUrl.length() - "{/id}".length());
			}

			// get the releases now
			final ResultGithub resultRelease = mGithubAccessor.executeGetRestOrder(null, releaseUrl, logBox);
			resultRelease.checkResultFormat(true, "Contents of a release must be a list");
			storeResult.addEvents(resultRelease.listEvents);
			if (!resultRelease.isError()) {
				for (final Map<String, Object> oneRelease : (List<Map<String, Object>>) resultRelease.getJsonArray()) {

					final AppsItem.AppsRelease appsRelease = appsItem.newInstanceRelease();
					appsRelease.id = (Long) oneRelease.get("id");
					appsRelease.version = oneRelease.get("name").toString();
					traceOneApps += "release[" + appsRelease.version + "] detected;";

					try {
						appsRelease.dateRelease = sdfParseRelease.parse(oneRelease.get("published_at").toString());
					} catch (final Exception e) {
						logBox.log(LOGLEVEL.ERROR, "FoodTruckStoreGithub : date [" + oneRelease.get("published_at") + "] can't be parse.");
					}
					appsRelease.releaseNote = oneRelease.get("body").toString();
					// search a ZIP access
					if (oneRelease.get("assets") != null) {
						for (final Map<String, Object> oneAsset : (List<Map<String, Object>>) oneRelease.get("assets")) {
							final String assetName = (String) oneAsset.get("name");
							if (assetName != null && assetName.endsWith(".zip")) {
								appsRelease.urlDownload = (String) oneAsset.get("browser_download_url");
								if (appsRelease.urlDownload != null && appsRelease.urlDownload.length() == 0) {
									appsRelease.urlDownload = null;
								}
								appsRelease.numberOfDownload = (Long) oneAsset.get("download_count");
								traceOneApps += "release with content;";
							}
						}
					}
					appsItem.listReleases.add(appsRelease);
				} // end loop on release

				if (appsItem.listReleases.size() == 0 || appsItem.getLastUrlDownload() == null) {
					appsItem.appsStatus = AppsStatus.NOTAVAILABLE;
				}
			}
			logBox.log(LogBox.LOGLEVEL.INFO, traceOneApps);
			if (appsItem.appsStatus == AppsStatus.NOTAVAILABLE)
			{
				if (withNotAvailable)
					storeResult.listCustomPage.add(appsItem);
			}
			else
				storeResult.listCustomPage.add(appsItem);
		} // end loop repo

		/*
		 * appsItem.contribFile = eElement.getAttribute("bonitacontributfile");
		 * appsItem.urlDownload = baseUrlDownload +
		 * eElement.getAttribute("bonitacontributfile");
		 * appsItem.documentationFile =
		 * eElement.getAttribute("documentationfile"); try {
		 * appsItem.releaseDate =
		 * sdfParseRelease.parse(eElement.getAttribute("releasedate").toString()
		 * ); } catch (final Exception e) {
		 * logger.severe("FoodTruckStoreGithub.getListAvailableApps Parse Date["
		 * + eElement.getAttribute("releasedate") + " error " + e); }
		 * appsItem.description =
		 * eElement.getElementsByTagName("description").item(0).getTextContent()
		 * ; // logo -------------- generated by GenerateListingItem.java final
		 * String logoSt =
		 * eElement.getElementsByTagName("logo").item(0).getTextContent(); if
		 * (logoSt != null && logoSt.length() > 0) { final Base64 base64 = new
		 * Base64(); appsItem.logo = base64.decode(logoSt); }
		 * storeResult.listCustomPage.add(appsItem); } } catch(final Exception
		 * e) { storeResult.addEvent(new BEvent(BadListingFormat, e, "")); }
		 */
		storeResult.endOperation();
		return storeResult;
	}

	/**
	 * check if the github repository match the expected type
	 *
	 * @param typeApps
	 * @param repositoryName
	 * @return the typeApp or null if nothing match
	 */
	private TypeArtefacts match(final List<TypeArtefacts> listTypeApps, String repositoryName) {
		// TypeAppsName is CUSTOMPAGE or CUSTOMWIDGET

		// we accept CUSTOMPAGE or PAGE
		if (repositoryName == null) {
			return null;
		}
		repositoryName = repositoryName.toUpperCase();

		for (TypeArtefacts typeApps : listTypeApps)
		{
			final String typeAppsName = typeApps.toString().toUpperCase();
			if (repositoryName.startsWith(typeAppsName + "_") || ("CUSTOM" + repositoryName).startsWith(typeAppsName + "_")) {
				return typeApps;
			}
		}
		return null;
	}

	@Override
	public FoodTruckResult downloadOneApps(final AppsItem appsItem, final LogBox logBox) {
		final FoodTruckResult foodTruckResult = new FoodTruckResult("DownloadOneCustomPage");
		if (appsItem.getLastUrlDownload() == null) {
			foodTruckResult.addEvent(new BEvent(noContribFile, "Apps[" + appsItem.getAppsName() + "]"));
			return foodTruckResult;
		}
		if (logBox.isLog(LOGLEVEL.MAIN)) {
			logBox.log(LOGLEVEL.MAIN, "FoodTruck:downloadOneCustomPage : download... [" + appsItem.getLastUrlDownload() + "]");
		}
		final ResultGithub resultListing = mGithubAccessor.getBinaryContent(appsItem.getLastUrlDownload(), "GET", null, null);
		if (logBox.isLog(LOGLEVEL.MAIN)) {
			logBox.log(LOGLEVEL.MAIN, "FoodTruck:downloadOneCustomPage : end download... [" + appsItem.getLastUrlDownload() + "] status="+resultListing.listEvents);
		}

		foodTruckResult.addEvents(resultListing.listEvents);
		if (BEventFactory.isError(resultListing.listEvents)) {
			return foodTruckResult;
		}
		// result is a String, save it in the byteArray
		foodTruckResult.contentByte = resultListing.contentByte;

		return foodTruckResult;
	}

}
