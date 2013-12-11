function shipSelection($scope,UrlService,$http) {

    //Ship types variables: source data.js
    $scope.shipTypes = shipTypes;

    //Control of checked ship types
    $scope.shipTypesSelect = function(shipType) {

        //if all is selected, deselect all other
        if(shipType.value==$scope.shipTypes[_.indexOf(_.pluck($scope.shipTypes, 'text'),'All')].value) {

            angular.forEach($scope.shipTypes, function(shipType) {
                shipType.include=false;
            });

            $scope.shipTypes[0].include=true;

        }

        //deselect all if any other is selected
        if(shipType.value!=$scope.shipTypes[_.indexOf(_.pluck($scope.shipTypes, 'text'),'All')].value) {
            $scope.shipTypes[0].include=false;
        }
    }

}