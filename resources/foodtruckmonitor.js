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

	
	this.showgithublogin=false;
	
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
	//  Navigation bar
	//
	// --------------------------------------------------------------------------
	                       
	this.navbaractiv='custompage';	
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}

	this.getNavStyle = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'border: 1px solid #c2c2c2;border-bottom-color: transparent;';
		return '';
	}
	
	// --------------------------------------------------------------------------
	//
	//  Custom page
	//
	// --------------------------------------------------------------------------
	this.loading= false;
	this.custompage={ 'showProvided': false, 'listpages': []};
	this.param={};
	this.param.showlocal=true;
	this.param.showBonitaAppsStore=true;
	this.param.github ={};
	this.param.gold=true;
	
	// in Community version, the addProfile is not allowed
	this.generalAllowAddProfile=false;
	

	this.getDisplayListPages = function() {
		var list=[];
		
		
		for (var j=0;j< this.custompage.listpages.length;j++)
		{
			var artifact = this.custompage.listpages[ j ];
		
			if (this.custompage.showProvided)
				list.push( artifact );
			else if ( artifact.isprovided == false ){
				list.push( artifact );
			}
		}
		return list;
	}

	this.restapi={ 'showProvided': false, 'listrestapi': []};

	this.getDisplayListRest = function() {
		var list=[];
		for (var j=0;j< this.restapi.listrestapi.length;j++)
		{
			if (this.restapi.showProvided)
				list.push( this.restapi.listrestapi[ j ] );
			else if ( ! this.restapi.listrestapi[ j ].isprovided){
				list.push( this.restapi.listrestapi[ j ] );
			}
		}
		return list;
	}
	
	// --------------------------------------------------------------------------
	//
	//  artifacts
	//
	// --------------------------------------------------------------------------

	// list
	this.getlistartifacts = function(typeartifact, show )
	{
		this.loading = true;
		this.param.show=show;
		this.param.typeartifact=typeartifact;
		var json = encodeURI(angular.toJson(this.param, false));
		var self=this;
		// alert("json="+json);
		
		this.custompage.status="Collect in progress";
		var d = new Date();

		$http.get( '?page=custompage_foodtruck&action=getlistartifacts&json='+json+'&t='+d.getTime() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
					
				console.log("result",jsonResult);
				self.custompage.listpages 		= jsonResult.listpages;
				self.restapi.listrestapi 		= jsonResult.listrestapi;
				self.custompage.listevents 		= jsonResult.listevents;
				self.alllistprofiles 			= jsonResult.alllistprofiles;
				self.generalAllowAddProfile 	= jsonResult.isAllowAddProfile;
				self.custompage.status="";
				self.loading = false;
			});

	}
	this.getlistartifacts('ALL', 'ALL');
	 	
	this.downloadartifact = function( custompageinfo )
	{
		custompageinfo.oldstatus=custompageinfo.status;
		custompageinfo.status="INDOWNLOAD";
		var self=this;
		
		this.custompage.status="Download "+custompageinfo.displayname;

		// note : the server can't save any information (thanks custom page in Debug mode !) so we have to send back all information.
		var param = { "displayname":custompageinfo.displayname,
					"name": custompageinfo.name,
					"status": custompageinfo.status,
					"type": custompageinfo.type,
					"store": custompageinfo.store,
					'urldownload':custompageinfo.urldownload,
					"storegithubname":custompageinfo.storegithubname,
					"github" : this.param.github};
		var json = encodeURI( angular.toJson(param, true));
		
		
		var d = new Date();

		this.statusfoodtruck = "Download "+custompageinfo.installid+" in progress";
		$http.get( '?page=custompage_foodtruck&action=downloadartifact&json='+json+'&t='+d.getTime())
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				// self.custompage.list 		= jsonResult.data.list;
				custompageinfo.status=  jsonResult.statuscustompage;
				custompageinfo.isAllowAddProfile=  jsonResult.isAllowAddProfile;
					
				self.custompage.listevents 		= jsonResult.listevents;
				self.custompage.status="";
					
				// alert("Result download ="+  angular.toJson(jsonResult, true) );

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
	this.addartifactinprofile = function( custompageinfo ) {
		var self=this;
		custompageinfo.statusinfo="Add in progress...";		
		this.currentpageinfo = custompageinfo;
		
		this.statusfoodtruck = "Add in profile "+custompageinfo.installid+" in progress";
		var param = { "displayname":custompageinfo.displayname,
				"name": custompageinfo.name,
				"profileid":custompageinfo.addinprofileid,
				"type": custompageinfo.type,				
				"status": custompageinfo.status,
				"storegithubname":custompageinfo.storegithubname,
				"github" : this.param.github}
		var json = encodeURI(angular.toJson(param, true));
		var d = new Date();

		$http.get( '?page=custompage_foodtruck&action=addartifactinprofile&json='+json+'&t='+d.getTime())
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// the custom page upgrated is in the list, first position
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				self.currentpageinfo.statusinfo     = jsonResult.statusinfo;
				self.currentpageinfo.listprofiles   = jsonResult.listpages[ 0 ].listprofiles;
				self.custompage.listevents 			= jsonResult.listevents;
				console.log("addprofile : new liste="+self.currentpageinfo.listprofiles);
				self.currentpageinfo.name="bob",
				self.custompage.status				= "";
					
				});

	}

	/**
	 * remove from the profile
	 */ 
 	this.removeartifactfromprofile = function( custompageinfo, profileinfo )
	{
		var self=this;
		custompageinfo.statusinfo="Remove in progress...";
		self.currentpageinfo = custompageinfo;
		
		this.statusfoodtruck = "Remove from profile "+custompageinfo.installid+" in progress";
		var param = { "displayname":custompageinfo.displayname,
				"name": custompageinfo.name,
				"type": custompageinfo.type,
				"profileid":profileinfo.id,
				"status": custompageinfo.status,
				"storegithubname":custompageinfo.storegithubname,
				"github" : this.param.github}
		var json = encodeURI(angular.toJson(param, true));
		var d = new Date();

		$http.get( '?page=custompage_foodtruck&action=removeartifactfromprofile&json='+json +'&t='+d.getTime())
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
				}
				self.currentpageinfo.statusinfo     = jsonResult.statusinfo ;
				self.currentpageinfo.listprofiles   = jsonResult.listpages[ 0 ].listprofiles;
				self.custompage.listevents 			= jsonResult.listevents;
				console.log("addprofile : new liste="+self.currentpageinfo.listprofiles);
			
				// alert("statusinfo="+angular.json(custompageinfo.statusinfo, true) )
				self.custompage.status			= "";
				
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
		self.saveinprogress=true;
		var d = new Date();

		$http.get( '?page=custompage_foodtruck&action=loadparameters&t='+d.getTime() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
			
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				console.log("loadparameters",jsonResult);
				self.saveinprogress=false;
				// $.extend( self.param, jsonResult.data); 
				// angular.copy(jsonResult.data.param, self.param); 
				self.param=jsonResult.param;
				self.listevents= jsonResult.listevents;
				self.getlistartifacts('all');
			},
		function(jsonResult ) {
			self.saveinprogress=false;

			// alert("loadparameters Error : "+ angular.toJson( jsonResult.data ) );
			// this.getlistartifacts();
			}
		);
		
	};


	this.saveParameters = function () 
	{
		var self = this;
		self.saveinprogress=true;
		var json = encodeURI(angular.toJson(self.param, false));
		
		console.log("saveParameters:"+json);
		var d = new Date();

		$http.get( '?page=custompage_foodtruck&action=saveparameters&json='+json+'&t='+d.getTime() )
		.success( function ( jsonResult, statusHttp, headers, config ) {	
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
				self.listevents= jsonResult.listevents;
				self.saveinprogress=false;
		},
		function(jsonResult ) {
			alert("Error when save parameters ("+jsonResult.status+")");
			self.saveinprogress=false;
		})	
	}
	// --------------------------------------------------------------------------
	//
	//  Initialisation
	//
	// --------------------------------------------------------------------------
	
	// during the loadParameters, the listCustomPage is started
	// this.loadParameters();
	
});



})();