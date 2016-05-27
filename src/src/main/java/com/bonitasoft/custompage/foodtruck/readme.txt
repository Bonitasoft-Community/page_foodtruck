
 
  
  FoodTruckAccess : the public access for any call
  
  FoodTruckIntBonitaStore : the interface to describe a Store
  FoodTruckStoreGihub : the github implementation
  FoodTruckStoreFactory : factory to access the different store 
   
  How to manage repisotory ?
  In FoodTruckAccess a list of repository is given, under a list of <FoodTruckDefStore>
  This list is given to the Factory.   
  then each access do 
    	for (final FoodTruckDefStore defStore : foodTruckParam.listRepository)
            {
                FoodTruckIntBonitaStore store =  factoryStore.getStore( defStore )
               ...
               }
               
Each apps is referenced in a store. Apps retains the store AND send in the JSON the storeMap.
So, when a call is made from the browser, the element return the storeName. On the factory, the store can be retrieve fro the storeName               