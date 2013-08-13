/*
//declare main module
var mainModule = angular.module('aisHDD', ['aisHDD.filter']);

// declare filter module
var filterModule = angular.module('aisHDD.filter', []);

// configure the module.
// in this example we will create a greeting filter
filterModule.filter('greet', function() {
 return function(name) {
    return 'Hello, ' + name + '!';
  };
});

filterModule.filter('capitalize', function() {
    return function(input, scope) {
        if (input!=null)
            return input.substring(0,1).toUpperCase()+input.substring(1);
    }
});


filterModule.filter('restrict',  function () {
        return function(input) {
	  if(input.isNumber)
              return input;
          else
              return 'must enter a number';
                     
     }
    });
*/