// directive for dnd between lists
app.directive('dndBetweenList', function($parse) {

    //inspiration from:
    //http://www.smartjava.org/content/drag-and-drop-angularjs-using-jquery-ui

    return function(scope, element, attrs) {
 
        // contains the args for this component
        var args = attrs.dndBetweenList.split(',');
        // contains the args for the target
        var targetArgs = $('#'+args[1]).attr('dnd-between-list').split(',');
 				
        // variables used for dnd
        var toUpdate;
        var target;
        var startIndex = -1;
        var toTarget = true;
 
        // watch the model, so we always know what element
        // is at a specific position
        scope.$watch(args[0], function(value) {
            toUpdate = value;
        },true);
 
        // also watch for changes in the target list
        scope.$watch(targetArgs[0], function(value) {
            target = value;
        },true);
 
        // use jquery to make the element sortable (dnd). This is called
        // when the element is rendered
        $(element[0]).sortable({
            items:'li',
            start:function (event, ui) {
                
                //determining startIndex depends on which list he is coming from
                //from includedList
                if(args[0]=="included") startIndex = ($(ui.item).index());
                //from notIncludedList
                else {
                	//to find index we need to find number of items before the moved item
                	var xyzCount = 0;
                	var timeCount = 0;
                	//var signalCount = 0;
                	angular.forEach(scope.notIncluded, function(item) {
      							if(item.category.indexOf("xyz") != -1) xyzCount++;
      							if(item.category.indexOf("time") != -1) timeCount++;
      							//if(item.category=="signal") signalCount++;
    							});
                	// and calculate where the item is dragged from
                	if(scope.filterCategory.indexOf("time") != -1) startIndex = ($(ui.item).index())+xyzCount;
                	else if(scope.filterCategory.indexOf("signal") != -1) startIndex = ($(ui.item).index())+(xyzCount+timeCount);
                	else startIndex = ($(ui.item).index());		
                } 
                toTarget = false;
                //console.log('startIndex: '+startIndex);
            },
            stop:function (event, ui) {
                var newParent = ui.item[0].parentNode.id;
				//console.log('newParent: '+newParent	);
								
				// on stop we determine the new index of the
                // item and store it there
                var newIndex = -1;
                
 				//we know which item to move
 				var toMove = toUpdate[startIndex];
 				
                //if taken from includedList
                if(args[0]=="included") {
                	
                	//if dropped in included make disorder
                	if(newParent=="includedList") newIndex = ($(ui.item).index());
                	//if dropped in notIncluded restore same order
                	else {
                        var indexCounter = 0;
                        //determine index, some items might be gone
                        angular.forEach(target, function(item) {
                            if(toMove.id>item.id) indexCounter++;
                        });
                        newIndex = indexCounter;
                    }
                }
                //if taken from notIncludedList
               	else {
               		//if dropped in included make disorder
                	if(newParent=="includedList") newIndex = ($(ui.item).index());
                	//if dropped in notIncluded keep same order
                	else newIndex=startIndex;
               	}
               	
                //console.log('newIndex: '+newIndex);
 
                // we need to remove him from the configured model
                toUpdate.splice(startIndex,1);
 								
 				//finally make changes to the arrays

 				//if we move between lists:
                if (newParent == args[1]) {
                    //add it to the linked list
                    target.splice(newIndex,0,toMove);
                } //if we move on same list	
                else {
                    toUpdate.splice(newIndex,0,toMove);
                    //This is a hack to force updating the ui if

                    //first store filterCategory
                    var tempFilterCategory = scope.filterCategory;
                    //then set filterCategory to random value
                    scope.filterCategory = 'fakeCategory';
                    //and back after 1ms to what is was and force ui update with apply
                    setTimeout(function(){
                        scope.filterCategory = tempFilterCategory;
                        scope.$apply(targetArgs[0]);
                        scope.$apply(args[0]);
                    },1);
                }
 
                // we move items in the array, if we want
                // to trigger an update in angular use $apply()
                // since we're outside angulars lifecycle
                scope.$apply(targetArgs[0]);
                scope.$apply(args[0]);
            },
            connectWith:'#'+args[1]
        })
    }
});