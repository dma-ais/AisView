function columnSelector($scope) {
	
	//Source variables
  $scope.columns = [
  	{text:'Date', value: 'date', include:false},
    {text:'Time', value: 'time', include:false},
    {text:'Month', value: 'month', include:false},
    {text:'Year', value: 'year', include:false},
    {text:'Earth', value: 'earth', include:false},
    {text:'Moon', value: 'moon', include:false},
    {text:'Mars', value: 'mars', include:false},
    {text:'Universe', value: 'universe', include:false}];
    
    
    $scope.addColumn = function(column) {
    	console.log(column.text + ' just added');
    	column.include = true;
    }
    
    $scope.removeColumn = function(column) {
    	console.log(column.text + ' just removed');
    	column.include = false;
    }
    
    
    }
