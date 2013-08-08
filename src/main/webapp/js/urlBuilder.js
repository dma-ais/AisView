function SourceIds($scope) {

	$scope.topLeftLat;
	$scope.topLeftLon;
	$scope.buttomRightLat;
	$scope.buttomRightLon;


  
  //Source variables
  $scope.sourceIds = [
  	{text:'All', value: 'all&', include:true},
    {text:'Source1', value: 'source1&', include:false},
    {text:'Source2', value: 'source2&', include:false},
    {text:'Source3', value: 'source3&', include:false},
    {text:'Source4', value: 'source4&', include:false},
    {text:'Source5', value: 'source5&', include:false},
    {text:'Source6', value: 'source6&', include:false}];
    
  $scope.sourceIdsSelect = function(sourceId) {
		
		//if all is selected, deselect all other
		if(sourceId.value==$scope.sourceIds[_.indexOf(_.pluck($scope.sourceIds, 'text'),'All')].value) {
			angular.forEach($scope.sourceIds, function(sourceId) {
      	sourceId.include=false;
    	});
    	$scope.sourceIds[0].include=true;  
		}
		
		//deselect all if any other is selected	
		if(sourceId.value!=$scope.sourceIds[_.indexOf(_.pluck($scope.sourceIds, 'text'),'All')].value) {
			$scope.sourceIds[0].include=false;
		}
  }  
    
  $scope.sourceBases = [
    {text:1+'.', input:'', counter: 1}];
    
  $scope.sourceCountries = [
    {text:1+'.', input:'', counter: 1}];
    
  $scope.sourceTypes = 'any';
  
  //Area variables
  $scope.bboxResult = '';
  
  $scope.format = 'table';
  
  $scope.samples = '';
    
	//making final request url
  $scope.url = function() {
    var base = 'http://www.example.com/';
    var ids = '';
    var bases = '';
    var types = '';
    
    //append all source IDs
    angular.forEach($scope.sourceIds, function(urlBuilder) {
      if (urlBuilder.include) ids+=urlBuilder.value;
    });
    
    //append all source bases
    angular.forEach($scope.sourceBases, function(urlBuilder) {
      bases+=urlBuilder.input+'&';
    });
    
  	return 	base +
  	 				'id='+ids+
  	 				'&base='+bases+
  	 				'&type='+$scope.sourceTypes+
  	 				'&format='+$scope.format+
  	 				'&samples='+$scope.samples+
  	 				'&area='+$scope.topLeftLat+','+$scope.topLeftLon+','+$scope.buttomRightLat+','+$scope.buttomRightLon;
  };
  
  
  var countBase = 2;
  var countCountry = 2;
  
  $scope.newTextFieldBase = function(sourceBase) {
  	
  	if(sourceBase.counter === $scope.sourceBases.length) {
  		$scope.sourceBases.push({text:countBase+'.', input:'', counter: countBase});
  		countBase++;
  		}
  };
  
  $scope.newTextFieldCountry = function(sourceCountry) {
  	
  	if(sourceCountry.counter === $scope.sourceCountries.length) {
  		$scope.sourceCountries.push({text:countCountry+'.', input:'', counter: countCountry});
  		countCountry++;
  		}
  };
  
  $scope.clearSources = function() {
		
		//set all source IDs to false
    angular.forEach($scope.sourceIds, function(urlBuilder) {
      urlBuilder.include=false;
    });
    
    $scope.sourceBases = [
    	{text:1+'.', input:'', counter: 1}];
    countBase = 2;
  	
  	$scope.sourceCountries = [
    	{text:1+'.', input:'', counter: 1}];
    countCountry = 2;
    
   	$scope.sourceTypes='any';  	  
  }
  
  $scope.test = function() {
		
		//newInput($scope.topLeftLat, $scope.topLeftLon, $scope.buttomRightLat, $scope.buttomRightLon);
		
		console.log('inside test');
		  	
  }
  
};

function SourceTypes($scope) {
    
}