'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('foodtruckmonitor', ['googlechart', 'ui.bootstrap', 'ngSanitize']);






// --------------------------------------------------------------------------
//
// Controler Ping
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('FoodTruckControler',
	function ( $http, $scope,$sce ) {

	
	// --------------------------------------------------------------------------
	//
	//  General
	//
	// --------------------------------------------------------------------------

	this.isshowhistory = false;
	this.showhistory = function( showhistory ) {
		this.isshowhistory = showhistory;
	};
	
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents);
	}
	
	
	                       
	
	// --------------------------------------------------------------------------
	//
	//  Custom page
	//
	// --------------------------------------------------------------------------
	this.loading= false;
	this.custompage= {};
	this.custompage.param={};
	this.custompage.param.showlocal=true;
	this.custompage.param.showBonitaAppsStore=true;
	this.custompage.param.github ={};
	this.custompage.param.github.login="";
	this.custompage.param.github.password="";
	this.custompage.param.gold=true;
	
	

	
	// list
	this.listcustompage = function( show )
	{
		this.loading = true;
		this.custompage.param.show=show;
		var json = angular.toJson(this.custompage.param, false);
		var self=this;
		// alert("json="+json);
		
		this.custompage.status="Collect in progress";

		$http.get( '?page=custompage_foodtruck&action=listcustompage&json='+json )
				.then( function ( jsonResult ) {
						console.log("result",jsonResult.data);
						self.custompage.list 			= jsonResult.data.list;
						self.custompage.listevents 		= jsonResult.data.listevents;
						self.alllistprofiles 			= jsonResult.data.alllistprofiles;
						self.custompage.status="";
				},
				function(jsonResult ) {
					alert("listcustompage: Can't connect the server ("+jsonResult.status+")");
					self.statusresource="Can't connect the server ("+jsonResult.status+")";
				})
				.finally(function() {
					self.loading = false;
				});

	}
	
	 	
	this.downloadcustompage = function( custompageinfo )
	{
		custompageinfo.oldstatus=custompageinfo.status;
		custompageinfo.status="INDOWNLOAD";
		var self=this;
		
		this.custompage.status="Download "+custompageinfo.displayname;

		// note : the server can't save any information (thanks custom page in Debug mode !) so we have to send back all information.
		var param = { "appsname":custompageinfo.appsname,
					"appsid": custompageinfo.appsid,
					"status": custompageinfo.status,
					'urldownload':custompageinfo.urldownload,
					"storegithubname":custompageinfo.storegithubname,
					"github" : this.custompage.param.github};
		var json = angular.toJson(param, true);
		
		
		
		this.statusfoodtruck = "Download "+custompageinfo.installid+" in progress";
		$http.get( '?page=custompage_foodtruck&action=downloadcustompage&json='+json)
				.then( function ( jsonResult ) {
					// self.custompage.list 		= jsonResult.data.list;
					custompageinfo.status=  jsonResult.data.statuscustompage;
					self.custompage.listevents 		= jsonResult.data.listevents;
					self.custompage.status="";
					
					// alert("Result download ="+  angular.toJson(jsonResult, true) );

				},
				function(jsonResult) {
					alert("downloadcustompage: Can't connect the server ("+jsonResult.status+")");
					self.statusresource="Can't connect the server ("+jsonResult.status+")";
					custompageinfo.status=custompageinfo.oldstatus;
					});

	}

	
	this.getlistprofiletoadd = function(custompageinfo ) {
		var listprofiletoadd=[];
		for (var i = 0; i < this.alllistprofiles.length; i++)		
		{
			var profileinfo = this.alllistprofiles[ i ];
			var exist=false;
			for (var j=0;j< custompageinfo.listprofiles.length;j++)
			{
				if (custompageinfo.listprofiles[ j ].name == profileinfo.name)
					exist=true;
			}
			
			if (! exist)
				listprofiletoadd.push( profileinfo );
		}
		return listprofiletoadd;
	}
	 
	
	/** add the custom page in a profile
	 * 
	 */
	this.currentpageinfo=null;
	this.addcustompageinprofile = function( custompageinfo ) {
		var self=this;
		custompageinfo.statusinfo="Add in progress";		
		this.currentpageinfo = custompageinfo;
		
		this.statusfoodtruck = "Add in profile "+custompageinfo.installid+" in progress";
		var param = { "appsname":custompageinfo.appsname,
				"appsid": custompageinfo.appsid,
				"displayname" : custompageinfo.displayname,
				"profileid":custompageinfo.addinprofileid,
				"status": custompageinfo.status}
		var json = angular.toJson(param, true);
		$http.get( '?page=custompage_foodtruck&action=addcustompageinprofile&json='+json)
				.then( function ( jsonResult ) {
					// the custom page upgrated is in the list, first position
					
					custompageinfo.statusinfo       = jsonResult.data.statusinfo ;
					custompageinfo.listprofiles     = jsonResult.data.list[ 0 ].listprofiles;
					self.custompage.listevents 		= jsonResult.data.listevents;
					self.custompage.status			= "";
					
				},
				function(jsonResult) {
					alert("downloadcustompage: Can't connect the server ("+jsonResult.status+")");
					self.statusresource				= "Can't connect the server ("+jsonResult.status+")";
					custompageinfo.status			= custompageinfo.oldstatus;
					});

	}

	/**
	 * remove from the profile
	 */ 
 	this.custompageoutofprofile = function( custompageinfo, profileinfo )
	{
		var self=this;
		custompageinfo.statusinfo="Remove in progress";
		
		this.statusfoodtruck = "Remove from profile "+custompageinfo.installid+" in progress";
		var param = { "appsname":custompageinfo.appsname,
				"appsid": custompageinfo.appsid,
				"profileid":profileinfo.id,
				"status": custompageinfo.status}
		var json = angular.toJson(param, true);
			
		$http.get( '?page=custompage_foodtruck&action=removecustompagefromprofile&json='+json )
				.then( function ( jsonResult ) {
					custompageinfo.statusinfo	= jsonResult.data.statusinfo;
					
					custompageinfo.listprofiles 		= jsonResult.data.list[ 0 ].listprofiles;
					
					alert("statusinfo="+angular.json(custompageinfo.statusinfo, true) )
					self.custompage.listevents 		= jsonResult.data.listevents;
					self.custompage.status			= "";
				
				},
				function( jsonResult) {
					alert("addcustompageinprofile: Can't connect the server ("+jsonResult.status+")");
					self.statusresource="Can't connect the server ("+jsonResult.status+")";
				});

	}
	// --------------------------------------------------------------------------
	//
	//  Custom widget
	//
	// --------------------------------------------------------------------------
	// demo
	this.customwidget={};
	this.customwidget.list =[{icon:"", name:"Rich Table", description:"Table accepted Select, Date, Link, Button", version:"1.1", releasedate:"14/10/2015"}     ];

	// --------------------------------------------------------------------------
	//
	//  properties
	//
	// --------------------------------------------------------------------------

	this.loadParameters = function() 
	{
		var self = this;
		
		$http.get( '?page=custompage_foodtruck&action=loadparameters' )
		.then( function ( jsonResult ) {
				console.log("loadparameters",jsonResult.data);
				$.extend( self.custompage.param, jsonResult.data); 
				self.custompage.param.status =""; // remove status to not save it again
				self.listcustompage();
			},
		function(jsonResult ) {
			// alert("loadparameters Error : "+ angular.toJson( jsonResult.data ) );
			this.listcustompage();
			}
		);
		
	};


	this.saveParameters = function () 
	{
		var self = this;
		self.statusparameters="";
		
		var json = angular.toJson(self.custompage.param, false);
		
		$http.get( '?page=custompage_foodtruck&action=saveparameters&json='+json )
		.then( function ( jsonResult ) {				
				self.statusparameters="Saved.";
		},
		function(jsonResult ) {
			alert("Error when save parameters ("+jsonResult.status+")");
			self.statusparameters="Error during save ("+jsonResult.status+")";
		})	
	}
	// --------------------------------------------------------------------------
	//
	//  Initialisation
	//
	// --------------------------------------------------------------------------
	
	// during the loadParameters, the listCustomPage is started
	this.loadParameters();
	
});



})();