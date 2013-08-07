function SourceIds($scope) {

	$scope.bboxResult2 = '';


  
  //Source variables
  $scope.sourceIds = [
    {text:'Source1', value: 'source1&', include:false},
    {text:'Source2', value: 'source2&', include:false},
    {text:'Source3', value: 'source3&', include:false},
    {text:'Source4', value: 'source4&', include:false},
    {text:'Source5', value: 'source5&', include:false},
    {text:'Source6', value: 'source6&', include:false}];
    
  $scope.sourceBases = [
    {text:1, input:''}];
    
  $scope.sourceCountry = [
    {text:1, input:''}];
    
  $scope.sourceTypes = [
    {text:'Any', value: 'any&', include:true},
    {text:'Live', value: 'live&', include:false},
    {text:'Sat', value: 'sat&', include:false}];
  
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
    
    
    
    //append all source types
    angular.forEach($scope.sourceTypes, function(urlBuilder) {
      if (urlBuilder.include) types+=urlBuilder.value;
    });
  
  return base + 'id='+ids+'&base='+bases+'&type='+types+'&format='+$scope.format+'&samples='+$scope.samples+'&area='+$scope.bboxResult2;
  };
  
  
  var inputNotCreated = true;
  var count = 1;
  
  $scope.newTextField = function() {
  	
  	//angular.forEach($scope.sourceBases, function(urlBuilder) {
    //  console.log(urlBuilder.input.length);
    //  if(urlBuilder.input.length===1) {
  	//		$scope.sourceBases.push({text:'2', input:''});
  	//	}
    //});
  	
  	console.log('text:'+ $scope.sourceBases.text);
  	console.log('count:'+ count);
  	if($scope.sourceBases.text===count) inputNotCreated = true;
  	
  	if(inputNotCreated) {
  		count++;
  		$scope.sourceBases.push({text:count, input:''});
  	  inputNotCreated = false;
  	  
  	}
  };
  
  $scope.clearSources = function() {
		
		//set all source IDs to false
    angular.forEach($scope.sourceIds, function(urlBuilder) {
      urlBuilder.include=false;
    });
    
    var count = 0;
    //set all source IDs to false
    angular.forEach($scope.sourceTypes, function(urlBuilder) {
      if(count===0) urlBuilder.include=true;
      else urlBuilder.include=false;
      count++;
    });				    	  
		  	
  }
  
  $scope.test = function() {
		
		console.log('inside test');
		  	
  }
  
};

function SourceTypes($scope) {
    
}