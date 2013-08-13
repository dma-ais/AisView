function columnSelector($scope) {
	
	//Source variables
	$scope.columns = [
		{text:'Date', 		value: 'date', 			include:false, marked:false},
		{text:'Time', 		value: 'time', 			include:false, marked:false},
		{text:'Month', 		value: 'month', 		include:false, marked:false},
		{text:'Year', 		value: 'year', 			include:false, marked:false},
		{text:'Earth', 		value: 'earth', 		include:false, marked:false},
		{text:'Moon', 		value: 'moon', 			include:false, marked:false},
		{text:'Mars', 		value: 'mars', 			include:false, marked:false},
		{text:'Universe', value: 'universe',	include:false, marked:false}];
	
	
	$scope.addColumn = function(column) {
		console.log(column.text + ' just added');
		column.include = true;
	}
	
	$scope.removeColumn = function(column) {
		console.log(column.text + ' just removed');
		column.include = false;
	}
	
	$scope.markColumn = function(column) {
		if(column.marked==false) column.marked = true;
		else column.marked = false;
	}
      
}
