function SourceIds($scope) {
  
  $scope.sourceIds = [
    {text:'Source1', value: 'source1&', include:false},
    {text:'Source2', value: 'source2&', include:false},
    {text:'Source3', value: 'source3&', include:false},
    {text:'Source4', value: 'source4&', include:false},
    {text:'Source5', value: 'source5&', include:false},
    {text:'Source6', value: 'source6&', include:false}];
    
  $scope.sourceBases = [
    {text:'1', input:''}];
    
  $scope.sourceTypes = [
    {text:'Any', value: 'any&', include:true},
    {text:'Live', value: 'live&', include:false},
    {text:'Sat', value: 'sat&', include:false}];
    
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
  
  return base + 'id=' + ids + '&base=' + bases + '&type=' + types;
  };
  
  $scope.newTextField = function() {
  	
  	angular.forEach($scope.sourceBases, function(urlBuilder) {
      console.log(urlBuilder.input);
    });
  	
  	//console.log($scope.sourceIds.text);
  	//console.log(2);
  	
  	//var inputNotCreated = true;
  	//if(inputNotCreated) {
  	//	$scope.sourceBases.push({text:'2', input:''});
  	//  inputNotCreated = false;
  	//}
  };
  
}

function SourceTypes($scope) {
    
}